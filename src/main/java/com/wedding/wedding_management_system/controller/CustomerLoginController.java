package com.wedding.wedding_management_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.wedding_management_system.dto.CustomerLoginDto;
import com.wedding.wedding_management_system.dto.CustomerLoginResponseDto;
import com.wedding.wedding_management_system.dto.ResetPasswordDto;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.repository.ProjectRepository;
import com.wedding.wedding_management_system.service.CustomerLoginService;
import com.wedding.wedding_management_system.service.CustomerService;
import com.wedding.wedding_management_system.service.EmailService;
import com.wedding.wedding_management_system.util.JwtToken;

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

    @Autowired
    private ProjectRepository projectRepository;

    @PostMapping("/login")
    public ResponseEntity<CustomerLoginResponseDto> login(@RequestBody CustomerLoginDto loginDto) {
        try {
            CustomerLoginResponseDto result = customerLoginService.login(loginDto);

            ResponseCookie jwtCookie = ResponseCookie.from("customerToken", result.getToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(60 * 60) // an hour
                    .sameSite("Lax")
                    .build();

            result.setToken(null);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(result);

        } catch (RuntimeException e) {
            CustomerLoginResponseDto errorBody = new CustomerLoginResponseDto();
            errorBody.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
        } catch (Exception e) {
            // 新增：攔截資料庫連線失敗等系統錯誤
            CustomerLoginResponseDto errorBody = new CustomerLoginResponseDto();
            errorBody.setMessage("系統連線異常，請稍後再試");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @CookieValue(value = "customerToken", required = false) String token) {

        if (token == null || !JwtToken.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String email = JwtToken.getEmail(token);
        Customer customer = customerService.findByEmail(email);

        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "查無此客戶"));
        }

        // 檢查帳號是否已被停用（即使 token 仍在有效期，也要在這裡擋住）
        try {
            customerLoginService.disableAccount(email);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }

        // 查詢是否有專案
        boolean hasProject = projectRepository.existsByBook_Customer_Id(customer.getId());

        return ResponseEntity.ok(Map.of(
                "name", customer.getName(),
                "customerId", customer.getId(),
                "hasProject", hasProject));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie expiredCookie = ResponseCookie.from("customerToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    // 登入後強制修改密碼
    @PostMapping("/update-password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @RequestBody Map<String, String> request,
            @CookieValue(value = "customerToken", required = false) String token) {

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
            // 把系統原生的錯誤訊息跟 StackTrace 都印出來！
            System.err.println("忘記密碼請求失敗，信箱 (" + email + ")");
            System.err.println("具體錯誤原因：" + e.getMessage());
            e.printStackTrace(); // 這行會把最底層的 Exception 完整印到 Cloud Run Logs 裡

        }

        return ResponseEntity.ok(Map.of("message", "若該電子郵件已註冊，您將在幾分鐘內收到重設密碼信件。"));
    }

    // ── 停用帳號 ──
    @PostMapping("/disable-account")
    public ResponseEntity<Map<String, String>> disableAccount(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            customerLoginService.disableAccount(email);
            return ResponseEntity.ok(Map.of("message", "此帳號目前未被停用"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}