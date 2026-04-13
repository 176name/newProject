package streaming.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 该类负责将 application.yml 中 zlm 开头的配置项
 * 自动映射到 Java 对象的属性中
 */
@Data
@Component
@ConfigurationProperties(prefix = "zlm")
public class ZlmConfig {

    /** 对应 yml 中的 zlm.host */
    private String host;

    /** 对应 yml 中的 zlm.http-port */
    private int httpPort;

    /** 对应 yml 中的 zlm.secret */
    private String secret;
}
