package streaming.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import streaming.entity.Device;
import streaming.mapper.DeviceMapper;
import streaming.service.IVisionDetectionService;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

@Slf4j
@Service
public class VisionDetectionServiceImpl implements IVisionDetectionService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private streaming.config.ZlmConfig zlmConfig;

    private CascadeClassifier detector;
    private boolean ready = false;
    private String status = "未初始化";

    // 缓存每个设备的抓图器（避免重复连接）
    private final Map<Long, FFmpegFrameGrabber> grabberCache = new ConcurrentHashMap<>();
    private Long currentDeviceId = null;

    @Override
    @PostConstruct
    public void init() {
        try {
            log.info("正在初始化人体检测器...");
            String modelPath = copyModelToTemp();
            detector = new CascadeClassifier(modelPath);
            if (!detector.empty()) {
                ready = true;
                status = "就绪";
                log.info("人体检测器初始化成功");
            } else {
                status = "模型加载失败";
                log.warn("人体检测器加载失败");
            }
        } catch (Exception e) {
            log.error("人体检测器初始化失败: {}", e.getMessage());
            ready = false;
            status = "初始化失败: " + e.getMessage();
        }
    }

    private String copyModelToTemp() throws Exception {
        String resourcePath = "/body.xml";
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("找不到模型文件: " + resourcePath);
        }
        File tempFile = File.createTempFile("haarcascade_fullbody", ".xml");
        tempFile.deleteOnExit();
        Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile.getAbsolutePath();
    }

    @Override
    public List<Map<String, Object>> detectPersons(byte[] imageBytes) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (!ready || detector == null) {
            return results;
        }
        try {
            Mat img = imdecode(new Mat(imageBytes), 1);
            if (img.empty()) {
                return results;
            }
            Mat gray = new Mat();
            cvtColor(img, gray, COLOR_BGR2GRAY);
            RectVector bodies = new RectVector();
            detector.detectMultiScale(gray, bodies);
            for (int i = 0; i < bodies.size(); i++) {
                Rect rect = bodies.get(i);
                Map<String, Object> person = new HashMap<>();
                person.put("x", rect.x());
                person.put("y", rect.y());
                person.put("width", rect.width());
                person.put("height", rect.height());
                results.add(person);
            }
            if (!results.isEmpty()) {
                log.debug("检测到 {} 个人体", results.size());
            }
        } catch (Exception e) {
            log.error("人体检测失败: {}", e.getMessage());
        }
        return results;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setCurrentDeviceId(Long deviceId) {
        this.currentDeviceId = deviceId;
    }

    @Override
    public byte[] getCurrentFrame() {
        if (currentDeviceId == null) {
            log.debug("未设置当前设备ID，无法抓图");
            return null;
        }

        Device device = deviceMapper.selectById(currentDeviceId);
        if (device == null) {
            log.warn("设备不存在: id={}", currentDeviceId);
            return null;
        }

        // 构建 RTSP 地址（与 StreamServiceImpl 的三种模式保持一致）
        String sourceUrl = device.getRtspUrl();
        String rtspUrl;
        if (sourceUrl != null && sourceUrl.startsWith("push://")) {
            // 模式1：外部推流 → 从 ZLM 的输出流抓帧
            String pushStream = sourceUrl.substring(7);
            rtspUrl = String.format("rtsp://%s:554/%s", zlmConfig.getHost(), pushStream);
        } else if (sourceUrl != null && !sourceUrl.isEmpty()) {
            // 模式2：自定义拉流地址
            rtspUrl = sourceUrl;
        } else {
            // 模式3：海康默认格式（密码如为 AES 加密，此处需先解密）
            String password = device.getPassword();
            rtspUrl = String.format("rtsp://%s:%s@%s:554/Streaming/Channels/%d",
                    device.getUsername(), password, device.getIp(), device.getChannel());
        }

        log.debug("抓帧 RTSP: {}", rtspUrl.replaceAll(":[^:]*@", ":***@")); // 隐藏密码

        return grabFrameFromRtsp(device.getId(), rtspUrl);
    }

    /**
     * 从 RTSP 地址抓取一帧
     */
    private byte[] grabFrameFromRtsp(Long deviceId, String rtspUrl) {
        FFmpegFrameGrabber grabber = grabberCache.get(deviceId);

        // 如果抓图器不存在或已失效，重新创建
        if (grabber == null) {
            try {
                grabber = new FFmpegFrameGrabber(rtspUrl);
                grabber.setOption("rtsp_transport", "tcp");  // TCP 更稳定
                grabber.start();
                grabberCache.put(deviceId, grabber);
                log.info("✅ 已创建抓图器: deviceId={}", deviceId);
            } catch (Exception e) {
                log.error("创建抓图器失败: deviceId={}, error={}", deviceId, e.getMessage());
                return null;
            }
        }

        try {
            // 抓一帧
            Frame frame = grabber.grabImage();
            if (frame == null) {
                // 抓帧失败，尝试重连
                log.warn("抓帧失败，尝试重建抓图器... deviceId={}", deviceId);
                try {
                    grabber.stop();
                } catch (Exception e) {
                    // ignore
                }
                grabberCache.remove(deviceId);
                return grabFrameFromRtsp(deviceId, rtspUrl);
            }

            // 转为 JPEG 字节数组
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage bi = converter.convert(frame);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpg", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("抓帧异常: deviceId={}, error={}", deviceId, e.getMessage());
            return null;
        }
    }

    /**
     * 清理资源（可在应用关闭时调用）
     */
    public void destroy() {
        for (Map.Entry<Long, FFmpegFrameGrabber> entry : grabberCache.entrySet()) {
            try {
                entry.getValue().stop();
            } catch (Exception e) {
                log.warn("关闭抓图器失败: deviceId={}", entry.getKey());
            }
        }
        grabberCache.clear();
        log.info("已清理所有抓图器");
    }
}
