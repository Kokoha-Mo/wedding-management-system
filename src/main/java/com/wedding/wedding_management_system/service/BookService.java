package com.wedding.wedding_management_system.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.wedding.wedding_management_system.dto.BookDetailRequestDTO;
import com.wedding.wedding_management_system.dto.BookResponseDTO;
import com.wedding.wedding_management_system.dto.CreateBookRequestDTO;
import com.wedding.wedding_management_system.dto.CustomerDTO;
import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.entity.BookDetail;
import com.wedding.wedding_management_system.entity.Consultation;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.repository.BookDetailRepository;
import com.wedding.wedding_management_system.repository.BookRepository;
import com.wedding.wedding_management_system.repository.ConsultationRepository;
import com.wedding.wedding_management_system.repository.CustomerRepository;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    @Autowired private BookRepository       bookRepository;
    @Autowired private BookDetailRepository bookDetailRepository;   // ← 新增
    @Autowired private CustomerRepository   customerRepository;
    @Autowired private EmployeeRepository   employeeRepository;
    @Autowired private PasswordEncoder      passwordEncoder;
    @Autowired private ConsultationRepository consultationRepository;

    private static final String TEMP_PASSWORD = "Wedding@2026";

    // ════════════════════════════════════════════════════════
    // 找或建立顧客（不動）
    // ════════════════════════════════════════════════════════
    private Customer findOrCreateCustomer(CreateBookRequestDTO dto) {
        log.info("嘗試找客戶，email={}", dto.getEmail());
        return customerRepository.findByEmail(dto.getEmail())
                .orElseGet(() -> {
                    log.info("查無客戶，建立新客戶");
                    Customer c = new Customer();
                    c.setName(ReName(dto.getName()));
                    c.setTel(dto.getTel());
                    c.setEmail(dto.getEmail());
                    c.setLineId(dto.getLineId());
                    c.setPassword(passwordEncoder.encode(TEMP_PASSWORD));
                    return customerRepository.save(c);
                });
    }

    private @NonNull String ReName(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return raw.trim();
    }

    // ════════════════════════════════════════════════════════
    // 建立預約主單 + 細項
    // ════════════════════════════════════════════════════════
    @Transactional
    public BookResponseDTO createBook(CreateBookRequestDTO dto) {

        // Step 1: 找或建立 customer
        Customer customer = findOrCreateCustomer(dto);

        // Step 2: 刪除該客戶舊有的 book（含細項）
        List<Book> existingBooks = bookRepository.findByCustomer(customer);
        if (!existingBooks.isEmpty()) {
            existingBooks.forEach(b -> bookDetailRepository.deleteByBookId(b.getId())); // ← 先刪細項
            bookRepository.deleteAll(existingBooks);
            log.info("刪除客戶舊有預約，customer_id={}, 共{}筆", customer.getId(), existingBooks.size());
        }

        // Step 3: 自動分配接案數最少的 manager
        Employee manager = employeeRepository.findEmployeeWithLeastBooks()
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("目前沒有可分配的業務人員"));

        // Step 4: 建立新 book
        Book book = new Book();
        book.setCustomer(customer);
        book.setManager(manager);
        book.setWeddingDate(dto.getWeddingDate());
        book.setGuestScale(dto.getGuestScale());
        book.setStyles(dto.getStyles());
        book.setPlace(dto.getPlace());
        book.setContent(dto.getContent());
        book.setStatus("處理中");
        Book saved = bookRepository.save(book);
        log.info("預約建立成功，book_id={}", saved.getId());

        return BookResponseDTO.from(saved, customer);
    }

    // ════════════════════════════════════════════════════════
    // 從諮詢單轉預約（不動）
    // ════════════════════════════════════════════════════════
    @Transactional
    public BookResponseDTO convertFromConsultation(Integer consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new EntityNotFoundException("找不到諮詢單，id=" + consultationId));

        CreateBookRequestDTO dto = new CreateBookRequestDTO();
        dto.setName(consultation.getName());
        dto.setTel(consultation.getTel());
        dto.setEmail(consultation.getEmail());
        dto.setLineId(consultation.getLineId());
        dto.setWeddingDate(consultation.getWeddingDate());
        dto.setStyles(consultation.getStyles());
        dto.setContent(consultation.getAdditionalNotes());
        // 諮詢單轉預約不帶 services，book_details 留空，後續由業務補填

        BookResponseDTO result = createBook(dto);
        consultation.setStatus("轉預約");
        consultationRepository.save(consultation);
        return result;
    }

    // ════════════════════════════════════════════════════════
    // 其餘方法不動
    // ════════════════════════════════════════════════════════
    public List<CustomerDTO> findSimilarCustomers(String email) {
        List<CustomerDTO> result = new ArrayList<>();
        if (email != null && !email.isBlank()) {
            Optional<Customer> found = customerRepository.findByEmail(email);
            found.ifPresent(c -> result.add(CustomerDTO.from(c)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<BookResponseDTO> findByStatus(String status) {
        return bookRepository.findByStatus(status)
                .stream()
                .map(book -> BookResponseDTO.from(book, book.getCustomer()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> statusCounts() {
        return Map.of(
                "處理中",  bookRepository.countByStatus("處理中"),
                "已簽約",  bookRepository.countByStatus("已簽約"),
                "取消預約", bookRepository.countByStatus("取消預約")
        );
    }

    @Transactional
    public BookResponseDTO updateStatus(Integer bookId, String newStatus) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到預約單，id=" + bookId));
        log.info("更新預約狀態 book_id={}: {} → {}", bookId, book.getStatus(), newStatus);
        book.setStatus(newStatus);
        Book saved = bookRepository.save(book);
        return BookResponseDTO.from(saved, saved.getCustomer());
    }
}