package streaming.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import streaming.domain.Result;
import streaming.service.IHistoryService;
import streaming.service.IDeviceOperationHistoryService;
import streaming.service.impl.StreamServiceImpl;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/video")
public class DeviceController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IHistoryService historyService;

    @Autowired
    private IDeviceOperationHistoryService deviceOperationHistoryService;

    @Autowired
    private StreamServiceImpl streamService;

    // 缓存设备在线状态
    private static final ConcurrentHashMap<String, Boolean> deviceStatusCache = new ConcurrentHashMap<>();
    private static long recordingIdCounter = System.currentTimeMillis();

    /**
     * 项目启动时自动检查并创建设备表
     */
    @PostConstruct
    public void init() {
        try {
            String createTable = "CREATE TABLE IF NOT EXISTS device (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "ip VARCHAR(50) NOT NULL UNIQUE, " +
                    "brand VARCHAR(50), " +
                    "username VARCHAR(50) DEFAULT 'admin', " +
                    "password VARCHAR(200), " +
                    "channel INT DEFAULT 101, " +
                    "status INT DEFAULT 0, " +
                    "last_online_time DATETIME, " +
                    "deleted_at DATETIME DEFAULT NULL, " +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            jdbcTemplate.execute(createTable);
            log.info(">>>>>> [SUCCESS] 数据库设备表校验/初始化完成");

            // 创建设备操作历史表
            String createHistoryTable = "CREATE TABLE IF NOT EXISTS device_operation_history (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID', " +
                    "device_id INT NOT NULL COMMENT '设备ID', " +
                    "device_name VARCHAR(100) COMMENT '设备名称（快照）', " +
                    "device_ip VARCHAR(50) COMMENT '设备IP（快照）', " +
                    "operation_type VARCHAR(50) NOT NULL COMMENT '操作类型: ADD/EDIT/DELETE/RESTORE/PERMANENT_DELETE', " +
                    "operation_desc VARCHAR(200) COMMENT '操作描述', " +
                    "operator_id INT DEFAULT 1 COMMENT '操作人ID', " +
                    "operator_name VARCHAR(50) DEFAULT 'admin' COMMENT '操作人名称', " +
                    "detail_before TEXT COMMENT '操作前数据快照（JSON）', " +
                    "detail_after TEXT COMMENT '操作后数据快照（JSON）', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间', " +
                    "INDEX idx_device_id (device_id), " +
                    "INDEX idx_operation_type (operation_type), " +
                    "INDEX idx_created_at (created_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备操作历史记录表';";
            jdbcTemplate.execute(createHistoryTable);
            log.info(">>>>>> [SUCCESS] 设备操作历史表校验/初始化完成");

            // 添加 record_type 字段（如果不存在）
            try {
                String addColumnSql = "ALTER TABLE recording_history ADD COLUMN record_type VARCHAR(20) DEFAULT 'manual' COMMENT '录制类型: auto(全天), manual(手动)'";
                jdbcTemplate.execute(addColumnSql);
                log.info(">>>>>> [SUCCESS] 添加 record_type 字段成功");
            } catch (Exception e) {
                log.info("record_type 字段已存在或添加失败: {}", e.getMessage());
            }

            // 添加索引
            try {
                String addIndexSql = "ALTER TABLE recording_history ADD INDEX idx_record_type (record_type)";
                jdbcTemplate.execute(addIndexSql);
                log.info(">>>>>> [SUCCESS] 添加 idx_record_type 索引成功");
            } catch (Exception e) {
                log.info("idx_record_type 索引已存在或添加失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error(">>>>>> [ERROR] 数据库初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 获取所有设备列表（只显示未删除的）
     */
    @GetMapping("/devices")
    public Result getDevices() {
        try {
            String sql = "SELECT id, name, ip, brand, username, password, channel, status, last_online_time " +
                    "FROM device WHERE deleted_at IS NULL ORDER BY id DESC";
            List<Map<String, Object>> devices = jdbcTemplate.queryForList(sql);
            return Result.success(devices);
        } catch (Exception e) {
            log.error("查询设备失败: {}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取历史设备列表（软删除的设备）
     */
    @GetMapping("/devices/history")
    public Result getHistoryDevices() {
        try {
            String sql = "SELECT id, name, ip, brand, username, password, channel, status, last_online_time, deleted_at " +
                    "FROM device WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC";
            List<Map<String, Object>> devices = jdbcTemplate.queryForList(sql);
            return Result.success(devices);
        } catch (Exception e) {
            log.error("查询历史设备失败: {}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 添加新设备
     */
    @PostMapping("/device")
    public Result addDevice(@RequestBody Map<String, Object> device) {
        try {
            String sql = "INSERT INTO device (name, ip, brand, username, password, channel, status) VALUES (?, ?, ?, ?, ?, ?, 0)";
            jdbcTemplate.update(sql,
                    device.get("name"),
                    device.get("ip"),
                    device.get("brand"),
                    device.get("username"),
                    device.get("password"),
                    device.get("channel")
            );

            // 记录操作历史
            deviceOperationHistoryService.record(
                    null,
                    (String) device.get("name"),
                    (String) device.get("ip"),
                    "ADD",
                    "添加设备: " + device.get("name"),
                    null,
                    JSON.toJSONString(device)
            );

            return Result.success("设备添加成功");
        } catch (Exception e) {
            log.error("添加设备失败: {}", e.getMessage());
            return Result.error("添加失败: IP可能已存在或数据库连接中断");
        }
    }

    /**
     * 更新设备信息
     */
    @PutMapping("/device/{id}")
    public Result updateDevice(@PathVariable Integer id, @RequestBody Map<String, Object> device) {
        try {
            // 获取修改前的数据
            String selectSql = "SELECT * FROM device WHERE id = ?";
            Map<String, Object> oldData = jdbcTemplate.queryForMap(selectSql, id);

            String sql = "UPDATE device SET name=?, ip=?, brand=?, username=?, password=?, channel=? WHERE id=?";
            jdbcTemplate.update(sql,
                    device.get("name"), device.get("ip"), device.get("brand"),
                    device.get("username"), device.get("password"), device.get("channel"), id
            );

            // 记录操作历史
            deviceOperationHistoryService.record(
                    id,
                    (String) device.get("name"),
                    (String) device.get("ip"),
                    "EDIT",
                    "编辑设备: " + device.get("name"),
                    JSON.toJSONString(oldData),
                    JSON.toJSONString(device)
            );

            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新设备失败: {}", e.getMessage());
            return Result.error("更新失败");
        }
    }

    /**
     * 软删除设备（移到历史记录）
     */
    @DeleteMapping("/device/{id}")
    public Result deleteDevice(@PathVariable Integer id) {
        try {
            // 获取删除前的数据
            String selectSql = "SELECT * FROM device WHERE id = ?";
            Map<String, Object> oldData = jdbcTemplate.queryForMap(selectSql, id);

            // 软删除：设置 deleted_at 时间
            String sql = "UPDATE device SET deleted_at = NOW() WHERE id = ?";
            jdbcTemplate.update(sql, id);

            // 记录操作历史
            deviceOperationHistoryService.record(
                    id,
                    (String) oldData.get("name"),
                    (String) oldData.get("ip"),
                    "DELETE",
                    "删除设备: " + oldData.get("name"),
                    JSON.toJSONString(oldData),
                    null
            );

            return Result.success("删除成功，设备已移至历史记录");
        } catch (Exception e) {
            log.error("删除设备失败: {}", e.getMessage());
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 恢复设备（从历史记录中恢复）
     */
    @PostMapping("/device/{id}/restore")
    public Result restoreDevice(@PathVariable Integer id) {
        try {
            // 获取恢复前的数据
            String selectSql = "SELECT * FROM device WHERE id = ?";
            Map<String, Object> oldData = jdbcTemplate.queryForMap(selectSql, id);

            // 恢复：将 deleted_at 设置为 NULL
            String sql = "UPDATE device SET deleted_at = NULL WHERE id = ?";
            jdbcTemplate.update(sql, id);

            // 记录操作历史
            deviceOperationHistoryService.record(
                    id,
                    (String) oldData.get("name"),
                    (String) oldData.get("ip"),
                    "RESTORE",
                    "恢复设备: " + oldData.get("name"),
                    JSON.toJSONString(oldData),
                    null
            );

            return Result.success("设备恢复成功");
        } catch (Exception e) {
            log.error("恢复设备失败: {}", e.getMessage());
            return Result.error("恢复失败: " + e.getMessage());
        }
    }

    /**
     * 彻底删除设备（从数据库中完全删除）
     */
    @DeleteMapping("/device/{id}/permanent")
    public Result permanentDeleteDevice(@PathVariable Integer id) {
        try {
            // 获取删除前的数据
            String selectSql = "SELECT * FROM device WHERE id = ?";
            Map<String, Object> oldData = jdbcTemplate.queryForMap(selectSql, id);

            // 彻底删除
            String sql = "DELETE FROM device WHERE id = ?";
            jdbcTemplate.update(sql, id);

            // 记录操作历史
            deviceOperationHistoryService.record(
                    id,
                    (String) oldData.get("name"),
                    (String) oldData.get("ip"),
                    "PERMANENT_DELETE",
                    "彻底删除设备: " + oldData.get("name"),
                    JSON.toJSONString(oldData),
                    null
            );

            return Result.success("设备已彻底删除");
        } catch (Exception e) {
            log.error("彻底删除设备失败: {}", e.getMessage());
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发设备在线状态检测
     */
    @GetMapping("/device/check")
    public Result checkDevice(@RequestParam String ip) {
        boolean online = false;
        try {
            // 1. 尝试 Ping
            InetAddress address = InetAddress.getByName(ip);
            if (address.isReachable(2000)) {
                online = true;
            }
            // 2. 如果 Ping 不通，尝试 80 端口
            if (!online) {
                try {
                    HttpResponse response = HttpRequest.get("http://" + ip).timeout(2000).execute();
                    online = (response.getStatus() == 200);
                } catch (Exception ignored) {}
            }

            // 更新缓存和数据库
            deviceStatusCache.put(ip, online);
            jdbcTemplate.update("UPDATE device SET status = ?, last_online_time = NOW() WHERE ip = ?",
                    online ? 1 : 0, ip);

            return Result.success(online);
        } catch (Exception e) {
            log.error("检测异常: {}", e.getMessage());
            return Result.error("检测异常");
        }
    }

    /**
     * 获取设备操作历史记录（分页）
     */
    @GetMapping("/device/operation/history")
    public Result getDeviceOperationHistory(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            Map<String, Object> result = deviceOperationHistoryService.getHistoryList(page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询设备操作历史失败: {}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    // ========== ZLM 回调接口 ==========

    /**
     * ZLM 录制完成后的回调接口
     */
    @PostMapping("/hook/on_record_mp4")
    public Result onRecordMp4(@RequestBody Map<String, Object> params) {
        try {
            log.info("收到 ZLM 录制完成回调: {}", params);

            // 解析参数（ZLM 用 stream 而不是 stream_id）
            String filePath = (String) params.get("file_path");
            String stream = (String) params.get("stream");
            Object durationObj = params.get("time_len");
            Long duration = durationObj != null ? Math.round(Double.parseDouble(durationObj.toString())) : 30;
            Object fileSizeObj = params.get("file_size");
            Long fileSize = fileSizeObj != null ? Long.parseLong(fileSizeObj.toString()) : 0;

            if (filePath == null || stream == null) {
                log.warn("回调参数不完整: filePath={}, stream={}", filePath, stream);
                return Result.error("参数不完整");
            }

            // 根据 stream 查找 deviceId
            Integer deviceId = getDeviceIdByStreamId(stream);
            if (deviceId == null) {
                log.error("未找到 stream 对应的设备: {}", stream);
                return Result.error("设备不存在");
            }

            // 自动插入数据库
            Long recordingId = System.currentTimeMillis();
            String sql = "INSERT INTO recording_history (recording_id, device_id, user_id, action, status, start_time, end_time, duration_seconds, file_path, file_size, record_type, created_at) " +
                    "VALUES (?, ?, 1, 'stop', 'success', NOW() - INTERVAL ? SECOND, NOW(), ?, ?, ?, 'manual', NOW())";
            jdbcTemplate.update(sql, recordingId, deviceId, duration, duration, filePath, fileSize);

            log.info("自动入库成功: recordingId={}, filePath={}, duration={}秒", recordingId, filePath, duration);

            return Result.success("ok");
        } catch (Exception e) {
            log.error("处理录制回调失败: {}", e.getMessage());
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    // ========== 录制历史相关接口 ==========

    /**
     * 开始录制（手动录制）
     */
    @PostMapping("/record/start")
    public Result startRecord(@RequestParam Integer deviceId,
                              @RequestParam(defaultValue = "1") Integer userId) {
        try {
            // ========== 强制清理该设备的旧录制状态 ==========
            // 清理数据库中卡住的 running 状态
            String cleanSql = "UPDATE recording_history SET status = 'failed', end_time = NOW(), error_message = '被新录制覆盖' WHERE device_id = ? AND status = 'running'";
            int cleaned = jdbcTemplate.update(cleanSql, deviceId);
            if (cleaned > 0) {
                log.info("清理了 {} 条卡住的录制状态，设备ID: {}", cleaned, deviceId);
            }

            // 检查是否还有 running 状态（二次确认）
            Map<String, Object> running = historyService.getRunningRecording(deviceId);
            if (running != null) {
                // 强制结束它
                Long oldRecordingId = (Long) running.get("recording_id");
                String forceFailSql = "UPDATE recording_history SET status = 'failed', end_time = NOW(), error_message = '强制结束' WHERE id = ?";
                jdbcTemplate.update(forceFailSql, oldRecordingId);
                log.info("强制结束旧的录制记录: {}", oldRecordingId);
            }
            // ====================================================

            // 检查设备是否存在（只查未删除的）
            String checkSql = "SELECT COUNT(*) FROM device WHERE id = ? AND deleted_at IS NULL";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, deviceId);
            if (count == null || count == 0) {
                return Result.error("设备不存在");
            }

            // 生成录制任务ID
            Long recordingId = ++recordingIdCounter;

            // 插入历史记录（手动录制类型）
            Long historyId = historyService.startRecording(deviceId, userId, recordingId);

            // 获取设备的 streamId
            String streamId = getStreamIdByDeviceId(deviceId);
            if (streamId == null || streamId.isEmpty()) {
                String deviceIp = getDeviceIpById(deviceId);
                if (deviceIp != null) {
                    streamId = "camera_" + deviceIp.replace(".", "_");
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("recordingId", recordingId);
            data.put("historyId", historyId);
            data.put("streamId", streamId);
            data.put("message", "开始录制成功");
            return Result.success(data);
        } catch (Exception e) {
            log.error("开始录制失败: {}", e.getMessage());
            return Result.error("开始录制失败: " + e.getMessage());
        }
    }

    /**
     * 停止录制（成功）
     */
    @PostMapping("/record/stop")
    public Result stopRecord(@RequestParam Long recordingId,
                             @RequestParam(required = false) String streamId,
                             @RequestParam(defaultValue = "manual") String recordType) {
        try {
            log.info("停止录制请求: recordingId={}, streamId={}, recordType={}", recordingId, streamId, recordType);

            // 如果前端没有传 streamId，从数据库查询
            if (streamId == null || streamId.isEmpty()) {
                streamId = getStreamIdByRecordingId(recordingId);
                log.info("从数据库查询到 streamId: {}", streamId);
            }

            // 如果还是为空，根据 device_id 生成
            if (streamId == null || streamId.isEmpty()) {
                Integer deviceId = getDeviceIdByRecordingId(recordingId);
                if (deviceId != null) {
                    String deviceIp = getDeviceIpById(deviceId);
                    if (deviceIp != null) {
                        streamId = "camera_" + deviceIp.replace(".", "_");
                        log.info("根据设备IP生成 streamId: {}", streamId);
                    }
                }
            }

            if (streamId == null || streamId.isEmpty()) {
                return Result.error("无法获取流ID，请检查录制记录");
            }

            // 调用 StreamService 停止推流并获取录制文件
            Result stopResult = streamService.stopPush(streamId, recordingId, recordType);

            log.info("停止录制结果: {}", stopResult);
            return stopResult;
        } catch (Exception e) {
            log.error("停止录制失败: {}", e.getMessage());
            return Result.error("停止录制失败: " + e.getMessage());
        }
    }

    /**
     * 录制失败（异常中断时调用）
     */
    @PostMapping("/record/fail")
    public Result recordFail(@RequestParam Long recordingId,
                             @RequestParam String errorMessage) {
        try {
            int rows = historyService.stopRecordingFailed(recordingId, errorMessage);
            if (rows > 0) {
                return Result.success("已记录失败信息");
            } else {
                return Result.error("未找到正在录制的任务");
            }
        } catch (Exception e) {
            log.error("记录失败信息出错: {}", e.getMessage());
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备当前录制状态
     */
    @GetMapping("/record/status")
    public Result getRecordStatus(@RequestParam Integer deviceId) {
        try {
            Map<String, Object> running = historyService.getRunningRecording(deviceId);
            if (running != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("isRecording", true);
                data.put("recordingId", running.get("recording_id"));
                data.put("startTime", running.get("start_time"));
                return Result.success(data);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("isRecording", false);
                return Result.success(data);
            }
        } catch (Exception e) {
            log.error("查询状态失败: {}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备录制历史记录（分页）- 不分类型
     */
    @GetMapping("/record/history")
    public Result getHistory(@RequestParam Integer deviceId,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int pageSize) {
        try {
            List<Map<String, Object>> list = historyService.getDeviceHistory(deviceId, page, pageSize);
            int total = historyService.getHistoryCount(deviceId);

            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询录制历史记录失败: {}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取录制历史记录（按类型筛选）
     */
    @GetMapping("/record/history/byType")
    public Result getHistoryByType(@RequestParam Integer deviceId,
                                   @RequestParam String recordType,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int offset = (page - 1) * pageSize;
            String sql = "SELECT h.*, d.name as device_name " +
                    "FROM recording_history h " +
                    "LEFT JOIN device d ON h.device_id = d.id " +
                    "WHERE h.device_id = ? AND h.record_type = ? AND h.status = 'success' " +
                    "ORDER BY h.created_at DESC " +
                    "LIMIT ? OFFSET ?";
            String countSql = "SELECT COUNT(*) FROM recording_history WHERE device_id = ? AND record_type = ? AND status = 'success'";

            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, deviceId, recordType, pageSize, offset);
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, deviceId, recordType);

            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            data.put("total", total != null ? total : 0);
            data.put("page", page);
            data.put("pageSize", pageSize);
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询录制历史失败: {}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 根据 streamId 获取 deviceId（支持 Long 类型转换）
     */
    private Integer getDeviceIdByStreamId(String streamId) {
        try {
            String sql = "SELECT id FROM device WHERE stream_id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, streamId);
            if (results != null && !results.isEmpty()) {
                Object idObj = results.get(0).get("id");
                if (idObj instanceof Integer) {
                    return (Integer) idObj;
                } else if (idObj instanceof Long) {
                    return ((Long) idObj).intValue();
                }
            }
        } catch (Exception e) {
            log.error("查询 deviceId 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据设备ID获取 streamId
     */
    private String getStreamIdByDeviceId(Integer deviceId) {
        try {
            String sql = "SELECT stream_id FROM device WHERE id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, deviceId);
            if (results != null && !results.isEmpty()) {
                Object streamId = results.get(0).get("stream_id");
                return streamId != null ? streamId.toString() : null;
            }
        } catch (Exception e) {
            log.error("获取 streamId 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据 recordingId 获取 streamId
     */
    private String getStreamIdByRecordingId(Long recordingId) {
        try {
            String sql = "SELECT d.stream_id FROM recording_history h " +
                    "LEFT JOIN device d ON h.device_id = d.id " +
                    "WHERE h.id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, recordingId);
            if (results != null && !results.isEmpty()) {
                Object streamId = results.get(0).get("stream_id");
                return streamId != null ? streamId.toString() : null;
            }
        } catch (Exception e) {
            log.error("获取 streamId 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据 recordingId 获取 deviceId
     */
    private Integer getDeviceIdByRecordingId(Long recordingId) {
        try {
            String sql = "SELECT device_id FROM recording_history WHERE id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, recordingId);
            if (results != null && !results.isEmpty()) {
                Object deviceId = results.get(0).get("device_id");
                if (deviceId instanceof Integer) {
                    return (Integer) deviceId;
                } else if (deviceId instanceof Long) {
                    return ((Long) deviceId).intValue();
                }
            }
        } catch (Exception e) {
            log.error("获取 deviceId 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据设备ID获取设备IP
     */
    private String getDeviceIpById(Integer deviceId) {
        try {
            String sql = "SELECT ip FROM device WHERE id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, deviceId);
            if (results != null && !results.isEmpty()) {
                Object ip = results.get(0).get("ip");
                return ip != null ? ip.toString() : null;
            }
        } catch (Exception e) {
            log.error("获取设备IP失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 删除录制记录
     */
    @DeleteMapping("/record/{id}")
    public Result deleteRecord(@PathVariable Long id) {
        try {
            String sql = "UPDATE recording_history SET status = 'deleted' WHERE id = ?";
            int rows = jdbcTemplate.update(sql, id);
            if (rows > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("记录不存在");
            }
        } catch (Exception e) {
            log.error("删除失败: {}", e.getMessage());
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
