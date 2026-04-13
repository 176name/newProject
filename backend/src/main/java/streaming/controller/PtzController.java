package streaming.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import streaming.domain.Result;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/ptz")
public class PtzController {

    /**
     * 海康云台控制代理接口
     */
    @PostMapping("/control")
    public Result control(@RequestParam String deviceIp,
                          @RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String action) {

        log.info("云台控制请求 - IP: {}, 动作: {}, 用户: {}", deviceIp, action, username);

        // 动作参数映射
        String cmd = mapActionToCommand(action);
        if (cmd == null) {
            return Result.error("不支持的云台动作: " + action);
        }

        // 构建海康 ISAPI 请求 URL
        String url = String.format("http://%s:80/ISAPI/PTZCtrl/channels/1/continuous", deviceIp);

        // 构建 XML 请求体
        String xmlBody = buildPtzXml(cmd);

        try {
            // 使用 Hutool 发送请求，支持 Digest 认证
            HttpResponse response = HttpRequest.put(url)
                    .header("Content-Type", "application/xml")
                    .basicAuth(username, password)  // Hutool 的 basicAuth 会自动处理 Digest
                    .body(xmlBody, "application/xml")
                    .timeout(5000)
                    .execute();

            int statusCode = response.getStatus();
            log.info("云台控制响应状态码: {}", statusCode);

            if (statusCode == 200 || statusCode == 201) {
                return Result.success("云台控制成功");
            } else {
                return Result.error("云台控制失败，状态码: " + statusCode);
            }
        } catch (Exception e) {
            log.error("云台控制异常: {}", e.getMessage());
            return Result.error("云台控制失败: " + e.getMessage());
        }
    }

    /**
     * 测试摄像头连接
     */
    @PostMapping("/test")
    public Result testConnection(@RequestParam String deviceIp,
                                 @RequestParam String username,
                                 @RequestParam String password) {
        try {
            String url = String.format("http://%s:80/ISAPI/PTZCtrl/channels/1/capabilities", deviceIp);
            HttpResponse response = HttpRequest.get(url)
                    .basicAuth(username, password)
                    .timeout(5000)
                    .execute();

            if (response.getStatus() == 200) {
                return Result.success("摄像头连接成功");
            } else {
                return Result.error("连接失败，状态码: " + response.getStatus());
            }
        } catch (Exception e) {
            return Result.error("连接失败: " + e.getMessage());
        }
    }

    private String mapActionToCommand(String action) {
        switch (action) {
            case "UP": return "tilt=20";
            case "DOWN": return "tilt=-20";
            case "LEFT": return "pan=-20";
            case "RIGHT": return "pan=20";
            case "ZOOM_IN": return "zoom=20";
            case "ZOOM_OUT": return "zoom=-20";
            default: return null;
        }
    }

    private String buildPtzXml(String cmd) {
        String[] parts = cmd.split("=");
        String param = parts[0];
        String value = parts[1];

        if ("pan".equals(param)) {
            return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<PTZData>\n" +
                    "  <pan>%s</pan>\n" +
                    "  <tilt>0</tilt>\n" +
                    "  <zoom>0</zoom>\n" +
                    "</PTZData>", value);
        } else if ("tilt".equals(param)) {
            return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<PTZData>\n" +
                    "  <pan>0</pan>\n" +
                    "  <tilt>%s</tilt>\n" +
                    "  <zoom>0</zoom>\n" +
                    "</PTZData>", value);
        } else if ("zoom".equals(param)) {
            return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<PTZData>\n" +
                    "  <pan>0</pan>\n" +
                    "  <tilt>0</tilt>\n" +
                    "  <zoom>%s</zoom>\n" +
                    "</PTZData>", value);
        }
        return "";
    }
}
