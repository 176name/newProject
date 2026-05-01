package streaming.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import streaming.domain.Result;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/playback")
public class PlaybackController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 从配置文件读取服务器 IP
    @Value("${server-ip:192.168.6.138}")
    private String serverIp;

    private int nginxPort = 8888;
    private int zlmPort = 9999;

    private String nginxBaseUrl;
    private String zlmBaseUrl;

    @PostConstruct
    public void init() {
        // 直接使用配置文件中配置的 IP
        nginxBaseUrl = String.format("http://%s:%d", serverIp, nginxPort);
        zlmBaseUrl = String.format("http://%s:%d", serverIp, zlmPort);
        log.info("PlaybackController 初始化: nginx={}, zlm={}", nginxBaseUrl, zlmBaseUrl);
    }

    /**
     * 获取服务器 IP 地址（前端调用，用于动态适配）
     */
    @GetMapping("/system/ip")
    public Result getServerIp() {
        log.info("返回服务器IP: {}", serverIp);
        return Result.success(serverIp);
    }

    @GetMapping("/history")
    public Result getHistoryList(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int pageSize,
                                 @RequestParam(required = false) Integer deviceId) {
        try {
            int offset = (page - 1) * pageSize;
            String sql;
            String countSql;

            if (deviceId != null && deviceId > 0) {
                sql = "SELECT h.*, d.name as device_name " +
                        "FROM recording_history h " +
                        "LEFT JOIN device d ON h.device_id = d.id " +
                        "WHERE h.status = 'success' AND h.device_id = ? " +
                        "ORDER BY h.created_at DESC " +
                        "LIMIT ? OFFSET ?";
                countSql = "SELECT COUNT(*) FROM recording_history WHERE status = 'success' AND device_id = ?";

                Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, deviceId);
                List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, deviceId, pageSize, offset);

                Map<String, Object> result = new HashMap<>();
                result.put("list", list);
                result.put("total", total != null ? total : 0);
                result.put("page", page);
                result.put("pageSize", pageSize);
                return Result.success(result);
            } else {
                sql = "SELECT h.*, d.name as device_name " +
                        "FROM recording_history h " +
                        "LEFT JOIN device d ON h.device_id = d.id " +
                        "WHERE h.status = 'success' " +
                        "ORDER BY h.created_at DESC " +
                        "LIMIT ? OFFSET ?";
                countSql = "SELECT COUNT(*) FROM recording_history WHERE status = 'success'";

                Integer total = jdbcTemplate.queryForObject(countSql, Integer.class);
                List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, pageSize, offset);

                Map<String, Object> result = new HashMap<>();
                result.put("list", list);
                result.put("total", total != null ? total : 0);
                result.put("page", page);
                result.put("pageSize", pageSize);
                return Result.success(result);
            }
        } catch (Exception e) {
            log.error("查询录制历史失败: {}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/devices")
    public Result getDevices() {
        try {
            String sql = "SELECT id, name FROM device WHERE deleted_at IS NULL ORDER BY id DESC";
            List<Map<String, Object>> devices = jdbcTemplate.queryForList(sql);
            return Result.success(devices);
        } catch (Exception e) {
            log.error("查询设备列表失败: {}", e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/video")
    public void playVideo(@RequestParam String filePath,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        OutputStream out = null;

        try {
            String decodedPath = URLDecoder.decode(filePath, "UTF-8");

            // 将容器内路径转换为 Nginx URL
            String nginxPath = decodedPath.replace("/opt/media/bin/www/record", "");
            String nginxUrl = nginxBaseUrl + "/record" + nginxPath;

            log.info("代理播放录像: {}", nginxUrl);

            HttpResponse httpResponse = HttpRequest.get(nginxUrl)
                    .timeout(30000)
                    .execute();

            if (httpResponse.getStatus() != 200) {
                log.warn("视频文件不存在: {}", nginxUrl);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("视频文件不存在");
                return;
            }

            String contentType = httpResponse.header("Content-Type");
            if (contentType == null || contentType.isEmpty()) {
                contentType = getMimeType(nginxPath);
            }
            response.setContentType(contentType);
            response.setHeader("Accept-Ranges", "bytes");

            String rangeHeader = request.getHeader("Range");
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                long fileSize = httpResponse.contentLength();
                String rangeValue = rangeHeader.substring(6);
                long start = 0;
                long end = fileSize - 1;

                if (rangeValue.contains("-")) {
                    String[] ranges = rangeValue.split("-");
                    start = Long.parseLong(ranges[0]);
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        end = Long.parseLong(ranges[1]);
                    }
                }

                if (start > end || start >= fileSize) {
                    response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    response.setHeader("Content-Range", "bytes */" + fileSize);
                    return;
                }

                long contentLength = end - start + 1;
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
                response.setContentLengthLong(contentLength);

                HttpResponse rangeResponse = HttpRequest.get(nginxUrl)
                        .header("Range", rangeHeader)
                        .timeout(30000)
                        .execute();

                out = response.getOutputStream();
                IoUtil.copy(rangeResponse.bodyStream(), out);
                out.flush();

                log.info("代理播放录像片段: {} (bytes {}-{}/{})", nginxUrl, start, end, fileSize);
            } else {
                response.setContentLengthLong(httpResponse.contentLength());
                out = response.getOutputStream();
                IoUtil.copy(httpResponse.bodyStream(), out);
                out.flush();
                log.info("代理播放录像文件: {}", nginxUrl);
            }

        } catch (Exception e) {
            log.error("播放录像失败: {}", e.getMessage());
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("播放失败: " + e.getMessage());
            } catch (IOException ex) {
                // ignore
            }
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                log.error("关闭流失败: {}", e.getMessage());
            }
        }
    }

    @GetMapping("/info")
    public Result getVideoInfo(@RequestParam Long historyId) {
        try {
            String sql = "SELECT id, recording_id, device_id, file_path, file_size, duration_seconds, start_time, end_time, created_at " +
                    "FROM recording_history WHERE id = ? AND status = 'success'";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, historyId);

            if (results.isEmpty()) {
                return Result.error("录像记录不存在");
            }

            Map<String, Object> record = results.get(0);
            String filePath = (String) record.get("file_path");

            if (filePath == null || filePath.isEmpty()) {
                return Result.error("视频文件路径为空");
            }

            String nginxPath = filePath.replace("/opt/media/bin/www/record", "");
            String nginxUrl = nginxBaseUrl + "/record" + nginxPath;

            boolean fileExists = false;
            try {
                HttpResponse headResponse = HttpRequest.head(nginxUrl).timeout(5000).execute();
                fileExists = headResponse.getStatus() == 200;
            } catch (Exception e) {
                log.warn("检查文件存在性失败: {}", e.getMessage());
            }

            Map<String, Object> info = new HashMap<>();
            info.put("historyId", record.get("id"));
            info.put("recordingId", record.get("recording_id"));
            info.put("deviceId", record.get("device_id"));
            info.put("filePath", filePath);
            info.put("fileSize", record.get("file_size"));
            info.put("durationSeconds", record.get("duration_seconds"));
            info.put("startTime", record.get("start_time"));
            info.put("endTime", record.get("end_time"));
            info.put("createdAt", record.get("created_at"));
            info.put("fileExists", fileExists);
            info.put("playUrl", nginxUrl);

            return Result.success(info);
        } catch (Exception e) {
            log.error("获取录像信息失败: {}", e.getMessage());
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    private String getMimeType(String fileName) {
        if (fileName == null) return "video/mp4";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".flv")) return "video/x-flv";
        if (lower.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (lower.endsWith(".ts")) return "video/MP2T";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".mkv")) return "video/x-matroska";
        return "video/mp4";
    }
}
