package streaming.service;

import java.util.List;
import java.util.Map;

public interface IVisionDetectionService {

    /**
     * 初始化检测器
     */
    void init();

    /**
     * 检测图片中的人体
     * @param imageBytes 图片字节数组
     * @return 检测结果列表，每个人体包含位置信息
     */
    List<Map<String, Object>> detectPersons(byte[] imageBytes);

    /**
     * 检测器是否就绪
     */
    boolean isReady();

    /**
     * 获取状态信息
     */
    String getStatus();

    /**
     * 获取摄像头当前帧（用于AI检测）
     * @return 图片字节数组（JPEG格式）
     */
    byte[] getCurrentFrame();

    /**
     * 设置当前设备ID
     * @param deviceId 设备ID
     */
    void setCurrentDeviceId(Long deviceId);
}
