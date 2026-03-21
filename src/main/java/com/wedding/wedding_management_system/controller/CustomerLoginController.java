package com.wedding.wedding_management_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue; // 🌟 新增這個 Import
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.wedding_management_system.dto.CustomerLoginDto;
import com.wedding.wedding_management_system.dto.CustomerLoginResponseDto;
import com.wedding.wedding_management_system.dto.ResetPasswordDto;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.service.CustomerLoginService;
import com.wedding.wedding_management_system.service.CustomerService;
import com.wedding.wedding_management_system.service.EmailService;
import com.wedding.wedding_management_system.util.JwtToken; // 🌟 把註解拿掉！

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerLoginController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomerLoginService customerLoginService;

    @Autowired
    private CustomerService customerService;

    @PostMapping("/login")
    public ResponseEntity<CustomerLoginResponseDto> login(@RequestBody CustomerLoginDto loginDto) {
        try {
            CustomerLoginResponseDto result = customerLoginService.login(loginDto);

            ResponseCookie jwtCookie = ResponseCookie.from("jwtToken", result.getToken())
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(60 * 60) // an hour
                    .build();

            result.setToken(null);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(result);

        } catch (RuntimeException e) {
            CustomerLoginResponseDto errorBody = new CustomerLoginResponseDto();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @CookieValue(value = "jwtToken", required = false) String token) {

        if (token == null || !JwtToken.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String email = JwtToken.getEmail(token);
        Customer customer = customerService.findByEmail(email);

        return ResponseEntity.ok(Map.of(
                "name", customer.getName(),
                "customerId", customer.getId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie expiredCookie = ResponseCookie.from("jwtToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    // 登入後強制修改密碼
    @PostMapping("/update-password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @RequestBody Map<String, String> request,
            @CookieValue(value = "jwtToken", required = false) String token) {

        // 1. 確認客人已經成功登入了 (Cookie 裡有帶 Token)
        if (token == null || !JwtToken.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入"));
        }

        String email = JwtToken.getEmail(token);
        String newPassword = request.get("newPassword");

        try {
            // 2. 呼叫 Service 更新密碼
            customerLoginService.updatePasswordAfterLogin(email, newPassword);
            return ResponseEntity.ok(Map.of("message", "密碼設定成功！"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── 驗證重設密碼 token ──
    @GetMapping("/verify-reset-token")
    public ResponseEntity<Map<String, String>> verifyResetToken(
            @RequestParam("token") String token) {
        try {
            customerLoginService.verifyResetToken(token);
            return ResponseEntity.ok(Map.of("message", "token 有效"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── 重設密碼 ──
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordDto dto) {
        try {
            customerLoginService.resetPassword(dto.getToken(), dto.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "密碼已更新"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── 忘記密碼 / 補發重設密碼信 ──
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        try {
            String token = customerLoginService.generateAndSaveResetToken(email);
            Customer customer = customerService.findByEmail(email);
            emailService.sendResetPasswordEmail(email, customer.getName(), token);
        } catch (RuntimeException e) {
            System.out.println("忘記密碼請求：信箱不存在或發生錯誤 (" + email + ")");
        }

        return ResponseEntity.ok(Map.of("message", "若該電子郵件已註冊，您將在幾分鐘內收到重設密碼信件。"));
    }
}