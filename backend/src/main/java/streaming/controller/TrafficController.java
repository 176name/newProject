package streaming.controller;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import streaming.config.ZlmConfig;
import streaming.domain.Result;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    @Autowired
    private ZlmConfig zlmConfig;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取实时流量数据
     */
    @GetMapping("/realtime")
    public Result getRealtimeTraffic() {
        try {
            String zlmUrl = String.format("http://%s:9999/index/api/getMediaList?secret=%s",
                    zlmConfig.getHost(), zlmConfig.getSecret());
            String response = HttpUtil.get(zlmUrl, 5000);
            JSONObject json = JSON.parseObject(response);

            if (json.getIntValue("code") != 0) {
                return Result.error("获取流媒体状态失败");
            }

            JSONArray data = json.getJSONArray("data");
            List<Map<String, Object>> streams = new ArrayList<>();
            long totalBytesSpeed = 0;
            int totalReaderCount = 0;

            for (int i = 0; i < data.size(); i++) {
                JSONObject stream = data.getJSONObject(i);
                Map<String, Object> info = new HashMap<>();
                info.put("stream", stream.getString("stream"));
                info.put("bytesSpeed", stream.getLong("bytesSpeed"));
                info.put("readerCount", stream.getInteger("readerCount"));
                info.put("totalBytes", stream.getLong("totalBytes"));
                info.put("aliveSecond", stream.getInteger("aliveSecond"));
                info.put("bitrate", stream.getLong("bytesSpeed") * 8 / 1024); // kbps

                streams.add(info);
                totalBytesSpeed += stream.getLong("bytesSpeed");
                totalReaderCount += stream.getInteger("readerCount");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("streams", streams);
            result.put("totalBandwidthKbps", totalBytesSpeed * 8 / 1024);
            result.put("totalBandwidthMbps", totalBytesSpeed * 8 / 1024 / 1024);
            result.put("totalViewers", totalReaderCount);
            result.put("streamCount", streams.size());

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取实时流量失败: {}", e.getMessage());
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备流量统计
     */
    @GetMapping("/device")
    public Result getDeviceTraffic(@RequestParam Integer deviceId,
                                   @RequestParam(defaultValue = "day") String period) {
        try {
            String sql;
            if ("day".equals(period)) {
                sql = "SELECT COALESCE(SUM(file_size), 0) as total_traffic, COUNT(*) as record_count " +
                        "FROM recording_history WHERE device_id = ? AND DATE(created_at) = CURDATE() AND status = 'success'";
            } else if ("week".equals(period)) {
                sql = "SELECT COALESCE(SUM(file_size), 0) as total_traffic, COUNT(*) as record_count " +
                        "FROM recording_history WHERE device_id = ? AND YEARWEEK(created_at) = YEARWEEK(CURDATE()) AND status = 'success'";
            } else if ("month".equals(period)) {
                sql = "SELECT COALESCE(SUM(file_size), 0) as total_traffic, COUNT(*) as record_count " +
                        "FROM recording_history WHERE device_id = ? AND MONTH(created_at) = MONTH(CURDATE()) AND status = 'success'";
            } else {
                return Result.error("无效的周期参数");
            }

            Map<String, Object> result = jdbcTemplate.queryForMap(sql, deviceId);

            // 修复：正确处理 BigDecimal 类型
            Object trafficObj = result.get("total_traffic");
            long totalTrafficBytes = getLongValue(trafficObj);

            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", deviceId);
            data.put("period", period);
            data.put("totalTrafficBytes", totalTrafficBytes);
            data.put("totalTrafficMB", totalTrafficBytes / 1024 / 1024);
            data.put("recordCount", result.get("record_count"));

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取设备流量统计失败: {}", e.getMessage());
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备流量排名
     */
    @GetMapping("/ranking")
    public Result getTrafficRanking(@RequestParam(defaultValue = "day") String period) {
        try {
            String sql;
            if ("day".equals(period)) {
                sql = "SELECT d.id, d.name, COALESCE(SUM(h.file_size), 0) as total_traffic, COUNT(h.id) as record_count " +
                        "FROM device d LEFT JOIN recording_history h ON d.id = h.device_id AND DATE(h.created_at) = CURDATE() AND h.status = 'success' " +
                        "WHERE d.deleted_at IS NULL " +
                        "GROUP BY d.id ORDER BY total_traffic DESC";
            } else if ("week".equals(period)) {
                sql = "SELECT d.id, d.name, COALESCE(SUM(h.file_size), 0) as total_traffic, COUNT(h.id) as record_count " +
                        "FROM device d LEFT JOIN recording_history h ON d.id = h.device_id AND YEARWEEK(h.created_at) = YEARWEEK(CURDATE()) AND h.status = 'success' " +
                        "WHERE d.deleted_at IS NULL " +
                        "GROUP BY d.id ORDER BY total_traffic DESC";
            } else {
                sql = "SELECT d.id, d.name, COALESCE(SUM(h.file_size), 0) as total_traffic, COUNT(h.id) as record_count " +
                        "FROM device d LEFT JOIN recording_history h ON d.id = h.device_id AND MONTH(h.created_at) = MONTH(CURDATE()) AND h.status = 'success' " +
                        "WHERE d.deleted_at IS NULL " +
                        "GROUP BY d.id ORDER BY total_traffic DESC";
            }

            List<Map<String, Object>> ranking = jdbcTemplate.queryForList(sql);
            List<Map<String, Object>> result = new ArrayList<>();

            for (Map<String, Object> item : ranking) {
                Map<String, Object> device = new HashMap<>();
                device.put("id", item.get("id"));
                device.put("name", item.get("name"));

                // 修复：正确处理 BigDecimal 类型
                Object trafficObj = item.get("total_traffic");
                long totalTrafficBytes = getLongValue(trafficObj);
                long totalTrafficMB = totalTrafficBytes / (1024 * 1024);

                device.put("total_traffic_mb", totalTrafficMB);
                device.put("record_count", item.get("record_count"));
                result.add(device);
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取流量排名失败: {}", e.getMessage());
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取流量趋势（用于图表）
     */
    @GetMapping("/trend")
    public Result getTrafficTrend(@RequestParam(defaultValue = "7") int days) {
        try {
            String sql = "SELECT DATE(created_at) as date, COALESCE(SUM(file_size), 0) as total_traffic, COUNT(*) as record_count " +
                    "FROM recording_history WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) AND status = 'success' " +
                    "GROUP BY DATE(created_at) ORDER BY date ASC";

            List<Map<String, Object>> trend = jdbcTemplate.queryForList(sql, days);
            List<Map<String, Object>> result = new ArrayList<>();

            for (Map<String, Object> item : trend) {
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", item.get("date"));

                // 修复：正确处理 BigDecimal 类型
                Object trafficObj = item.get("total_traffic");
                long totalTrafficBytes = getLongValue(trafficObj);
                long totalTrafficMB = totalTrafficBytes / (1024 * 1024);

                dayData.put("total_traffic_mb", totalTrafficMB);
                dayData.put("record_count", item.get("record_count"));
                result.add(dayData);
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取流量趋势失败: {}", e.getMessage());
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 安全地将 Object 转换为 long 值
     * 处理 BigDecimal、Long、Integer、String 等类型
     */
    private long getLongValue(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof BigDecimal) {
            return ((BigDecimal) obj).longValue();
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
}

