package com.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final com.blog.service.AvatarStorage avatars;

    public WebConfig(com.blog.service.AvatarStorage avatars) { this.avatars = avatars; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 头像
        String location = avatars.directory().toUri().toString();
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");

        //封面上传目录（方案一：项目根目录）
        String projectPath = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + projectPath);
    }
}
