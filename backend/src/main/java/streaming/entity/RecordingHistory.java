// RecordingHistory.java
package streaming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("recording_history")
public class RecordingHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordingId;
    private Integer deviceId;
    private Integer userId;
    private String action;
    private String status;
    private Date startTime;
    private Date endTime;
    private Integer durationSeconds;
    private String errorMessage;
    private Long fileSize;
    private String filePath;
    private Date createdAt;
    private String recordType;
}
