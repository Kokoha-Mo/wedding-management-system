package com.wedding.wedding_management_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.wedding_management_system.dto.CustomerLoginDto;
import com.wedding.wedding_management_system.service.CustomerLoginService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerLoginController {

    @Autowired
    private CustomerLoginService customerLoginService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody CustomerLoginDto loginDto) {
        try {
            // 1. 呼叫 Service 進行登入驗證，成功會回傳 JWT Token
            String token = customerLoginService.login(loginDto);

            // 2. 建立 HttpOnly Cookie，將 JWT 放入其中
            //    這裡的 Max-Age 單位是秒 (10 * 60 = 600 秒)
            ResponseCookie jwtCookie = ResponseCookie.from("jwtToken", token)
                    .httpOnly(true)
                    .secure(false) // 只有 HTTPS 時才能送出，本地開發先設為 false
                    .path("/")     // 整個網站都可以帶這個 Cookie
                    .maxAge(10 * 60)
                    .build();

            // 3. 準備回傳給前端的 Body 資料 
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "客戶登入成功");
            responseBody.put("email", loginDto.getEmail());

            // 4. 將 Cookie 加到 Response Header 中並回傳給前端
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(responseBody);

        } catch (RuntimeException e) {
            // 如果 Service 拋出例外（像是密碼錯誤或帳號不存在），回傳 401 Unauthorized
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
        }
    }
}
