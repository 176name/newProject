package streaming.service;

import java.util.Map;

public interface IDeviceOperationHistoryService {

    /**
     * 记录设备操作历史
     */
    void record(Integer deviceId, String deviceName, String deviceIp,
                String operationType, String operationDesc,
                String detailBefore, String detailAfter);

    /**
     * 分页获取操作历史列表
     */
    Map<String, Object> getHistoryList(int page, int pageSize);
}
