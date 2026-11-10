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

    @Value("${recording.auto.enabled:false}")
    private boolean autoRecordingEnabled;

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

        // ZLM 内置测试流（不需要设备配置）
        if ("test".equals(cameraIp)) {
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

        log.info("找到设备: id={}, name={}, ip={}, username={}, channel={}, rtspUrl={}",
                device.getId(), device.getName(), device.getIp(), device.getUsername(), device.getChannel(),
                device.getRtspUrl());

        String sourceUrl = device.getRtspUrl();
        String streamId = "camera_" + device.getIp().replace(".", "_");

        // ========== 模式1：外部推流（push://live/webcam）
        // 源由外部工具（如 FFmpeg 采集笔记本摄像头）推送到 ZLM，ZLM 自动注册流，无需流代理
        if (sourceUrl != null && sourceUrl.startsWith("push://")) {
            String pushStream = sourceUrl.substring(7);
            String playUrl = String.format("http://%s:%d/%s.live.flv",
                    zlmConfig.getHost(), zlmConfig.getHttpPort(), pushStream);
            device.setStreamId(pushStream);
            device.setStatus(1);
            deviceMapper.updateById(device);
            log.info("外部推流模式: stream={}, 播放地址: {}", pushStream, playUrl);
            return Result.success(playUrl);
        }

        // ========== 模式2/3：ZLM 拉流（自定义源地址 或 海康默认格式）
        String rtspUrl;
        if (sourceUrl != null && !sourceUrl.isEmpty()) {
            // 模式2：自定义拉流地址（如手机摄像头 RTSP、网络摄像头）
            rtspUrl = sourceUrl;
        } else {
            // 模式3：海康威视默认 RTSP 格式
            String plainPassword = device.getPassword();
            rtspUrl = String.format("rtsp://%s:%s@%s:554/Streaming/Channels/%d",
                    device.getUsername(), plainPassword, device.getIp(), device.getChannel());
        }

        log.info("生成的流ID: {}", streamId);
        log.info("RTSP 源地址: {}", rtspUrl);

        // 推流，根据配置决定是否开启自动录制
        String url = String.format("http://%s:%d/index/api/addStreamProxy?secret=%s&vhost=__defaultVhost__&app=live&stream=%s&url=%s",
                zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), streamId, rtspUrl);
        if (autoRecordingEnabled) {
            url += "&enableMp4=1";
        }

        log.info("调用 ZLM API: {}", url);

        try {
            String response = HttpUtil.get(url);
            JSONObject json = JSON.parseObject(response);
            log.info("ZLM 响应: {}", response);

            if (json != null) {
                int code = json.getIntValue("code");
                String msg = json.getString("msg");

                // 流已存在——关闭旧流并重建，确保继承最新的 ZLM 全局配置（如 protocol.enable_mp4）
                if (code == -1 && msg != null && msg.contains("exists")) {
                    log.info("流代理已存在，关闭后重建以确保录制能力: {}", streamId);
                    String key = "__defaultVhost__/live/" + streamId;
                    String delUrl = String.format("http://%s:%d/index/api/delStreamProxy?secret=%s&key=%s",
                            zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), key);
                    try {
                        HttpUtil.get(delUrl);
                        Thread.sleep(500);
                    } catch (Exception delEx) {
                        log.warn("删除旧流代理失败（可忽略）: {}", delEx.getMessage());
                    }
                    // 重新创建
                    response = HttpUtil.get(url);
                    json = JSON.parseObject(response);
                    code = json != null ? json.getIntValue("code") : -1;
                    log.info("重建流代理响应: code={}", code);
                }

                if (code == 0 || (code == -1 && msg != null && msg.contains("exists"))) {
                    String playUrl = String.format("http://%s:%d/live/%s.live.flv",
                            zlmConfig.getHost(), zlmConfig.getHttpPort(), streamId);

                    device.setStreamId(streamId);
                    device.setStatus(1);
                    deviceMapper.updateById(device);

                    log.info("ZLM 推流成功，设备: {}, 播放地址: {}", device.getName(), playUrl);
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
     * 停止推流（仅删除流代理，不处理录制文件）
     */
    public Result stopPush(String streamId) {
        log.info("========== 停止推流 ==========");
        log.info("删除流ID: {}", streamId);

        try {
            String delUrl = String.format("http://%s:%d/index/api/delStreamProxy?secret=%s&key=__defaultVhost__/live/%s",
                    zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), streamId);

            log.info("删除流代理 API: {}", delUrl);
            String delResponse = HttpUtil.get(delUrl, 5000);
            log.info("删除响应: {}", delResponse);

            Device device = findDeviceByStreamId(streamId);
            if (device != null) {
                device.setStatus(0);
                deviceMapper.updateById(device);
            }

            log.info("========== 停止推流成功 ==========");
            return Result.success("推流已停止");
        } catch (Exception e) {
            log.error("停止推流失败: {}", e.getMessage(), e);
            return Result.error("停止推流失败: " + e.getMessage());
        }
    }

    /**
     * 开始MP4录制（通过ZLM API在已有流上开始录制）
     */
    public Result startMp4Recording(String streamId, Long recordingId) {
        log.info("========== 开始MP4录制 ==========");
        log.info("streamId: {}, recordingId: {}", streamId, recordingId);

        try {
            String[] appStream = splitAppStream(streamId);
            // 注意：ZLM 录制接口 type=1 表示 MP4（type=0 为 HLS）
            String startRecordUrl = String.format("http://%s:%d/index/api/startRecord?secret=%s&type=1&vhost=__defaultVhost__&app=%s&stream=%s",
                    zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), appStream[0], appStream[1]);

            log.info("调用ZLM开始录制API: {}", startRecordUrl);
            String response = HttpUtil.get(startRecordUrl, 5000);
            JSONObject json = JSON.parseObject(response);
            log.info("ZLM开始录制响应: {}", response);

            if (json.getIntValue("code") == 0) {
                // 更新 recording_history 状态为录制中
                jdbcTemplate.update(
                        "UPDATE recording_history SET status = 'recording' WHERE id = ?",
                        recordingId);
                log.info("MP4录制已开始, streamId={}, recordingId={}", streamId, recordingId);
                return Result.success("录制已开始");
            } else {
                log.error("ZLM开始录制失败: {}", json.getString("msg"));
                return Result.error("开始录制失败: " + json.getString("msg"));
            }
        } catch (Exception e) {
            log.error("调用ZLM开始录制异常: {}", e.getMessage(), e);
            return Result.error("开始录制失败: " + e.getMessage());
        }
    }

    /**
     * 停止MP4录制（通过ZLM API停止录制并获取文件信息）
     */
    /**
     * 获取今天的录制文件列表（文件名集合），用于录制前后对比
     */
    public java.util.Set<String> getTodayRecordFiles(String streamId) {
        java.util.Set<String> files = new java.util.HashSet<>();
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String[] appStream = splitAppStream(streamId);
            String url = String.format("http://%s:%d/index/api/getMp4RecordFile?secret=%s&vhost=__defaultVhost__&app=%s&stream=%s&period=%s",
                    zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), appStream[0], appStream[1], today);
            String response = HttpUtil.get(url, 3000);
            JSONObject json = JSON.parseObject(response);
            if (json != null && json.getIntValue("code") == 0) {
                JSONObject data = json.getJSONObject("data");
                if (data != null) {
                    Object pathsObj = data.get("paths");
                    if (pathsObj instanceof List) {
                        for (Object p : (List<?>) pathsObj) {
                            files.add((String) p);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取录制文件列表失败: {}", e.getMessage());
        }
        return files;
    }

    public Result stopMp4Recording(String streamId, Long recordingId) {
        return stopMp4Recording(streamId, recordingId, null);
    }

    /**
     * 停止MP4录制，beforeFiles 为录制前的文件快照，用于精确匹配新产生的文件
     */
    public Result stopMp4Recording(String streamId, Long recordingId, java.util.Set<String> beforeFiles) {
        log.info("========== 停止MP4录制 ==========");
        log.info("streamId: {}, recordingId: {}", streamId, recordingId);

        try {
            // 1. 调用ZLM停止录制API
            String[] appStream = splitAppStream(streamId);
            String stopRecordUrl = String.format("http://%s:%d/index/api/stopRecord?secret=%s&type=1&vhost=__defaultVhost__&app=%s&stream=%s",
                    zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), appStream[0], appStream[1]);

            log.info("调用ZLM停止录制API: {}", stopRecordUrl);
            String stopResponse = HttpUtil.get(stopRecordUrl, 5000);
            JSONObject stopJson = JSON.parseObject(stopResponse);
            log.info("ZLM停止录制响应: {}", stopResponse);

            boolean stopped = stopJson.getIntValue("code") == 0;

            if (!stopped) {
                String failSql = "UPDATE recording_history SET end_time = NOW(), status = 'failed', " +
                        "error_message = 'ZLM停止录制失败' WHERE id = ?";
                jdbcTemplate.update(failSql, recordingId);
                log.warn("ZLM停止录制失败: recordingId={}", recordingId);
                return Result.error("录制失败，ZLM停止录制返回错误");
            }

            // 2. 获取录制开始时间（时间校验基准）
            long recordingStartMs = 0;
            try {
                java.sql.Timestamp startTime = jdbcTemplate.queryForObject(
                        "SELECT start_time FROM recording_history WHERE id = ?",
                        java.sql.Timestamp.class, recordingId);
                if (startTime != null) {
                    recordingStartMs = startTime.getTime() - 60000; // 容忍1分钟误差
                }
                log.info("录制开始时间: {}, recordingId={}", startTime, recordingId);
            } catch (Exception e) {
                log.warn("获取录制开始时间失败: {}", e.getMessage());
            }

            // 3. 轮询 ZLM 查找新文件（最多等20秒，每2秒查一次）
            String recordFilePath = null;
            Long fileSize = 0L;
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            for (int poll = 0; poll < 10 && recordFilePath == null; poll++) {
                if (poll > 0) {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }

                // 先检查 DB 是否已被 on_record_mp4 回调更新
                try {
                    String dbPath = jdbcTemplate.queryForObject(
                            "SELECT file_path FROM recording_history WHERE id = ? AND file_path IS NOT NULL AND file_path != ''",
                            String.class, recordingId);
                    if (dbPath != null && !dbPath.isEmpty()) {
                        recordFilePath = dbPath;
                        log.info("第{}次轮询: 回调已更新 recordingId={}, filePath={}", poll + 1, recordingId, dbPath);
                        break;
                    }
                } catch (Exception ignored) {}

                // 查询 ZLM 文件列表
                String getRecordUrl = String.format("http://%s:%d/index/api/getMp4RecordFile?secret=%s&vhost=__defaultVhost__&app=%s&stream=%s&period=%s",
                        zlmConfig.getHost(), zlmConfig.getHttpPort(), zlmConfig.getSecret(), appStream[0], appStream[1], today);
                String recordResponse = HttpUtil.get(getRecordUrl, 5000);

                if (recordResponse == null) continue;
                JSONObject recordJson = JSON.parseObject(recordResponse);
                if (recordJson.getIntValue("code") != 0) continue;
                JSONObject data = recordJson.getJSONObject("data");
                if (data == null) continue;
                String rootPath = data.getString("rootPath");
                Object pathsObj = data.get("paths");
                if (!(pathsObj instanceof List)) continue;
                List<String> paths = (List<String>) pathsObj;
                if (paths == null || paths.isEmpty()) continue;

                log.info("第{}次轮询: ZLM 返回 {} 个文件", poll + 1, paths.size());

                // 找第一个满足条件的文件：不在快照中 且 时间戳不早于录制开始
                for (int i = paths.size() - 1; i >= 0; i--) {
                    String f = (String) paths.get(i);
                    if (!f.endsWith(".mp4")) continue;
                    if (beforeFiles != null && beforeFiles.contains(f)) {
                        log.info("  文件已在快照中: {}", f);
                        continue;
                    }
                    long fileTs = parseFileTimestamp(f);
                    if (fileTs > 0 && fileTs < recordingStartMs) {
                        log.info("  时间戳过早: {} (fileTs={} < recTs={})", f, fileTs, recordingStartMs);
                        continue;
                    }
                    recordFilePath = rootPath + f;
                    log.info("第{}次轮询找到匹配文件: {}", poll + 1, recordFilePath);
                    break;
                }
            }

            // 3. 计算录制时长
            long durationSeconds = 0;
            try {
                java.sql.Timestamp startTime = jdbcTemplate.queryForObject(
                        "SELECT start_time FROM recording_history WHERE id = ?",
                        java.sql.Timestamp.class, recordingId);
                if (startTime != null) {
                    durationSeconds = (System.currentTimeMillis() - startTime.getTime()) / 1000;
                }
            } catch (Exception e) {
                durationSeconds = 0;
            }

            // 4. 更新数据库记录
            if (recordFilePath != null) {
                String updateSql = "UPDATE recording_history SET end_time = NOW(), " +
                        "duration_seconds = ?, file_path = ?, file_size = ?, status = 'success' " +
                        "WHERE id = ?";
                jdbcTemplate.update(updateSql, durationSeconds, recordFilePath, fileSize, recordingId);
                log.info("录制完成: recordingId={}, filePath={}, size={}, duration={}s",
                        recordingId, recordFilePath, fileSize, durationSeconds);
            } else {
                // 轮询20秒仍未找到文件，保持 recording 状态等 ZLM 回调来补
                String updateSql = "UPDATE recording_history SET end_time = NOW(), " +
                        "duration_seconds = ?, status = 'recording' WHERE id = ?";
                jdbcTemplate.update(updateSql, durationSeconds, recordingId);
                log.info("轮询结束未找到文件，等待ZLM回调: recordingId={}, duration={}s", recordingId, durationSeconds);
            }

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("filePath", recordFilePath);
            resultData.put("fileSize", fileSize);
            resultData.put("durationSeconds", durationSeconds);
            return Result.success(resultData);

        } catch (Exception e) {
            log.error("停止MP4录制失败: {}", e.getMessage(), e);
            // 标记录制失败
            try {
                jdbcTemplate.update(
                        "UPDATE recording_history SET end_time = NOW(), status = 'failed', error_message = ? WHERE id = ?",
                        e.getMessage(), recordingId);
            } catch (Exception ignored) {}
            return Result.error("停止录制失败: " + e.getMessage());
        }
    }

    /**
     * 将流ID拆分为 app/stream 两部分。
     * 兼容 "live/webcam"（外部推流）与 "camera_x"（传统ZLM拉流）两种格式。
     */
    private String[] splitAppStream(String streamId) {
        if (streamId != null && streamId.contains("/")) {
            String[] parts = streamId.split("/", 2);
            return new String[]{parts[0], parts[1]};
        }
        return new String[]{"live", streamId};
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

    /**
     * 从 ZLM 文件路径中解析时间戳（毫秒）
     * 格式1（日期目录）：/2026-05-14/09-52-00-0.mp4 → 从目录取日期，文件名取时间
     * 格式2（完整文件名）：2026-05-14-09-52-00-0.mp4 → 全部从文件名取
     */
    private long parseFileTimestamp(String filePath) {
        try {
            int year, month, day, hour, minute, second;

            // 查找路径中是否有日期目录 /YYYY-MM-DD/
            int lastSlash = filePath.lastIndexOf('/');
            if (lastSlash >= 0) {
                String dirPart = filePath.substring(0, lastSlash);
                String filePart = filePath.substring(lastSlash + 1);
                int prevSlash = dirPart.lastIndexOf('/');
                String dateDir = prevSlash >= 0 ? dirPart.substring(prevSlash + 1) : dirPart;

                String[] dateParts = dateDir.split("-");
                if (dateParts.length == 3) {
                    year = Integer.parseInt(dateParts[0]);
                    month = Integer.parseInt(dateParts[1]) - 1;
                    day = Integer.parseInt(dateParts[2]);
                } else {
                    return 0;
                }

                // 从文件名取时间：09-52-00-0.mp4 → 去掉扩展名和序号
                int dot = filePart.lastIndexOf('.');
                if (dot >= 0) filePart = filePart.substring(0, dot);
                String[] timeParts = filePart.split("-");
                if (timeParts.length >= 3) {
                    hour = Integer.parseInt(timeParts[0]);
                    minute = Integer.parseInt(timeParts[1]);
                    second = Integer.parseInt(timeParts[2]);
                } else {
                    return 0;
                }
            } else {
                // 无目录，尝试完整文件名格式
                String name = filePath;
                int dot = name.lastIndexOf('.');
                if (dot >= 0) name = name.substring(0, dot);
                String[] parts = name.split("-");
                if (parts.length < 6) return 0;
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]) - 1;
                day = Integer.parseInt(parts[2]);
                hour = Integer.parseInt(parts[3]);
                minute = Integer.parseInt(parts[4]);
                second = Integer.parseInt(parts[5]);
            }

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(year, month, day, hour, minute, second);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            log.warn("解析文件时间戳失败: path={}, error={}", filePath, e.getMessage());
        }
        return 0;
    }
}
