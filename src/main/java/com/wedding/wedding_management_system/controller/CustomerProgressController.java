package com.wedding.wedding_management_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.wedding_management_system.dto.ProjectProgressDTO;
import com.wedding.wedding_management_system.service.CustomerProgressService;
import com.wedding.wedding_management_system.util.JwtToken;

@RestController
@RequestMapping("/api/customer/my-project")
public class CustomerProgressController {

    @Autowired
    private CustomerProgressService customerProgressService;

    /**
     * 取得當前登入客戶的籌備進度
     * 前端對應 API：GET /api/customer/my-project/progress
     */
    @GetMapping("/progress")
    public ResponseEntity<?> getMyProgress(HttpServletRequest request) {

        String token = null;

        // 🌟 關鍵修改：尋找名為 "customerToken" 的 Cookie
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("customerToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("找不到登入憑證 (Cookie缺失)");
        }

        // 接下來一樣用 JwtToken 工具解析
        if (!JwtToken.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token 無效或已過期");
        }

        String customerEmail = JwtToken.getEmail(token);

        try {
            // 正式呼叫 Service 拿資料！
            ProjectProgressDTO progressData = customerProgressService.getProgressByCustomerEmail(customerEmail);
            return ResponseEntity.ok(progressData);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 客戶新增留言與上傳附件
     */
    @PostMapping("/communication")
    public ResponseEntity<?> postCommunication(
            @RequestParam(value = "content", required = false) String content, // 🌟 關鍵：允許空字串(只有圖片時)
            @RequestParam(value = "createBy", required = false, defaultValue = "客戶") String createBy,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        // 1. 驗證身分 (延續我們剛才打通的 SecurityContext)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }

        String customerEmail = (String) authentication.getPrincipal();

        try {
            // 2. 呼叫 Service 處理儲存邏輯
            customerProgressService.addCommunicationByCustomerEmail(customerEmail, content, createBy, files);

            // 3. 成功回傳 (前端有寫 if (response.data.success) 會接這個)
            return ResponseEntity.ok().body("{\"success\": true}");

        } catch (RuntimeException e) {
            // 如果找不到專案，回傳 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            // 處理檔案上傳或其他不可預期錯誤
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"success\": false, \"message\": \"系統處理失敗\"}");
        }
    }
}