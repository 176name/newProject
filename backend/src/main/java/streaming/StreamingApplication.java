package streaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类：负责启动整个 Spring 容器
 * 它会自动扫描 streaming 包及其子包下的所有注解（@Component, @Service, @RestController等）
 */
@SpringBootApplication
public class StreamingApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamingApplication.class, args);
        System.out.println("========================================");
        System.out.println("   流媒体后端服务点火成功！监听端口: 8081   ");
        System.out.println("========================================");
    }
}
