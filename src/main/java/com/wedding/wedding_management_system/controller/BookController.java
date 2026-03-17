package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.dto.BookResponseDTO;
import com.wedding.wedding_management_system.dto.CreateBookRequestDTO;
import com.wedding.wedding_management_system.dto.CustomerDTO;
import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.repository.BookRepository;
import com.wedding.wedding_management_system.repository.CustomerRepository;
import com.wedding.wedding_management_system.service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping ("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

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
            @RequestBody Map<String, String> payload) { // 🌟 新增：用來接收前端傳來的 JSON

        try {
            // 從 payload 中取出前端填寫的 partnerName
            String partnerName = payload.get("partnerName");
            
            // 將伴侶姓名一併傳給 Service 處理
            BookResponseDTO result = bookService.convertFromConsultation(consultationId, partnerName);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "轉預約失敗：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<List<CustomerDTO>> checkDuplicate(
            @RequestParam(required = false) String email) {

        List<CustomerDTO> similar = bookService.findSimilarCustomers(email);
        return ResponseEntity.ok(similar);
    }

    @GetMapping
    public ResponseEntity<List<BookResponseDTO>> findByStatus(
            @RequestParam(defaultValue = "處理中") String status) {

        List<BookResponseDTO> books = bookService.findByStatus(status);
        return ResponseEntity.ok(books);
    }

    @PatchMapping("{id}/update") //只更新一個欄位
    public ResponseEntity<BookResponseDTO>updateStatus(@PathVariable Integer id, @RequestBody Map<String,String>body){
        BookResponseDTO result = bookService.updateStatus(id, body.get("status"));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status-counts")
    public ResponseEntity<Map<String, Long>> statusCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("處理中", bookRepository.countByStatus("處理中"));
        counts.put("已簽約", bookRepository.countByStatus("已簽約"));
        counts.put("取消", bookRepository.countByStatus("取消"));
        return ResponseEntity.ok(counts);
    }




}
