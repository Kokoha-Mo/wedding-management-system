package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.dto.BookResponseDTO;
import com.wedding.wedding_management_system.dto.CreateBookRequestDTO;
import com.wedding.wedding_management_system.dto.CustomerDTO;
import com.wedding.wedding_management_system.dto.UpdateBookDetailsRequestDTO;
import com.wedding.wedding_management_system.repository.BookRepository;
import com.wedding.wedding_management_system.service.BookService;
import com.wedding.wedding_management_system.service.ConsultationConvertService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/employee/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    private final BookRepository bookRepository;

    private final ConsultationConvertService convertService;

    @PostMapping
    public ResponseEntity<BookResponseDTO> create(
            @Valid @RequestBody CreateBookRequestDTO request) {

        BookResponseDTO result = bookService.createBook(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    // 從諮詢單轉預約
    @PostMapping("/from-consultation/{consultationId}")
    public ResponseEntity<?> convertFromConsultation(
            @PathVariable Integer consultationId,
            @RequestBody Map<String, String> payload) { // 🌟 接收前端傳來的 JSON

        try {
            // 🌟 取出前端填寫的伴侶姓名，以及可能被櫃檯人員修改的信箱與電話
            String partnerName = payload.get("partnerName");
            String email = payload.get("email");
            String tel = payload.get("tel");

            // 將所有資料一併傳給 Service 處理
            BookResponseDTO result = convertService.convertFromConsultation(consultationId, partnerName, email, tel);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (RuntimeException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "發生未知錯誤";

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", errorMsg);

            // 🌟 根據我們在 Service 層設定的錯誤前綴，精準回傳對應的 HTTP 狀態碼
            if (errorMsg.startsWith("EMAIL_EXISTS:") || errorMsg.startsWith("TEL_EXISTS:")) {
                // 這是業務邏輯的資源衝突，使用 warn 記錄即可，並回傳 409
                log.warn("轉預約失敗 (資料衝突) - Consultation ID: {}, 原因: {}", consultationId, errorMsg);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);

            } else if (errorMsg.startsWith("TEL_FORMAT:") || errorMsg.contains("找不到此諮詢單")
                    || errorMsg.contains("已經轉換過了")) {
                // 這是前端傳來的參數不合規，回傳 400
                log.warn("轉預約失敗 (無效請求) - Consultation ID: {}, 原因: {}", consultationId, errorMsg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

            } else {
                // 真正未知的系統嚴重錯誤，才使用 error 級別記錄並印出 stack trace，回傳 500
                log.error("轉預約發生非預期系統錯誤 - Consultation ID: " + consultationId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        }
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<List<CustomerDTO>> checkDuplicate(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String tel) {

        List<CustomerDTO> similar = bookService.findSimilarCustomers(email, tel);
        return ResponseEntity.ok(similar);
    }

    @GetMapping
    public ResponseEntity<List<BookResponseDTO>> findByStatus(
            @RequestParam(defaultValue = "處理中") String status,
            @RequestParam(required = false) Integer managerId) {

        List<BookResponseDTO> books = (managerId !=null)
                ? bookService.findByManagerAndStatus(managerId,status)
                : bookService.findByStatus(status);

        return ResponseEntity.ok(books);
    }

    @PatchMapping("{id}/update") // 只更新一個欄位
    public ResponseEntity<BookResponseDTO> updateStatus(@PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        Integer managerId = (body.get("managerId") != null) ? Integer.valueOf(body.get("managerId")) : null;
        BookResponseDTO result = bookService.updateStatus(id, body.get("status"),managerId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/info")
    public ResponseEntity<BookResponseDTO> updateBookInfo(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateBookDetailsRequestDTO request) {
        BookResponseDTO result = bookService.updateBookInfo(id, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status-counts")
    public ResponseEntity<Map<String, Long>> statusCounts(
            @RequestParam(required = false) Integer managerId) {
        Map<String, Long> counts = (managerId != null)
                ? bookService.statusCountsByManager(managerId)
                : Map.of(
                "處理中", bookRepository.countByStatus("處理中"),
                "已簽約", bookRepository.countByStatus("已簽約"),
                "取消",   bookRepository.countByStatus("取消")
        );
        return ResponseEntity.ok(counts);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

}
