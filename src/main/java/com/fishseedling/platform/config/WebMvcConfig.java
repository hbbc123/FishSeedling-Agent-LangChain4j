package com.fishseedling.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.path}")
    private String uploadPath;

    /**
     * 静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 文件上传路径映射
        // 由于context-path是/api，这里配置/files/**即可
        // 最终访问路径会是 /api/files/**
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadPath);
    }
}





