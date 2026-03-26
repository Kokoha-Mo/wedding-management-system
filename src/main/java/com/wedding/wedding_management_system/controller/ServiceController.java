package com.wedding.wedding_management_system.controller; // 記得確認你的 package 路徑是否正確

import com.wedding.wedding_management_system.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final BookService bookService;

    // 負責處理 GET /api/services 的請求
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllServices() {
        List<Map<String, Object>> pricingList = bookService.getAllServicesPricing();
        return ResponseEntity.ok(pricingList);
    }
}