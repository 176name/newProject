package streaming.service;

import streaming.domain.Result;

public interface IStreamService {
    /**
     * 根据摄像头IP开启拉流代理
     */
    Result startPush(String cameraIp);
}
