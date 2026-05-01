package streaming.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class AlertService {

    @Value("${screenshot.dir:./screenshots}")
    private String screenshotDir;

    // 在构造函数中不做事，等 @PostConstruct 初始化
    public AlertService() {
        // 空构造函数，不做事
    }

    @PostConstruct
    public void init() {
        // 确保截图目录存在
        File dir = new File(screenshotDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("✅ 创建截图目录: {}", dir.getAbsolutePath());
            }
        }
        log.info("📸 AlertService 初始化完成，截图目录: {}", dir.getAbsolutePath());
    }

    /**
     * 保存截图到本地
     * @param imageBytes 图片字节数组
     * @param type 类型：stranger/fall_confirmed/fall_alert
     * @return 保存的文件路径，失败返回 null
     */
    public String saveScreenshot(byte[] imageBytes, String type) {
        try {
            // 生成文件名：类型_时间戳.jpg
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = type + "_" + timestamp + ".jpg";

            // 确保目录存在
            File dir = new File(screenshotDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 保存文件
            String filepath = screenshotDir + File.separator + filename;
            try (FileOutputStream fos = new FileOutputStream(filepath)) {
                fos.write(imageBytes);
            }

            log.info("✅ 截图已保存: {}", filepath);
            return filepath;

        } catch (IOException e) {
            log.error("保存截图失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 发送告警通知
     * @param message 告警消息
     * @param screenshotPath 截图路径（可选）
     */
    public void sendAlert(String message, String screenshotPath) {
        // 1. 打印日志（至少能看到告警记录）
        log.warn("📢 告警: {} | 截图: {}", message, screenshotPath);

        // TODO: 2. 后续可以添加实际推送
        // - WebSocket 推送到前端
        // - 微信/企业微信推送
        // - 邮件通知
        // - 短信通知
    }
}
