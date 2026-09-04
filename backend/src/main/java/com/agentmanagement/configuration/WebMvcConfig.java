package com.agentmanagement.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置：上传文件静态映射。
 * /uploads/** → 磁盘 {file.upload-dir}/（当前用于用户头像 avatars/）。
 * 实际访问 URL 含 context-path：/api/v1/uploads/**，
 * SecurityConfig 已对 /uploads/** 放行匿名读取（&lt;img&gt; 无法携带 JWT）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // File.toURI() 保证末尾斜杠与特殊字符编码正确
        String location = new File(uploadDir).getAbsoluteFile().toURI().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
