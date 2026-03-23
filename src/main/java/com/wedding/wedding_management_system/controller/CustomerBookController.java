package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.dto.BookDetailRequestDTO;
import com.wedding.wedding_management_system.dto.BookDetailResponseDTO;
import com.wedding.wedding_management_system.dto.BookResponseDTO;
import com.wedding.wedding_management_system.dto.UpdateBookDetailsRequestDTO;
import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.entity.BookDetail;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.entity.Service;
import com.wedding.wedding_management_system.repository.BookDetailRepository;
import com.wedding.wedding_management_system.repository.BookRepository;
import com.wedding.wedding_management_system.repository.ServiceRepository;
import com.wedding.wedding_management_system.service.CustomerService;
import com.wedding.wedding_management_system.util.JwtToken;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerBookController {

    private final CustomerService customerService;
    private final BookRepository bookRepository;
    private final BookDetailRepository bookDetailRepository;
    private final ServiceRepository serviceRepository;

    // ── 工具：從 token 取得客戶，驗證失敗回 null ──
    private Customer getCustomerFromToken(String token) {
        if (token == null || !JwtToken.isValid(token))
            return null;
        String email = JwtToken.getEmail(token);
        return customerService.findByEmail(email);
    }

    private Customer getCurrentCustomer() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return customerService.findByEmail(email);
    }

    // ════════════════════════════════════════
    // GET /api/customer/book
    // 讀取客戶自己的預約資料（步驟一顯示用）
    // ════════════════════════════════════════
    @GetMapping("/book")
    public ResponseEntity<?> getMyBook(
            @CookieValue(value = "customerToken", required = false) String token) {

        Customer customer = getCustomerFromToken(token);
        if (customer == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入"));

        List<Book> books = bookRepository.findByCustomer(customer);
        if (books.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "找不到預約資料"));

        Book book = books.get(0);
        return ResponseEntity.ok(BookResponseDTO.from(book, customer));
    }

    // ════════════════════════════════════════
    // PATCH /api/customer/book
    // 修改婚宴基本資訊（步驟一修改用）
    // ════════════════════════════════════════
    @PatchMapping("/book")
    public ResponseEntity<?> updateMyBook(
            @CookieValue(value = "customerToken", required = false) String token,
            @RequestBody UpdateBookDetailsRequestDTO dto) {

        Customer customer = getCustomerFromToken(token);
        if (customer == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入"));

        List<Book> books = bookRepository.findByCustomer(customer);
        if (books.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "找不到預約資料"));


        Book book = books.get(0);
        if (dto.getWeddingDate() != null)
            book.setWeddingDate(dto.getWeddingDate());
        if (dto.getGuestScale() != null)
            book.setGuestScale(dto.getGuestScale());
        if (dto.getPlace() != null)
            book.setPlace(dto.getPlace());
        if (dto.getStyles() != null)
            book.setStyles(dto.getStyles());
        if (dto.getNotes() != null)
            book.setContent(dto.getNotes());

        bookRepository.save(book);
        log.info("客戶修改預約資料，customer_id={}, book_id={}", customer.getId(), book.getId());
        return ResponseEntity.ok(BookResponseDTO.from(book, customer));
    }

    // ════════════════════════════════════════
    // GET /api/customer/book/details
    // 讀取客戶自己的服務細項（步驟二載入用）
    // ════════════════════════════════════════
    @GetMapping("/book/details")
    public ResponseEntity<?> getMyBookDetails() {
        Customer customer = getCurrentCustomer();

        List<Book> books = bookRepository.findByCustomer(customer);
        if (books.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "找不到預約資料"));

        Book book = books.get(0);
        List<BookDetailResponseDTO> result = bookDetailRepository
                .findByBookIdOrderByServiceIdAsc(book.getId())
                .stream()
                .map(BookDetailResponseDTO::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ════════════════════════════════════════
    // POST /api/customer/book/details
    // 送出方案細節（步驟三確認送出用）
    // ════════════════════════════════════════
    @PostMapping("/book/details")
    public ResponseEntity<?> submitBookDetails(
            @CookieValue(value = "customerToken", required = false) String token,
            @RequestBody List<BookDetailRequestDTO> details) {

        Customer customer = getCustomerFromToken(token);
        if (customer == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入"));

        List<Book> books = bookRepository.findByCustomer(customer);
        if (books.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "找不到預約資料"));

        Book book = books.get(0);

        // 刪除舊的 book_details
        bookDetailRepository.deleteByBookId(book.getId());

        // 建立新的 book_details
        List<BookDetail> newDetails = details.stream().map(d -> {
            Service service = serviceRepository.findById(d.getServiceId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到服務項目，id=" + d.getServiceId()));
            BookDetail bd = new BookDetail();
            bd.setBook(book);
            bd.setService(service);
            bd.setUnitPrice(d.getUnitPrice());
            bd.setCeremonyDate(d.getCeremonyDate());
            return bd;
        }).collect(Collectors.toList());

        bookDetailRepository.saveAll(newDetails);
        log.info("客戶送出方案細節，customer_id={}, book_id={}, 共{}筆", customer.getId(), book.getId(), newDetails.size());
        return ResponseEntity.ok(Map.of("message", "方案已送出", "count", newDetails.size()));
    }
}