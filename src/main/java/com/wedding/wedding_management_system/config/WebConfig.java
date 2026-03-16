package com.wedding.wedding_management_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 🌟 告訴 Spring Boot：只要有人請求網址開頭是 /uploads/ 的
        // 就請你幫我把門打開，去專案根目錄底下的 uploads/ 資料夾找檔案給他！
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}