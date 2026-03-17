package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.dto.ConsultationRequestDTO;
import com.wedding.wedding_management_system.entity.Consultation;
import com.wedding.wedding_management_system.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // 回傳的資料會自動轉成 JSON
@RequestMapping("/api/consultations") // Controller 統一網址前綴
@RequiredArgsConstructor // Lombok 自動生成建構子，用來注入 Service
@CrossOrigin(origins = "*") // 🚧 開發階段小撇步：允許跨網域請求，避免 Vue 和 Spring Boot 跑在不同 Port 時被瀏覽器擋下來 (CORS)
public class ConsultationController {

    private final ConsultationService consultationService;

    /**
     * 獲取所有諮詢單列表 (供 櫃台人員 諮詢單管理 使用)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllConsultations() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 呼叫 Service 撈出所有資料
            List<Consultation> consultations = consultationService.getAllConsultations();
            
            response.put("success", true);
            response.put("data", consultations); // 把資料塞進 data 欄位
            
            return ResponseEntity.ok(response); // 回傳 HTTP 200 OK
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "獲取諮詢單資料失敗，請稍後再試。");
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 接收客戶端傳來的諮詢表單
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitConsultation(@RequestBody ConsultationRequestDTO requestDTO) {

        // 準備一個 Map 來包裝回傳給前端的 JSON 格式
        Map<String, Object> response = new HashMap<>();

        try {
            // 將前端傳來的 JSON (已轉成 DTO) 交給 Service 處理
            Consultation savedConsultation = consultationService.createConsultation(requestDTO);

            // 成功時的回應
            response.put("success", true);
            response.put("message", "諮詢單送出成功！我們的婚顧團隊將盡快與您聯絡。");
            response.put("consultationId", savedConsultation.getId()); // 可以回傳生成的 ID 給前端確認

            // 回傳 HTTP 狀態碼 201 (Created)
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            // 發生錯誤時的防呆機制
            response.put("success", false);
            response.put("message", "哎呀！系統發生了一點小狀況，請稍後再試。");
            response.put("error", e.getMessage());

            // 回傳 HTTP 狀態碼 500 (Internal Server Error)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新諮詢單狀態
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Integer id, 
            @RequestBody Map<String, String> requestBody) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 從前端傳來的 JSON 中取出 "status" 的值
            String newStatus = requestBody.get("static");
            
            // 基礎的防呆驗證
            if (newStatus == null || newStatus.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "狀態值不能為空！");
                return ResponseEntity.badRequest().body(response); // 回傳 400 Bad Request
            }

            // 呼叫 Service 執行更新邏輯
            Consultation updatedConsultation = consultationService.updateStatus(id, newStatus);
            
            response.put("success", true);
            response.put("message", "狀態更新成功！");
            response.put("data", updatedConsultation);
            
            return ResponseEntity.ok(response); // 回傳 200 OK
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新狀態時發生錯誤：" + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 回傳 500
        }
    }
}