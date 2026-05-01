package streaming.ai;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AIClient {

    @Value("${ai.python.url:http://localhost:5001}")
    private String pythonApiUrl;

    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        log.info("AI Client 初始化完成, Python服务地址: {}", pythonApiUrl);
    }

    /**
     * 检测图片 - 调用 Python AI 引擎
     * @param imageBytes 图片字节数组
     * @return 检测结果
     */
    public DetectResult detect(byte[] imageBytes) throws IOException {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "frame.jpg",
                        RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(pythonApiUrl + "/detect")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Python AI 调用失败: " + response.code());
            }
            String json = response.body().string();
            JsonNode node = objectMapper.readTree(json);
            return parseDetectResult(node);
        }
    }

    /**
     * 注册人脸
     */
    public RegisterResult registerFace(String name, byte[] photoBytes) throws IOException {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", name)
                .addFormDataPart("file", "face.jpg",
                        RequestBody.create(photoBytes, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(pythonApiUrl + "/face/register")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String json = response.body().string();
            JsonNode node = objectMapper.readTree(json);
            boolean success = node.get("code").asInt() == 200;
            String msg = node.get("msg").asText();
            return new RegisterResult(success, msg);
        }
    }

    /**
     * 获取人脸列表
     */
    public List<FaceInfo> listFaces() throws IOException {
        Request request = new Request.Builder()
                .url(pythonApiUrl + "/face/list")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String json = response.body().string();
            JsonNode node = objectMapper.readTree(json);
            List<FaceInfo> faces = new ArrayList<>();
            if (node.get("code").asInt() == 200) {
                for (JsonNode item : node.get("data")) {
                    faces.add(new FaceInfo(
                            item.get("id").asText(),
                            item.get("name").asText()
                    ));
                }
            }
            return faces;
        }
    }

    /**
     * 删除人脸
     */
    public boolean deleteFace(String faceId) throws IOException {
        Request request = new Request.Builder()
                .url(pythonApiUrl + "/face/" + faceId)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String json = response.body().string();
            JsonNode node = objectMapper.readTree(json);
            return node.get("code").asInt() == 200;
        }
    }

    /**
     * 健康检查
     */
    public boolean healthCheck() {
        try {
            Request request = new Request.Builder()
                    .url(pythonApiUrl + "/health")
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            return false;
        }
    }

    private DetectResult parseDetectResult(JsonNode node) {
        JsonNode data = node.get("data");
        return new DetectResult(
                data.get("has_stranger").asBoolean(),
                data.get("stranger_count").asInt(),
                data.get("fallen").asBoolean(),
                data.get("fall_score").asInt(),
                data.get("alert").asText()
        );
    }

    // ========== 内部类 ==========

    public static class DetectResult {
        private final boolean hasStranger;
        private final int strangerCount;
        private final boolean fallen;
        private final int fallScore;
        private final String alert;

        public DetectResult(boolean hasStranger, int strangerCount, boolean fallen, int fallScore, String alert) {
            this.hasStranger = hasStranger;
            this.strangerCount = strangerCount;
            this.fallen = fallen;
            this.fallScore = fallScore;
            this.alert = alert;
        }

        public boolean hasStranger() { return hasStranger; }
        public int getStrangerCount() { return strangerCount; }
        public boolean isFallen() { return fallen; }
        public int getFallScore() { return fallScore; }
        public String getAlert() { return alert; }
    }

    public static class RegisterResult {
        private final boolean success;
        private final String message;
        public RegisterResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class FaceInfo {
        private final String id;
        private final String name;
        public FaceInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }
        public String getId() { return id; }
        public String getName() { return name; }
    }
}
