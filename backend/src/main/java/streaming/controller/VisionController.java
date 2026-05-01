package streaming.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import streaming.domain.Result;
import streaming.service.IVisionDetectionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vision")
public class VisionController {

    @Autowired
    private IVisionDetectionService visionDetectionService;

    /**
     * 人体检测 (原有的接口)
     */
    @PostMapping("/detect/person")
    public Result detectPerson(@RequestParam("image") MultipartFile file) {
        try {
            byte[] imageBytes = file.getBytes();
            List<Map<String, Object>> persons = visionDetectionService.detectPersons(imageBytes);

            Map<String, Object> result = new HashMap<>();
            result.put("hasPerson", !persons.isEmpty());
            result.put("personCount", persons.size());
            result.put("persons", persons);

            return Result.success(result);
        } catch (Exception e) {
            log.error("人体检测失败: {}", e.getMessage());
            return Result.error("检测失败: " + e.getMessage());
        }
    }

    /**
     * AI 报警接口 (Python 调用此接口，通过 JSON 传回报警信息)
     */
    @PostMapping("/alarm/trigger")
    public Result receiveAiAlarm(@RequestBody Map<String, Object> alarmData) {
        log.info("收到 AI 视觉分析服务报警: {}", alarmData);
        try {
            // 这里你可以根据业务逻辑，将报警记录入库
            // 例如: visionDetectionService.saveAlarmRecord(alarmData);

            return Result.success("报警处理成功");
        } catch (Exception e) {
            log.error("报警处理异常: {}", e.getMessage());
            return Result.error("处理异常: " + e.getMessage());
        }
    }

    /**
     * 获取检测器状态 (原有的接口)
     */
    @GetMapping("/status")
    public Result getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("ready", visionDetectionService.isReady());
        status.put("status", visionDetectionService.getStatus());
        return Result.success(status);
    }
}
