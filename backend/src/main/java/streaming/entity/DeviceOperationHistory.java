package streaming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("device_operation_history")
public class DeviceOperationHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer deviceId;
    private String deviceName;
    private String deviceIp;
    private String operationType;
    private String operationDesc;
    private Integer operatorId;
    private String operatorName;
    private String detailBefore;
    private String detailAfter;
    private Date createdAt;
}
