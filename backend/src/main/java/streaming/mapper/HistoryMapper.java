package streaming.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import streaming.entity.RecordingHistory;

import java.util.List;
import java.util.Map;

@Mapper
public interface HistoryMapper extends BaseMapper<RecordingHistory> {

    @Select("SELECT * FROM recording_history WHERE device_id = #{deviceId} AND status = 'running' LIMIT 1")
    RecordingHistory getRunningRecording(@Param("deviceId") Integer deviceId);

    @Update("UPDATE recording_history SET " +
            "action = 'stop', status = 'success', end_time = NOW(), " +
            "duration_seconds = TIMESTAMPDIFF(SECOND, start_time, NOW()), " +
            "file_size = #{fileSize}, file_path = #{filePath} " +
            "WHERE recording_id = #{recordingId} AND status = 'running'")
    int updateSuccess(@Param("recordingId") Long recordingId,
                      @Param("fileSize") Long fileSize,
                      @Param("filePath") String filePath);

    @Update("UPDATE recording_history SET " +
            "action = 'stop', status = 'failed', end_time = NOW(), " +
            "error_message = #{errorMessage} " +
            "WHERE recording_id = #{recordingId} AND status = 'running'")
    int updateFailed(@Param("recordingId") Long recordingId,
                     @Param("errorMessage") String errorMessage);

    @Select("SELECT h.*, d.name as device_name " +
            "FROM recording_history h " +
            "LEFT JOIN device d ON h.device_id = d.id " +
            "WHERE h.device_id = #{deviceId} " +
            "ORDER BY h.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> getHistoryByDevice(@Param("deviceId") Integer deviceId,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM recording_history WHERE device_id = #{deviceId}")
    int getHistoryCount(@Param("deviceId") Integer deviceId);

    // 新增：按类型查询
    @Select("SELECT h.*, d.name as device_name " +
            "FROM recording_history h " +
            "LEFT JOIN device d ON h.device_id = d.id " +
            "WHERE h.device_id = #{deviceId} AND h.record_type = #{recordType} AND h.status = 'success' " +
            "ORDER BY h.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> getHistoryByDeviceAndType(@Param("deviceId") Integer deviceId,
                                                        @Param("recordType") String recordType,
                                                        @Param("offset") int offset,
                                                        @Param("limit") int limit);

    // 新增：按类型统计
    @Select("SELECT COUNT(*) FROM recording_history WHERE device_id = #{deviceId} AND record_type = #{recordType} AND status = 'success'")
    int getHistoryCountByType(@Param("deviceId") Integer deviceId, @Param("recordType") String recordType);
}
