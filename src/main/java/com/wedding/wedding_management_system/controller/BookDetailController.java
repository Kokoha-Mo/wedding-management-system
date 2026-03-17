package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.dto.BookDetailRequestDTO;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookDetailController {

    private final BookRepository       bookRepository;
    private final BookDetailRepository bookDetailRepository;
    private final ServiceRepository    serviceRepository;   // ← 查 Service entity 用

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
    public ResponseEntity<List<BookDetail>> getDetails(@PathVariable Integer bookId) {
        return ResponseEntity.ok(bookDetailRepository.findByBookId(bookId));
    }
}