package streaming.service;

import streaming.domain.Result;
import streaming.entity.Device;

import java.util.List;

public interface IStreamService {

    /**
     * 根据摄像头IP开启拉流代理
     */
    Result startPush(String cameraIp);

    /**
     * 根据设备ID开启拉流代理
     */
    Result startPushByDeviceId(Long deviceId);

    /**
     * 获取所有设备列表
     */
    List<Device> getAllDevices();

    /**
     * 添加新设备
     */
    Result addDevice(Device device);

    /**
     * 删除设备
     */
    Result deleteDevice(Long id);
}
