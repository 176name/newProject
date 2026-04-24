package streaming.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import streaming.entity.RecordingHistory;
import streaming.mapper.HistoryMapper;
import streaming.service.IHistoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HistoryServiceImpl implements IHistoryService {

    @Autowired
    private HistoryMapper historyMapper;

    @Override
    public Long startRecording(Integer deviceId, Integer userId, Long recordingId) {
        RecordingHistory history = new RecordingHistory();
        history.setRecordingId(recordingId);
        history.setDeviceId(deviceId);
        history.setUserId(userId);
        history.setAction("start");
        history.setStatus("running");
        history.setStartTime(new java.util.Date());
        history.setCreatedAt(new java.util.Date());
        history.setRecordType("manual");

        historyMapper.insert(history);
        return history.getId();
    }

    @Override
    public int stopRecordingSuccess(Long recordingId, Long fileSize, String filePath) {
        return historyMapper.updateSuccess(recordingId, fileSize, filePath);
    }

    @Override
    public int stopRecordingFailed(Long recordingId, String errorMessage) {
        return historyMapper.updateFailed(recordingId, errorMessage);
    }

    @Override
    public Map<String, Object> getRunningRecording(Integer deviceId) {
        RecordingHistory history = historyMapper.getRunningRecording(deviceId);
        if (history == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("recording_id", history.getRecordingId());
        result.put("start_time", history.getStartTime());
        result.put("device_id", history.getDeviceId());
        return result;
    }

    @Override
    public List<Map<String, Object>> getDeviceHistory(Integer deviceId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return historyMapper.getHistoryByDevice(deviceId, offset, pageSize);
    }

    @Override
    public int getHistoryCount(Integer deviceId) {
        return historyMapper.getHistoryCount(deviceId);
    }

    @Override
    public List<Map<String, Object>> getDeviceHistoryByType(Integer deviceId, String recordType, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return historyMapper.getHistoryByDeviceAndType(deviceId, recordType, offset, pageSize);
    }

    @Override
    public int getHistoryCountByType(Integer deviceId, String recordType) {
        return historyMapper.getHistoryCountByType(deviceId, recordType);
    }
}
