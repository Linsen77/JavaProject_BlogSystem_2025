package com.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 头像
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("file:C:/Users/33132/Desktop/Java_2025/BlogSystem/avatar/");

        //封面上传目录（方案一：项目根目录）
        String projectPath = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + projectPath);
    }
}
