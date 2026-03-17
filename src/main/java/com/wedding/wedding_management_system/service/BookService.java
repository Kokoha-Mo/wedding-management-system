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
    private PasswordEncoder passwordEncoder; // 🌟 用來加密初始密碼

    @Autowired
    private ConsultationRepository consultationRepository;

    // 臨時密碼固定值，客戶登入後必須重設
    private static final String TEMP_PASSWORD = "Wedding@2026";

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
                    c.setPassword(passwordEncoder.encode(TEMP_PASSWORD));
                    // c.setPasswordResetRequired(true);
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

    // 🌟 聰明解析器：把字串 "100-150" 轉成數字 100，"尚未決定" 轉成 null
    private Integer parseGuestScale(String scaleStr) {
        if (scaleStr == null || scaleStr.isBlank() || scaleStr.contains("尚未")) {
            return null; // 如果是尚未決定，直接回傳 null
        }
        try {
            // 利用正則表達式濾出數字，例如 "100-150人" 會被切成 ["100", "150"]
            String[] nums = scaleStr.split("\\D+");
            for (String num : nums) {
                if (!num.isEmpty()) {
                    return Integer.parseInt(num); // 抓取第一個出現的數字存入
                }
            }
        } catch (Exception e) {
            log.warn("無法解析 guestScale: {}", scaleStr);
        }
        return null;
    }

    @Transactional
    public BookResponseDTO convertFromConsultation(Integer consultationId, String partnerName) {

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new RuntimeException("找不到此諮詢單 ID: " + consultationId));

        if ("轉預約".equals(consultation.getStatus())) {
            throw new RuntimeException("此諮詢單已經轉換過了，請勿重複操作！");
        }

        // ==========================================
        // 🌟 升級版防護網：檢查信箱或電話是否已經存在
        // ==========================================
        // 1. 檢查信箱
        if (consultation.getEmail() != null && !consultation.getEmail().trim().isEmpty()) {
            if (customerRepository.findByEmail(consultation.getEmail()).isPresent()) {
                throw new RuntimeException("資料衝突！此信箱 (" + consultation.getEmail() + ") 已有建檔紀錄，請確認是否為舊客戶。");
            }
        }

        // 2. 檢查電話 (🌟 這裡改成 findFirstByTel)
        if (consultation.getTel() != null && !consultation.getTel().trim().isEmpty()) {
            if (customerRepository.findFirstByTel(consultation.getTel()).isPresent()) {
                throw new RuntimeException("資料衝突！此電話 (" + consultation.getTel() + ") 已有建檔紀錄，請確認是否為舊客戶。");
            }
        }
        // ==========================================

        // 1. 建立新客戶 (Customer)
        Customer customer = new Customer();
        String fullName = consultation.getName();
        if (partnerName != null && !partnerName.trim().isEmpty()) {
            fullName = fullName + " & " + partnerName.trim();
        }

        customer.setName(fullName);
        customer.setEmail(consultation.getEmail());
        customer.setTel(consultation.getTel());
        customer.setLineId(consultation.getLineId());

        String defaultPassword = consultation.getTel() != null ? consultation.getTel() : "12345678";
        customer.setPassword(passwordEncoder.encode(defaultPassword));
        customer = customerRepository.save(customer);

        // 🌟 2. 自動分配接案數最少的婚顧部 MANAGER
        Employee manager = employeeRepository.findManagerWithLeastBooks()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("目前沒有可分配的婚顧部業務人員"));

        // 3. 建立新預約單 (Book)
        Book book = new Book();
        book.setCustomer(customer);
        book.setManager(manager); // 🌟 綁定負責業務
        book.setWeddingDate(consultation.getWeddingDate());
        book.setStyles(consultation.getStyles());
        book.setStatus("處理中");

        // 🌟 綁定客戶的補充說明到預約單的 Content
        book.setContent(consultation.getAdditionalNotes());

        // 🌟 使用我們寫好的聰明解析器來處理賓客數
        book.setGuestScale(parseGuestScale(consultation.getGuestScale()));

        book = bookRepository.save(book);

        // 4. 更新諮詢單狀態
        consultation.setStatus("轉預約");
        consultationRepository.save(consultation);

        return new BookResponseDTO();
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

        // 🌟 修改：自動分配接案數最少的 "婚顧部 MANAGER"
        Employee manager = employeeRepository.findManagerWithLeastBooks()
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
