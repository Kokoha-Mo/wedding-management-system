package com.wedding.wedding_management_system.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.wedding.wedding_management_system.dto.BookResponseDTO;
import com.wedding.wedding_management_system.dto.CreateBookRequestDTO;
import com.wedding.wedding_management_system.dto.CustomerDTO;
import com.wedding.wedding_management_system.entity.Consultation;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.repository.ConsultationRepository;
import com.wedding.wedding_management_system.repository.CustomerRepository;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.repository.BookRepository;

@Slf4j // log
@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ConsultationRepository consultationRepository;

    // 臨時密碼固定值，客戶登入後必須重設
    private static final String TEMP_PASSWORD = "Wedding@2026";

    @Autowired
    private CustomerLoginService customerLoginService;

    @Autowired
    private EmailService emailService;

    private Customer findOrCreateCustomer(CreateBookRequestDTO dto) {
        log.info("嘗試找客戶，email={}", dto.getEmail());
        return customerRepository.findByEmail(dto.getEmail())
                .orElseGet(() -> {
                    log.info("查無客戶，建立新客戶");
                    Customer c = new Customer();
                    c.setName(ReName(dto.getName())); // 直接清理 name 欄位
                    c.setTel(dto.getTel());
                    c.setEmail(dto.getEmail());
                    c.setLineId(dto.getLineId());
                    // c.setPassword(passwordEncoder.encode(TEMP_PASSWORD));
                    // c.setPasswordResetRequired(true);
                    // 先存檔取得有 ID 的 Customer
                    Customer savedCustomer = customerRepository.save(c);

                    // 寄送「設定密碼信件」的邏輯
                    try {
                        String token = customerLoginService.generateAndSaveResetToken(savedCustomer.getEmail());
                        emailService.sendResetPasswordEmail(savedCustomer.getEmail(), savedCustomer.getName(), token);
                        log.info("已成功寄送帳號設定信給新客戶: {}", savedCustomer.getEmail());
                    } catch (Exception e) {
                        log.error("寄送帳號設定信失敗，email={}, 錯誤: {}", savedCustomer.getEmail(), e.getMessage());
                    }

                    return customerRepository.save(c);
                });
    }

    private @NonNull String ReName(String raw) {
        if (raw == null || raw.isBlank())
            return "";

        // 把所有常見分隔符號（含多個空白）統一換成 & 分隔
        String cleaned = raw.trim()
                .replaceAll("[&＆/／、,，]+", "&") // 標點換成 &
                .replaceAll("\\s+", "&") // 空白也換成 &
                .replaceAll("&+", " & "); // 多個連續 & 合併，並加空格

        // 去掉頭尾可能殘留的 & 或空白
        return cleaned.replaceAll("^[\\s&]+|[\\s&]+$", "");
    }

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

        // 建立 customer + book
        BookResponseDTO result = createBook(dto);

        // 更新諮詢單狀態為「轉預約」
        consultation.setStatus("轉預約");
        consultationRepository.save(consultation);

        return result;
    }

    // public List<Book> getBooksByCustomerId(int customerId) {
    // return bookRepository.findByCustomer_Id(customerId);
    // }
    //
    // public List<Book> getAllBooks() {
    // return bookRepository.findAll();
    // }
    //
    // public List<Book> getBooksByCancel() {
    // return bookRepository.findByStatus(toString());
    // }

    public List<CustomerDTO> findSimilarCustomers(String email) {
        List<CustomerDTO> result = new ArrayList<>();

        if (email != null && !email.isBlank()) {
            Optional<Customer> found = customerRepository.findByEmail(email);
            if (found.isPresent()) {
                result.add(CustomerDTO.from(found.get()));
            }
        }
        return result;
    }

    @Transactional
    public BookResponseDTO createBook(CreateBookRequestDTO dto) {

        // ── Step 1: 找或建立 customer ──────────────────────────
        Customer customer = findOrCreateCustomer(dto);

        // ── Step 2: 刪除該客戶舊有的 book ─────────────────────
        List<Book> existingBooks = bookRepository.findByCustomer(customer);
        if (!existingBooks.isEmpty()) {
            bookRepository.deleteAll(existingBooks);
            log.info("刪除客戶舊有預約，customer_id={}, 共{}筆", customer.getId(), existingBooks.size());
        }

        // 自動分配接案數最少的 manager
        Employee manager = employeeRepository.findEmployeeWithLeastBooks()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("目前沒有可分配的業務人員"));

        // ── Step 3: 建立新 book ────────────────────────────────
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

    /**
     * 依狀態查詢列表（對應前端三個 tab）
     */
    @Transactional(readOnly = true)
    public List<BookResponseDTO> findByStatus(String status) {
        return bookRepository.findByStatus(status)
                .stream()
                .map(book -> BookResponseDTO.from(book, book.getCustomer()))
                .collect(Collectors.toList());
    }

    /**
     * 各狀態數量（供 tab badge 顯示）
     */
    @Transactional(readOnly = true)
    public Map<String, Long> statusCounts() {
        return Map.of(
                "處理中", bookRepository.countByStatus("處理中"),
                "已簽約", bookRepository.countByStatus("已簽約"),
                "取消預約", bookRepository.countByStatus("取消預約"));
    }

    /**
     * 更新預約狀態
     */
    @Transactional
    public BookResponseDTO updateStatus(Integer bookId, String newStatus) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到預約單，id=" + bookId));
        log.info("更新預約狀態 book_id={}: {} → {}", bookId, book.getStatus(), newStatus);
        book.setStatus(newStatus);
        Book saved = bookRepository.save(book);
        Customer customer = saved.getCustomer();
        return BookResponseDTO.from(saved, customer);
    }
}
