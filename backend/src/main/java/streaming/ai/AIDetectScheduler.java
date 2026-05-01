package streaming.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import streaming.service.impl.VisionDetectionServiceImpl;

@Slf4j
@Component
@EnableScheduling
public class AIDetectScheduler {

    @Autowired
    private AIClient aiClient;

    @Autowired
    private VisionDetectionServiceImpl visionDetectionService;

    @Autowired
    private AlertService alertService;

    @Value("${ai.detect.device-id:1}")
    private Long defaultDeviceId;

    // 摔倒检测状态
    private int fallConsecutiveCount = 0;
    private Long fallConfirmedTime = null;
    private byte[] lastFallScreenshot = null;

    // 陌生人去重
    private Long lastStrangerTime = null;

    /**
     * 每秒抽帧检测
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void detect() {
        try {
            // 设置当前设备ID
            visionDetectionService.setCurrentDeviceId(defaultDeviceId);

            // 获取当前帧
            byte[] frame = visionDetectionService.getCurrentFrame();
            if (frame == null) {
                log.debug("获取帧失败");
                return;
            }

            // 调用 Python AI 检测
            AIClient.DetectResult result = aiClient.detect(frame);
            if (result == null) {
                log.debug("AI检测失败");
                return;
            }

            log.debug("AI检测结果: hasStranger={}, fallen={}", result.hasStranger(), result.isFallen());

            // 处理陌生人
            if (result.hasStranger()) {
                handleStranger(frame, result);
            }

            // 处理摔倒
            if (result.isFallen()) {
                handleFall(frame, result);
            } else {
                resetFallState();
            }

        } catch (Exception e) {
            log.error("AI检测异常: {}", e.getMessage());
        }
    }

    private void handleStranger(byte[] frame, AIClient.DetectResult result) {
        long now = System.currentTimeMillis();
        if (lastStrangerTime != null && (now - lastStrangerTime) < 30 * 60 * 1000) {
            return; // 30分钟内不重复告警
        }

        // 保存截图
        String screenshotPath = alertService.saveScreenshot(frame, "stranger");

        // 发送告警
        alertService.sendAlert("陌生人闯入", screenshotPath);

        lastStrangerTime = now;
        log.warn("⚠️ 陌生人闯入！截图: {}", screenshotPath);
    }

    private void handleFall(byte[] frame, AIClient.DetectResult result) {
        fallConsecutiveCount++;

        // 连续5帧确认摔倒
        if (fallConsecutiveCount >= 5 && fallConfirmedTime == null) {
            fallConfirmedTime = System.currentTimeMillis();
            lastFallScreenshot = frame;
            alertService.saveScreenshot(frame, "fall_confirmed");
            log.warn("⚠️ 确认摔倒，开始30秒计时...");
        }

        // 确认后30秒告警
        if (fallConfirmedTime != null) {
            long elapsed = System.currentTimeMillis() - fallConfirmedTime;
            if (elapsed >= 30 * 1000) {
                String screenshotPath = alertService.saveScreenshot(lastFallScreenshot, "fall_alert");
                alertService.sendAlert("老人摔倒30秒未起身", screenshotPath);
                log.warn("⚠️ 摔倒告警已发送！");
                resetFallState();
            }
        }
    }

    private void resetFallState() {
        fallConsecutiveCount = 0;
        fallConfirmedTime = null;
        lastFallScreenshot = null;
    }
}
