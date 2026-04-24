package streaming.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import streaming.domain.Result;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Value("${qwen.api-key:}")
    private String qwenApiKey;

    /**
     * AI 流量分析接口
     */
    @PostMapping("/analyze-traffic")
    public Result analyzeTraffic(@RequestBody TrafficAnalyzeRequest request) {
        if (qwenApiKey == null || qwenApiKey.isEmpty()) {
            return Result.error("请先配置千问 API Key（在 application.yml 中设置 qwen.api-key）");
        }

        String prompt = buildTrafficPrompt(request);
        log.info("开始AI分析，prompt长度: {}", prompt.length());

        try {
            String aiResponse = callQwenAPI(prompt);
            log.info("AI分析成功，响应长度: {}", aiResponse.length());
            TrafficAnalyzeResult result = parseTrafficResponse(aiResponse, request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("AI分析失败: {}", e.getMessage(), e);
            return Result.error("AI分析失败: " + e.getMessage());
        }
    }

    /**
     * 调用千问 API
     */
    private String callQwenAPI(String prompt) {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        JSONObject body = new JSONObject();
        body.put("model", "qwen-turbo");

        JSONObject input = new JSONObject();
        JSONArray messages = new JSONArray();

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个专业的视频监控流量分析专家，擅长分析摄像头流量数据、码率优化和成本控制。请用简洁专业的语言回答，分点列出。");
        messages.add(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        input.put("messages", messages);
        body.put("input", input);

        JSONObject parameters = new JSONObject();
        parameters.put("result_format", "message");
        body.put("parameters", parameters);

        log.info("调用千问API，url: {}", url);

        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + qwenApiKey)
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .timeout(60000)
                .execute();

        log.info("千问API响应状态码: {}", response.getStatus());

        if (response.getStatus() != 200) {
            log.error("千问API返回非200状态码: {}, 响应内容: {}", response.getStatus(), response.body());
            throw new RuntimeException("千问API调用失败，状态码: " + response.getStatus());
        }

        String responseBody = response.body();
        log.info("千问API响应内容: {}", responseBody);

        JSONObject resp = JSON.parseObject(responseBody);

        // 检查是否有错误
        if (resp.containsKey("code") && resp.getIntValue("code") != 0) {
            String errorMsg = resp.getString("message");
            log.error("千问API返回错误: code={}, message={}", resp.getIntValue("code"), errorMsg);
            throw new RuntimeException("千问API错误: " + errorMsg);
        }

        JSONObject output = resp.getJSONObject("output");
        if (output == null) {
            log.error("千问API响应缺少output字段");
            throw new RuntimeException("千问API响应格式异常");
        }

        // 修复：choices 是数组，不是对象
        JSONArray choicesArray = output.getJSONArray("choices");
        if (choicesArray == null || choicesArray.isEmpty()) {
            log.error("千问API响应choices为空");
            throw new RuntimeException("千问API响应数据为空");
        }

        JSONObject firstChoice = choicesArray.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        if (message == null) {
            log.error("千问API响应缺少message字段");
            throw new RuntimeException("千问API响应格式异常");
        }

        String content = message.getString("content");
        if (content == null || content.isEmpty()) {
            log.error("千问API响应content为空");
            throw new RuntimeException("千问API返回内容为空");
        }

        return content;
    }

    /**
     * 构建流量分析 Prompt
     */
    private String buildTrafficPrompt(TrafficAnalyzeRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下摄像头流量数据：\n\n");

        sb.append("【整体概况】\n");
        sb.append("- 今日总流量：").append(request.totalTraffic).append(" GB\n");
        sb.append("- 今日费用：").append(request.todayCost).append(" 元\n");
        sb.append("- 平均码率：").append(request.avgBitrate).append(" kbps\n");
        sb.append("- 峰值带宽：").append(request.peakBandwidth).append(" Mbps\n");
        sb.append("- 峰值时段：").append(request.peakTime).append("\n\n");

        sb.append("【各摄像头流量详情】\n");
        for (int i = 0; i < request.cameraRank.size(); i++) {
            CameraTraffic cam = request.cameraRank.get(i);
            sb.append((i+1)).append(". ").append(cam.name).append("（").append(cam.location).append("）\n");
            sb.append("   - 流量：").append(cam.traffic).append(" GB\n");
            sb.append("   - 码率：").append(cam.bitrate).append(" kbps\n");
            sb.append("   - 费用：").append(cam.cost).append(" 元\n");
        }
        sb.append("\n");

        sb.append("【24小时流量趋势】\n");
        for (int i = 0; i < request.trafficTrend.size(); i++) {
            TrafficPoint point = request.trafficTrend.get(i);
            sb.append("- ").append(point.time).append("：").append(point.value).append(" Mbps\n");
        }
        sb.append("\n");

        sb.append("请从以下五个方面给出分析报告：\n");
        sb.append("1. 整体流量评估（流量是否正常、费用是否合理、峰值时段分析）\n");
        sb.append("2. 高流量摄像头分析（哪些摄像头流量异常高、原因分析）\n");
        sb.append("3. 码率分析（哪些摄像头码率过高、优化空间）\n");
        sb.append("4. 优化建议（3-5条具体可执行的建议，每条建议要说明预期效果）\n");
        sb.append("5. 预警信息（如有需要紧急关注的问题）\n");

        return sb.toString();
    }

    /**
     * 解析 AI 返回结果
     */
    private TrafficAnalyzeResult parseTrafficResponse(String response, TrafficAnalyzeRequest request) {
        TrafficAnalyzeResult result = new TrafficAnalyzeResult();

        // 提取各个部分
        result.overview = extractSection(response, "整体流量评估", "高流量摄像头分析");
        result.highTraffic = extractSection(response, "高流量摄像头分析", "码率分析");
        result.bitrateAnalysis = extractSection(response, "码率分析", "优化建议");

        // 提取优化建议
        result.suggestions = new ArrayList<>();
        String suggestionsText = extractSection(response, "优化建议", "预警信息");
        if (suggestionsText != null && !suggestionsText.isEmpty()) {
            String[] lines = suggestionsText.split("\n");
            for (String line : lines) {
                String clean = line.trim();
                if (clean.startsWith("-") || clean.startsWith("•") || clean.matches("^\\d+\\..*")) {
                    clean = clean.replaceAll("^[-•\\d.\\s]+", "").trim();
                    if (clean.length() > 3) {
                        result.suggestions.add(clean);
                    }
                }
            }
        }

        // 提取预警信息
        result.alert = extractSection(response, "预警信息", null);
        if (result.alert == null || result.alert.isEmpty()) {
            result.alert = extractAlertFromText(response);
        }

        return result;
    }

    private String extractSection(String text, String startMark, String endMark) {
        int start = text.indexOf(startMark);
        if (start == -1) return "暂无数据";
        start = start + startMark.length();

        int end = text.length();
        if (endMark != null) {
            int endIdx = text.indexOf(endMark, start);
            if (endIdx != -1) end = endIdx;
        }

        String section = text.substring(start, end);
        return section.trim().replaceAll("^[：:\\s]+", "");
    }

    private String extractAlertFromText(String text) {
        if (text.contains("预警") || text.contains("警告") || text.contains("异常") || text.contains("超标")) {
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.contains("预警") || line.contains("警告") || line.contains("异常")) {
                    return line.trim();
                }
            }
        }
        return null;
    }

    // ========== 内部类 ==========
    public static class TrafficAnalyzeRequest {
        public double totalTraffic;
        public double todayCost;
        public int avgBitrate;
        public double peakBandwidth;
        public String peakTime;
        public List<CameraTraffic> cameraRank;
        public List<TrafficPoint> trafficTrend;
    }

    public static class CameraTraffic {
        public String name;
        public String location;
        public double traffic;
        public int bitrate;
        public double cost;
    }

    public static class TrafficPoint {
        public String time;
        public double value;
    }

    public static class TrafficAnalyzeResult {
        public String overview;
        public String highTraffic;
        public String bitrateAnalysis;
        public List<String> suggestions;
        public String alert;
    }
}
