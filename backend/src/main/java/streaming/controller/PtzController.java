package streaming.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import streaming.domain.Result;
import streaming.entity.Device;
import streaming.mapper.DeviceMapper;

@Slf4j
@RestController
@RequestMapping("/api/ptz")
public class PtzController {

    @Autowired
    private DeviceMapper deviceMapper;

    @PostMapping("/control")
    public Result control(@RequestParam String deviceIp,
                          @RequestParam String action) {

        log.info("云台控制请求 - IP: {}, 动作: {}", deviceIp, action);

        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getIp, deviceIp)
        );
        if (device == null) {
            return Result.error("设备不存在: " + deviceIp);
        }

        return ptzControlViaISAPI(deviceIp, device.getUsername(), device.getPassword(), action);
    }

    private Result ptzControlViaISAPI(String deviceIp, String username, String password, String action) {
        String xmlBody = buildPtzXml(action);
        if (xmlBody == null) {
            return Result.error("不支持的云台动作: " + action);
        }

        String url = String.format("http://%s:80/ISAPI/PTZCtrl/channels/1/continuous", deviceIp);

        // 使用 Digest 认证
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(username, password)
        );

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {

            HttpPut httpPut = new HttpPut(url);
            httpPut.setHeader("Content-Type", "application/xml");
            httpPut.setEntity(new StringEntity(xmlBody, "UTF-8"));

            HttpResponse response = httpClient.execute(httpPut);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            log.info("云台控制响应状态码: {}, 响应内容: {}", statusCode, responseBody);

            if (statusCode == 200) {
                return Result.success("云台控制成功");
            } else if (statusCode == 401) {
                log.error("认证失败，请检查用户名密码");
                return Result.error("认证失败，请检查用户名密码");
            } else {
                return Result.error("云台控制失败，状态码: " + statusCode);
            }

        } catch (Exception e) {
            log.error("云台控制异常: {}", e.getMessage());
            return Result.error("云台控制失败: " + e.getMessage());
        }
    }

    private String buildPtzXml(String action) {
        switch (action) {//pan,tilt,zoom控制左右，上下，边角的速度和方向
            case "UP":
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<PTZData>\n" +
                        "  <pan>0</pan>\n" +
                        "  <tilt>50</tilt>\n" +
                        "  <zoom>0</zoom>\n" +
                        "</PTZData>";
            case "DOWN":
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<PTZData>\n" +
                        "  <pan>0</pan>\n" +
                        "  <tilt>-50</tilt>\n" +
                        "  <zoom>0</zoom>\n" +
                        "</PTZData>";
            case "LEFT":
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<PTZData>\n" +
                        "  <pan>-50</pan>\n" +
                        "  <tilt>0</tilt>\n" +
                        "  <zoom>0</zoom>\n" +
                        "</PTZData>";
            case "RIGHT":
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<PTZData>\n" +
                        "  <pan>50</pan>\n" +
                        "  <tilt>0</tilt>\n" +
                        "  <zoom>0</zoom>\n" +
                        "</PTZData>";
            case "ZOOM_IN":
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<PTZData>\n" +
                        "  <pan>0</pan>\n" +
                        "  <tilt>0</tilt>\n" +
                        "  <zoom>50</zoom>\n" +
                        "</PTZData>";
            case "ZOOM_OUT":
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<PTZData>\n" +
                        "  <pan>0</pan>\n" +
                        "  <tilt>0</tilt>\n" +
                        "  <zoom>-50</zoom>\n" +
                        "</PTZData>";
            case "STOP":
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<PTZData>\n" +
                        "  <pan>0</pan>\n" +
                        "  <tilt>0</tilt>\n" +
                        "  <zoom>0</zoom>\n" +
                        "</PTZData>";
            default:
                return null;
        }
    }
}
