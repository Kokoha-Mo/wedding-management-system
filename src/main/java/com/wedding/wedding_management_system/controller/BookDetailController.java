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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
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

        // 1. 更新備註
        if (request.getNotes() != null) {
            book.setContent(request.getNotes());
            bookRepository.save(book);
        }

        // 2. 確保清單裡有 service_id=1（A方案，永遠必選）
        List<BookDetailRequestDTO> detailList = request.getDetails() != null
                ? new ArrayList<>(request.getDetails())
                : new ArrayList<>();

        boolean hasServiceOne = detailList.stream()
                .anyMatch(s -> s.getServiceId() != null && s.getServiceId() == 1);
        if (!hasServiceOne) {
            BookDetailRequestDTO aService = new BookDetailRequestDTO();
            aService.setServiceId(1);
            detailList.add(0, aService);
        }

        // 3. 刪除舊細項，重新寫入（價格從 services 表取得）
        bookDetailRepository.deleteByBookId(bookId);

        List<BookDetail> details = detailList.stream()
                .filter(s -> s.getServiceId() != null)
                .map(s -> {
                    Service service = serviceRepository.findById(s.getServiceId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "找不到服務項目，service_id=" + s.getServiceId()));
                    BookDetail d = new BookDetail();
                    d.setBook(book);
                    d.setService(service);
                    d.setUnitPrice(service.getPrice()); // 直接從 services 表取正確價格
                    d.setCeremonyDate(s.getCeremonyDate());
                    return d;
                })
                .collect(Collectors.toList());

        bookDetailRepository.saveAll(details);

        return ResponseEntity.ok(Map.of(
                "message", "需求與方案已成功更新！",
                "saved",   details.size()
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
                    d.setService(service);
                    d.setUnitPrice(service.getPrice());
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

        // 計算總價
        int total = details.stream()
                .mapToInt(d -> d.getUnitPrice() != null ? d.getUnitPrice() : 0)
                .sum();

        List<Map<String, Object>> services = details.stream()
                .map(d -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("serviceId",   d.getService().getId());
                    item.put("serviceName", d.getService().getName());
                    item.put("unitPrice",   d.getUnitPrice() != null ? d.getUnitPrice() : d.getService().getPrice());
                    item.put("ceremonyDate", d.getCeremonyDate() != null ? d.getCeremonyDate().toString() : null);
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("services",   services);
        result.put("notes",      book.getContent() != null ? book.getContent() : "");
        result.put("totalPrice", total);
        result.put("customerName", book.getCustomer() != null ? book.getCustomer().getName() : "");
        result.put("tel",          book.getCustomer() != null ? book.getCustomer().getTel()  : "");
        result.put("lineId",       book.getCustomer() != null ? book.getCustomer().getLineId(): "");
        result.put("weddingDate",  book.getWeddingDate()  != null ? book.getWeddingDate().toString() : "");
        result.put("guestScale",   book.getGuestScale()   != null ? book.getGuestScale() : 0);
        result.put("place",        book.getPlace()        != null ? book.getPlace()   : "");
        result.put("styles",       book.getStyles()       != null ? book.getStyles()  : "");

        return ResponseEntity.ok(result);
    }
}