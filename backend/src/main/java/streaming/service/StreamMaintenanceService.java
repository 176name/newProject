package streaming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import streaming.entity.Device;
import streaming.mapper.DeviceMapper;
import streaming.service.impl.StreamServiceImpl;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@EnableScheduling
public class StreamMaintenanceService {

    @Value("${recording.auto.enabled:false}")
    private boolean autoRecordingEnabled;

    @Value("${recording.auto.retention-days:7}")
    private int retentionDays;

    @Autowired
    private StreamServiceImpl streamService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void autoStartStreams() {
        if (!autoRecordingEnabled) {
            log.info("24×7 全天录像未开启（recording.auto.enabled=false）");
            return;
        }

        log.info("========== 24×7 全天录像已开启，自动为所有设备建流 ==========");
        try {
            List<Device> devices = deviceMapper.selectList(
                    new LambdaQueryWrapper<Device>().isNull(Device::getDeletedAt));
            log.info("找到 {} 个设备，开始自动建流...", devices.size());

            for (Device device : devices) {
                try {
                    log.info("自动建流: device={}, ip={}", device.getName(), device.getIp());
                    streamService.startPush(device.getIp());
                } catch (Exception e) {
                    log.error("自动建流失败: device={}, error={}", device.getName(), e.getMessage());
                }
            }

            log.info("========== 自动建流完成 ==========");
        } catch (Exception e) {
            log.error("自动建流过程异常: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void healthCheck() {
        if (!autoRecordingEnabled) return;

        try {
            List<Device> onlineDevices = deviceMapper.selectList(
                    new LambdaQueryWrapper<Device>()
                            .eq(Device::getStatus, 1)
                            .isNull(Device::getDeletedAt));

            for (Device device : onlineDevices) {
                try {
                    // addStreamProxy 是幂等的——流已存在时 ZLM 返回 code=-1，不影响
                    streamService.startPush(device.getIp());
                } catch (Exception e) {
                    log.warn("健康检查重连失败: device={}, error={}", device.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("健康检查异常: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldRecordings() {
        if (!autoRecordingEnabled) {
            return;
        }

        log.info("========== 开始清理 {} 天前的自动录像 ==========", retentionDays);
        try {
            String querySql = "SELECT id, file_path FROM recording_history " +
                    "WHERE record_type = 'auto' AND status = 'success' " +
                    "AND created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(querySql, retentionDays);
            log.info("找到 {} 条过期录像记录", records.size());

            int deletedFiles = 0;
            int deletedRecords = 0;

            for (Map<String, Object> record : records) {
                Long id = ((Number) record.get("id")).longValue();
                String filePath = (String) record.get("file_path");

                // 删除磁盘文件
                if (filePath != null && !filePath.isEmpty()) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        if (file.delete()) {
                            deletedFiles++;
                            log.debug("已删除文件: {}", filePath);
                        } else {
                            log.warn("无法删除文件: {}", filePath);
                        }
                    }
                }

                // 删除数据库记录
                jdbcTemplate.update("DELETE FROM recording_history WHERE id = ?", id);
                deletedRecords++;
            }

            log.info("========== 清理完成：删除 {} 个文件，{} 条数据库记录 ==========", deletedFiles, deletedRecords);
        } catch (Exception e) {
            log.error("清理过期录像异常: {}", e.getMessage(), e);
        }
    }
}
