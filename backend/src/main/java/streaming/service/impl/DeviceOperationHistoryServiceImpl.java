package streaming.service.impl;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import streaming.entity.DeviceOperationHistory;
import streaming.mapper.DeviceOperationHistoryMapper;
import streaming.service.IDeviceOperationHistoryService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeviceOperationHistoryServiceImpl implements IDeviceOperationHistoryService {

    @Autowired
    private DeviceOperationHistoryMapper historyMapper;

    @Override
    public void record(Integer deviceId, String deviceName, String deviceIp,
                       String operationType, String operationDesc,
                       String detailBefore, String detailAfter) {
        try {
            DeviceOperationHistory history = new DeviceOperationHistory();
            history.setDeviceId(deviceId);
            history.setDeviceName(deviceName);
            history.setDeviceIp(deviceIp);
            history.setOperationType(operationType);
            history.setOperationDesc(operationDesc);
            history.setOperatorId(1);
            history.setOperatorName("admin");
            history.setDetailBefore(detailBefore);
            history.setDetailAfter(detailAfter);
            history.setCreatedAt(new Date());

            historyMapper.insert(history);
            log.info("记录设备操作历史成功: {} - {}", operationType, deviceName);
        } catch (Exception e) {
            log.error("记录设备操作历史失败: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getHistoryList(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<DeviceOperationHistory> list = historyMapper.getHistoryList(offset, pageSize);
        int total = historyMapper.getHistoryCount();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }
}
