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
import com.wedding.wedding_management_system.dto.CustomerLoginResponseDto;
import com.wedding.wedding_management_system.service.CustomerLoginService;

@RestController
@RequestMapping("/api/customer")
public class CustomerLoginController {

    @Autowired
    private CustomerLoginService customerLoginService;

    @PostMapping("/login")
    public ResponseEntity<CustomerLoginResponseDto> login(@RequestBody CustomerLoginDto loginDto) {
        try {
            // 1. 呼叫 Service 進行登入驗證，成功回傳包含 token、email、name 的 DTO
            CustomerLoginResponseDto result = customerLoginService.login(loginDto);

            // 2. 建立 HttpOnly Cookie，Controller 從 DTO 拿 token 來設定
            ResponseCookie jwtCookie = ResponseCookie.from("jwtToken", result.getToken())
                    .httpOnly(true)
                    .secure(false) // 只有 HTTPS 時才能送出，本地開發先設為 false
                    .path("/") // 整個網站都可以帶這個 Cookie
                    .maxAge(10 * 60) // 秒
                    .build();

            // 3. token 是給 Cookie 用的，不需要回傳給前端，把它清空
            result.setToken(null);

            // 4. 將 Cookie 加到 Response Header 中並回傳給前端
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(result);

        } catch (RuntimeException e) {
            // 如果 Service 拋出例外（像是密碼錯誤或帳號不存在），回傳 401 Unauthorized
            // 錯誤時 name/email 為 null，只有 HTTP 401 狀態碼告知前端失敗
            CustomerLoginResponseDto errorBody = new CustomerLoginResponseDto();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie expiredCookie = ResponseCookie.from("jwtToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // 刪除jwtToken cookie
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

}
