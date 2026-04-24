package streaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StreamingApplication {
    public static void main(String[] args) {
        // 强制设置属性，确保程序去找 127.0.0.1，密码锁死 123456
        System.setProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/streaming_db?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        System.setProperty("spring.datasource.username", "root");
        System.setProperty("spring.datasource.password", "123456");

        SpringApplication.run(StreamingApplication.class, args);
        System.out.println("========================================");
        System.out.println("   流媒体后端服务点火成功！监听端口: 8081   ");
        System.out.println("========================================");
    }
}
