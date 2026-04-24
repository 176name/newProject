package streaming.service.impl;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import streaming.config.ZlmConfig;
import streaming.domain.Result;
import streaming.entity.Device;
import streaming.mapper.DeviceMapper;
import streaming.service.IStreamService;
import streaming.util.AESUtil;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class StreamServiceImpl implements IStreamService {

    @Autowired
    private ZlmConfig zlmConfig;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${aes.secret-key:1234567890123456}")
    private String aesSecretKey;

    @PostConstruct
    public void init() {
        AESUtil.setSecretKey(aesSecretKey);
        log.info("AES 密钥初始化完成");
    }

    public List<Device> getAllDevices() {
        List<Device> devices = deviceMapper.selectList(null);
        devices.forEach(d -> d.setPassword(null));
        log.info("查询到 {} 个设备", devices.size());
        return devices;
    }

    public Result startPushByDeviceId(Long deviceId) {
        log.info("根据设备ID推流: {}", deviceId);
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            log.error("设备不存在, id: {}", deviceId);
            return Result.error("设备不存在");
        }
        log.info("找到设备: {}, IP: {}", device.getName(), device.getIp());
        return startPush(device.getIp());
    }

    @Override
    public Result startPush(String cameraIp) {
        log.info("========== 开始推流 ==========");
        log.info("接收到的 cameraIp: '{}'", cameraIp);

        if ("127.0.0.1".equals(cameraIp) || "test".equals(cameraIp)) {
            String playUrl = String.format("http://%s:%d/live/test.live.flv",
                    zlmConfig.getHost(), zlmConfig.getHttpPort());
            log.info("使用 ZLM 内置测试流: {}", playUrl);
            return Result.success(playUrl);
        }

        Device device = findDeviceByIp(cameraIp);
        if (device == null) {
            log.error("未找到 IP 为 '{}' 的摄像头配置", cameraIp);
            return Result.error("未找到该摄像头的配置信息");
        }

        log.info("找到设备: id={}, name={}, ip={}, username={}, channel={}",
                device.getId(), device.getName(), device.getIp(), device.getUsername(), device.getChannel());

        String plainPassword = device.getPassword();
        log.info("使用设备密码: {}", plainPassword);

        String streamId = "camera_" + device.getIp().replace(".", "_");
        log.info("生成的流ID: {}", streamId);

        String rtspUrl = String.format("rtsp://%s:%s@%s:554/Streaming/Channels/%d",
                device.getUsername(), plainPassword, device.getIp(), device.getChannel());

        log.info("RTSP 源地址: {}", rtspUrl);

        // ========== 关键修改：添加 enable_mp4=1 开启 MP4 录制 ==========
        String url = String.format("http://%s:%d/index/api/addStreamProxy?secret=%s&vhost=__defaultVhost__&app=live&stream=%s&url=%s&enable_mp4=1&mp4_max_seconds=3600",
                zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), streamId, rtspUrl);

        log.info("调用 ZLM API: {}", url);

        try {
            String response = HttpUtil.get(url);
            JSONObject json = JSON.parseObject(response);
            log.info("ZLM 响应: {}", response);

            if (json != null) {
                int code = json.getIntValue("code");
                String msg = json.getString("msg");

                if (code == 0 || (code == -1 && msg != null && msg.contains("exists"))) {
                    String playUrl = String.format("http://%s:%d/live/%s.live.flv",
                            zlmConfig.getHost(), zlmConfig.getHttpPort(), streamId);

                    device.setStreamId(streamId);
                    device.setStatus(1);
                    deviceMapper.updateById(device);

                    if (code == 0) {
                        log.info("ZLM 开启新拉流成功，设备: {}, 播放地址: {}", device.getName(), playUrl);
                    } else {
                        log.info("ZLM 检测到流已存在，复用地址: {}, 设备: {}", playUrl, device.getName());
                    }

                    log.info("========== 推流成功 ==========");
                    return Result.success(playUrl);
                }

                log.error("ZLM 返回业务错误 - Code: {}, Msg: {}", code, msg);
                return Result.error("ZLM拉流失败：" + msg);
            }

            return Result.error("ZLM 返回数据异常");

        } catch (Exception e) {
            log.error("连接 ZLM 服务器异常", e);
            return Result.error("连接ZLM服务器失败");
        }
    }

    /**
     * 停止推流并获取录制文件
     */
    public Result stopPush(String streamId, Long recordingId, String recordType) {
        log.info("========== 停止推流 ==========");
        log.info("停止流ID: {}, recordingId: {}, recordType: {}", streamId, recordingId, recordType);

        String recordFilePath = null;
        Long fileSize = 0L;
        boolean hasFile = false;

        try {
            // 1. 通过 ZLM API 获取录制文件信息
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String getRecordUrl = String.format("http://%s:%d/index/api/getMp4RecordFile?secret=%s&vhost=__defaultVhost__&app=live&stream=%s&period=%s",
                    zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), streamId, today);

            log.info("查询录制文件 API: {}", getRecordUrl);
            String recordResponse = HttpUtil.get(getRecordUrl, 5000);
            log.info("录制文件查询响应: {}", recordResponse);

            if (recordResponse != null) {
                JSONObject recordJson = JSON.parseObject(recordResponse);
                if (recordJson.getIntValue("code") == 0) {
                    JSONObject data = recordJson.getJSONObject("data");
                    if (data != null) {
                        String rootPath = data.getString("rootPath");
                        Object pathsObj = data.get("paths");
                        if (pathsObj instanceof List) {
                            List<String> paths = (List<String>) pathsObj;
                            if (paths != null && !paths.isEmpty()) {
                                String fileName = paths.get(paths.size() - 1);
                                if (fileName.endsWith(".mp4")) {
                                    recordFilePath = rootPath + fileName;
                                    hasFile = true;
                                    log.info("找到录制文件: {}", recordFilePath);
                                }
                            }
                        }
                    }
                }
            }

            // 2. 删除流代理
            String delUrl = String.format("http://%s:%d/index/api/delStreamProxy?secret=%s&key=__defaultVhost__/live/%s",
                    zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), streamId);

            log.info("删除流代理 API: {}", delUrl);
            String delResponse = HttpUtil.get(delUrl, 5000);
            log.info("删除响应: {}", delResponse);

            // 3. 更新设备状态
            Device device = findDeviceByStreamId(streamId);
            if (device != null) {
                device.setStatus(0);
                deviceMapper.updateById(device);
            }

            // 4. 更新录制历史记录
            if (recordingId != null && hasFile && recordFilePath != null) {
                // 获取录制时长
                long durationSeconds = 0;
                try {
                    java.sql.Timestamp startTime = jdbcTemplate.queryForObject(
                            "SELECT start_time FROM recording_history WHERE id = ?",
                            java.sql.Timestamp.class, recordingId);
                    if (startTime != null) {
                        durationSeconds = (System.currentTimeMillis() - startTime.getTime()) / 1000;
                    }
                } catch (Exception e) {
                    durationSeconds = 30;
                }

                String updateSql = "UPDATE recording_history SET end_time = NOW(), " +
                        "duration_seconds = ?, file_path = ?, file_size = ?, status = 'success' " +
                        "WHERE id = ?";
                jdbcTemplate.update(updateSql, durationSeconds, recordFilePath, fileSize, recordingId);
                log.info("更新录制历史成功: recordingId={}, filePath={}", recordingId, recordFilePath);
            } else if (recordingId != null) {
                String failSql = "UPDATE recording_history SET end_time = NOW(), status = 'failed', error_message = '录制失败，未生成有效文件' WHERE id = ?";
                jdbcTemplate.update(failSql, recordingId);
                log.info("更新录制历史失败: recordingId={}", recordingId);
            }

            log.info("========== 停止推流成功 ==========");
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("filePath", recordFilePath);
            resultData.put("fileSize", fileSize);
            return Result.success(resultData);

        } catch (Exception e) {
            log.error("停止推流失败: {}", e.getMessage(), e);
            return Result.error("停止推流失败: " + e.getMessage());
        }
    }

    /**
     * 停止推流并获取录制文件（兼容旧调用）
     */
    public Result stopPush(String streamId, Long recordingId) {
        return stopPush(streamId, recordingId, "manual");
    }

    private Device findDeviceByStreamId(String streamId) {
        if (streamId == null) return null;
        return deviceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Device>()
                        .eq(Device::getStreamId, streamId)
        );
    }

    private Device findDeviceByIp(String ip) {
        String trimmedIp = ip.trim();
        log.info("查询设备，原始IP: '{}', 处理后IP: '{}'", ip, trimmedIp);

        Device device = deviceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Device>()
                        .eq(Device::getIp, trimmedIp)
        );

        if (device == null) {
            log.warn("未找到 IP 为 '{}' 的设备", trimmedIp);
        } else {
            log.info("找到设备: id={}, name={}", device.getId(), device.getName());
        }

        return device;
    }

    public Result addDevice(Device device) {
        try {
            log.info("添加新设备: name={}, ip={}", device.getName(), device.getIp());
            String encryptedPassword = AESUtil.encrypt(device.getPassword());
            device.setPassword(encryptedPassword);
            device.setStatus(0);
            deviceMapper.insert(device);
            log.info("添加设备成功: {}", device.getName());
            return Result.success("设备添加成功");
        } catch (Exception e) {
            log.error("添加设备失败", e);
            return Result.error("添加失败: " + e.getMessage());
        }
    }

    public Result deleteDevice(Long id) {
        log.info("删除设备: id={}", id);
        deviceMapper.deleteById(id);
        log.info("删除设备成功, id: {}", id);
        return Result.success("设备删除成功");
    }
}
