package com.wedding.wedding_management_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 套用到所有路由 API
                .allowedOriginPatterns("*")
                // 開放指定網域或全域(*)。
                // 注意：當allowCredentials 為true時，
                // 不能使用allowedOrigins("*")，要改用allowedOriginPatterns("*")

                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*") // 允許所有請求標頭
                .allowCredentials(true) // 允許前端傳送 Cookie (包含 JWT Token 的 HttpOnly Cookie)
                .maxAge(3600);
    }
}
