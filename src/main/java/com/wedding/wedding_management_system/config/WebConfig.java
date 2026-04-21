package com.wedding.wedding_management_system.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.wedding.wedding_management_system.interceptor.IpRateLimitInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private IpRateLimitInterceptor ipRateLimitInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 🌟 告訴 Spring Boot：只要有人請求網址開頭是 /uploads/ 的
        // 就請你幫我把門打開，去專案根目錄底下的 uploads/ 資料夾找檔案給他！
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 告訴 Spring：只要是打忘記密碼的 API，都要先經過 IP 攔截器的檢查
        registry.addInterceptor(ipRateLimitInterceptor)
                .addPathPatterns("/api/customer/forgot-password");
    }
}