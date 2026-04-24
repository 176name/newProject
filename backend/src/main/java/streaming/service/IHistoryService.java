package streaming.service;

import java.util.List;
import java.util.Map;

public interface IHistoryService {

    Long startRecording(Integer deviceId, Integer userId, Long recordingId);

    int stopRecordingSuccess(Long recordingId, Long fileSize, String filePath);

    int stopRecordingFailed(Long recordingId, String errorMessage);

    Map<String, Object> getRunningRecording(Integer deviceId);

    List<Map<String, Object>> getDeviceHistory(Integer deviceId, int page, int pageSize);

    int getHistoryCount(Integer deviceId);
    List<Map<String, Object>> getDeviceHistoryByType(Integer deviceId, String recordType, int page, int pageSize);
    int getHistoryCountByType(Integer deviceId, String recordType);
}
