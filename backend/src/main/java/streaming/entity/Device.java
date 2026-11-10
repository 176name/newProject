package streaming.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device")
public class Device {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String brand;
    private String ip;
    private String username;
    private String password;  // 加密后的密码
    private Integer channel;
    private String rtspUrl;
    private String streamId;
    private Integer status;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime deletedAt;
}
