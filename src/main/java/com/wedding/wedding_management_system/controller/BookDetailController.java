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

    @PutMapping("/{bookId}/details")
    public ResponseEntity<?> updateBookDetails(
            @PathVariable Integer bookId,
            @RequestBody UpdateBookDetailsRequestDTO request) {

        // TODO: 1. 更新資料庫中該張預約單 (bookId) 的「備註 (content/notes)」
        System.out.println("準備更新單號 " + bookId + " 的備註: " + request.getNotes());

        // TODO: 2. 更新資料庫中的「服務細項」
        // 業界標準作法：先「刪除」該 bookId 底下所有的舊細項，然後把 request.getDetails() 裡的「重新新增」進去
        System.out.println("收到新的細項數量: " + request.getDetails().size());

        // 回傳成功訊息給前端
        Map<String, String> response = new HashMap<>();
        response.put("message", "需求與方案已成功更新！");
        return ResponseEntity.ok(response);
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
        // 這裡去呼叫 Service / Repository 撈取資料
        // BookDetailsDTO details = bookService.getBookDetails(bookId);

        // 這裡先隨便塞個假資料測試看看前端會不會動：
        Map<String, Object> fakeData = new HashMap<>();
        fakeData.put("services", Arrays.asList("新秘造型師", "空拍機壯闊空景拍攝", "迎賓與拍照區主題設計"));
        fakeData.put("notes", "這是我後端傳過來的備註測試！");

        return ResponseEntity.ok(fakeData);
    }
}