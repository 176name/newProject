package streaming.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import streaming.entity.DeviceOperationHistory;
import java.util.List;
import java.util.Map;

@Mapper
public interface DeviceOperationHistoryMapper extends BaseMapper<DeviceOperationHistory> {

    @Select("SELECT * FROM device_operation_history ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<DeviceOperationHistory> getHistoryList(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM device_operation_history")
    int getHistoryCount();

    @Select("SELECT * FROM device_operation_history WHERE device_id = #{deviceId} ORDER BY created_at DESC")
    List<DeviceOperationHistory> getHistoryByDeviceId(@Param("deviceId") Integer deviceId);
}
