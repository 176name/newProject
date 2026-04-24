package streaming.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import streaming.entity.Device;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}
