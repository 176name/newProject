package streaming.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /screenshots/** 到本地 screenshots 目录
        registry.addResourceHandler("/screenshots/**")
                .addResourceLocations("file:screenshots/");
    }
}
