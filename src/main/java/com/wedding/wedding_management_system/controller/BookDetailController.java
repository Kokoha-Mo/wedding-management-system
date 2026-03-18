package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.dto.BookDetailRequestDTO;
import com.wedding.wedding_management_system.dto.UpdateBookDetailsRequestDTO;
import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.entity.BookDetail;
import com.wedding.wedding_management_system.entity.Service;
import com.wedding.wedding_management_system.repository.BookDetailRepository;
import com.wedding.wedding_management_system.repository.BookRepository;
import com.wedding.wedding_management_system.repository.ServiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employee/books")
@RequiredArgsConstructor
public class BookDetailController {

    private final BookRepository       bookRepository;
    private final BookDetailRepository bookDetailRepository;
    private final ServiceRepository    serviceRepository;

    /**
     * GET /api/books/my?customerId={id}
     * 顧客登入後查自己的 book_id
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyBook(@RequestParam Integer customerId) {
        List<Book> books = bookRepository.findByCustomerId(customerId);
        if (books.isEmpty()) {
            return ResponseEntity.ok(null);
        }
        Book book = books.get(0);
        return ResponseEntity.ok(Map.of(
                "bookId",      book.getId(),
                "status",      book.getStatus(),
                "weddingDate", book.getWeddingDate() != null ? book.getWeddingDate().toString() : "",
                "guestScale",  book.getGuestScale()  != null ? book.getGuestScale()  : 0,
                "place",       book.getPlace()        != null ? book.getPlace()        : "",
                "styles",      book.getStyles()       != null ? book.getStyles()       : "",
                "content",     book.getContent()      != null ? book.getContent()      : ""
        ));
    }

    @PutMapping("/{bookId}/details")
    public ResponseEntity<?> updateBookDetails(
            @PathVariable Integer bookId,
            @RequestBody UpdateBookDetailsRequestDTO request) {

        // 1. 找到 book
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到預約單，id=" + bookId));

        // 2. 更新備註（存進 book.content）
        if (request.getNotes() != null) {
            book.setContent(request.getNotes());
            bookRepository.save(book);
        }

        // 3. 刪除舊細項，重新寫入新細項
        bookDetailRepository.deleteByBookId(bookId);

        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            List<BookDetail> details = request.getDetails().stream()
                    .filter(s -> s.getServiceId() != null)
                    .map(s -> {
                        Service service = serviceRepository.findById(s.getServiceId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "找不到服務項目，service_id=" + s.getServiceId()));
                        BookDetail d = new BookDetail();
                        d.setBook(book);
                        d.setService(service);
                        d.setUnitPrice(s.getUnitPrice() != null ? s.getUnitPrice() : service.getPrice());
                        d.setCeremonyDate(s.getCeremonyDate());
                        return d;
                    })
                    .collect(Collectors.toList());
            bookDetailRepository.saveAll(details);
        }

        return ResponseEntity.ok(Map.of(
                "message", "需求與方案已成功更新！",
                "saved", request.getDetails() != null ? request.getDetails().size() : 0
        ));
    }

    /**
     * POST /api/books/{bookId}/details
     * 顧客端送出服務細項（覆蓋舊的）
     * Body: [ { "service_id": 1, "unit_price": 43800 }, ... ]
     */
    @PostMapping("/{bookId}/details")
    public ResponseEntity<?> saveDetails(
            @PathVariable Integer bookId,
            @RequestBody List<BookDetailRequestDTO> services) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到預約單，id=" + bookId));

        // 覆蓋：先刪舊細項
        bookDetailRepository.deleteByBookId(bookId);

        // 寫入新細項（透過 ServiceRepository 查出 Service entity）
        List<BookDetail> details = services.stream()
                .filter(s -> s.getServiceId() != null)
                .map(s -> {
                    Service service = serviceRepository.findById(s.getServiceId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "找不到服務項目，service_id=" + s.getServiceId()));

                    BookDetail d = new BookDetail();
                    d.setBook(book);
                    d.setService(service);          // ← 設定 Service entity
                    d.setUnitPrice(s.getUnitPrice());
                    d.setCeremonyDate(s.getCeremonyDate());
                    return d;
                })
                .collect(Collectors.toList());

        bookDetailRepository.saveAll(details);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "saved",   details.size()
        ));
    }

    /**
     * GET /api/books/{bookId}/details
     * 查詢某 book 的所有細項
     */
    @GetMapping("/{bookId}/details")
    public ResponseEntity<?> getBookDetails(@PathVariable Integer bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到預約單，id=" + bookId));

        List<BookDetail> details = bookDetailRepository.findByBookIdOrderByServiceIdAsc(bookId);

        // 回傳已選服務的 service_id 清單（前端用來勾選 checkbox）
        List<Map<String, Object>> services = details.stream()
                .map(d -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("serviceId",   d.getService().getId());
                    item.put("serviceName", d.getService().getName());
                    item.put("unitPrice",   d.getUnitPrice());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("services", services);
        result.put("notes",    book.getContent() != null ? book.getContent() : "");

        return ResponseEntity.ok(result);
    }
}