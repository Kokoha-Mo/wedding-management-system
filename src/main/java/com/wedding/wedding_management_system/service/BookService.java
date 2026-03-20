package com.wedding.wedding_management_system.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.wedding.wedding_management_system.dto.BookResponseDTO;
import com.wedding.wedding_management_system.dto.UpdateBookDetailsRequestDTO;
import com.wedding.wedding_management_system.dto.CreateBookRequestDTO;
import com.wedding.wedding_management_system.dto.CustomerDTO;
import com.wedding.wedding_management_system.entity.*;
import com.wedding.wedding_management_system.repository.BookDetailRepository;
import com.wedding.wedding_management_system.repository.BookRepository;
import com.wedding.wedding_management_system.repository.CustomerRepository;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import com.wedding.wedding_management_system.repository.ConsultationRepository;
import com.wedding.wedding_management_system.repository.ServiceRepository;

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

    @Autowired private BookRepository bookRepository;
    @Autowired private BookDetailRepository bookDetailRepository;   // ← 新增
    @Autowired private CustomerRepository customerRepository;
    @Autowired private EmployeeRepository   employeeRepository;
    @Autowired private PasswordEncoder      passwordEncoder;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private ServiceRepository serviceRepository;

    // ════════════════════════════════════════════════════════
    // 找或建立顧客（不動）
    // ════════════════════════════════════════════════════════
    private Customer findOrCreateCustomer(CreateBookRequestDTO dto) {
        // 1. 先用 email 查（email 不為空時）
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            Optional<Customer> byEmail = customerRepository.findByEmail(dto.getEmail());
            if (byEmail.isPresent()) {
                log.info("以 email 找到既有客戶，email={}", dto.getEmail());

                Customer c = byEmail.get();
                c.setName(ReName(dto.getName()));  // 同步更新名字
                if (dto.getTel()    != null) c.setTel(dto.getTel());
                if (dto.getLineId() != null) c.setLineId(dto.getLineId());
                return customerRepository.save(c);
            }
        }

        // 2. 再用 tel 查
        if (dto.getTel() != null && !dto.getTel().isBlank()) {
            Optional<Customer> byTel = customerRepository.findFirstByTel(dto.getTel());
            if (byTel.isPresent()) {
                log.info("以 tel 找到既有客戶，tel={}", dto.getTel());
                return byTel.get();
            }
        }

        // 3. 都查無 → 建立新客戶，密碼預設為手機號碼
        log.info("查無客戶，建立新客戶");
        Customer c = new Customer();
        c.setName(ReName(dto.getName()));
        c.setTel(dto.getTel());
        c.setEmail(dto.getEmail());
        c.setLineId(dto.getLineId());
        // 預設密碼為手機號碼（去除非數字字元）
        String rawPassword = dto.getTel() != null
                ? dto.getTel().replaceAll("[^0-9]", "")
                : "12345678";
        c.setPassword(passwordEncoder.encode(rawPassword));
        log.info("新客戶建立，預設密碼為手機號碼");

        // 2. 這裡打上「強制修改密碼」的暗號！
        c.setResetToken("FORCE_RESET");
        return customerRepository.save(c);
    }

    private @NonNull String ReName(String raw) {
        if (raw == null || raw.isBlank()) return "";
        // 前端已處理成 "A & B" 格式，這裡只做頭尾空白清理
        return raw.trim();
    }

    // ════════════════════════════════════════════════════════
    // 建立預約主單 + 細項
    // ════════════════════════════════════════════════════════

    public List<CustomerDTO> findSimilarCustomers(String email, String tel) {
        List<CustomerDTO> result = new ArrayList<>();

        // 查 email
        if (email != null && !email.isBlank()) {
            customerRepository.findByEmail(email)
                    .ifPresent(c -> result.add(CustomerDTO.from(c)));
        }

        // 查 tel（避免重複加入）
        if (tel != null && !tel.isBlank() && result.isEmpty()) {
            customerRepository.findFirstByTel(tel)
                    .ifPresent(c -> result.add(CustomerDTO.from(c)));
        }

        return result;
    }
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

        // Step 3:🌟 修改：自動分配接案數最少的 "婚顧部 MANAGER"
        Employee manager = employeeRepository.findManagerWithLeastBooks()
                .stream()
                .findFirst()
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

        // 自動帶入 service_id=1（A方案｜婚宴全時統籌）
        serviceRepository.findById(1).ifPresent(service -> {
            BookDetail defaultDetail = new BookDetail();
            defaultDetail.setBook(saved);
            defaultDetail.setService(service);
            defaultDetail.setUnitPrice(service.getPrice());
            bookDetailRepository.save(defaultDetail);
            log.info("自動帶入 A方案，book_id={}", saved.getId());
        });
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

    // 依員工 ID + 狀態查詢（只看自己負責的）
    @Transactional(readOnly = true)
    public List<BookResponseDTO> findByManagerAndStatus(Integer managerId, String status) {
        return bookRepository.findByManager_IdAndStatus(managerId, status)
                .stream()
                .map(book -> BookResponseDTO.from(book, book.getCustomer()))
                .collect(Collectors.toList());
    }

    // 依員工 ID 查各狀態數量
    @Transactional(readOnly = true)
    public Map<String, Long> statusCountsByManager(Integer managerId) {
        return Map.of(
                "處理中", bookRepository.countByManager_IdAndStatus(managerId, "處理中"),
                "已簽約", bookRepository.countByManager_IdAndStatus(managerId, "已簽約"),
                "取消",   bookRepository.countByManager_IdAndStatus(managerId, "取消")
        );
    }

//    @Transactional(readOnly = true)
//    public Map<String, Long> statusCounts() {
//        return Map.of(
//                "處理中",  bookRepository.countByStatus("處理中"),
//                "已簽約",  bookRepository.countByStatus("已簽約"),
//                "取消預約", bookRepository.countByStatus("取消預約")
//        );
//    }

    @Transactional
    public BookResponseDTO updateBookInfo(Integer bookId, UpdateBookDetailsRequestDTO request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到預約單，id=" + bookId));

        // 更新 book 欄位
        if (request.getWeddingDate() != null) book.setWeddingDate(request.getWeddingDate());
        if (request.getGuestScale()  != null) book.setGuestScale(request.getGuestScale());
        if (request.getPlace()       != null) book.setPlace(request.getPlace());
        if (request.getStyles()      != null) book.setStyles(request.getStyles());

        // 同步更新 customer 基本資料
        Customer customer = book.getCustomer();
        if (request.getName()   != null && !request.getName().isBlank())
            customer.setName(ReName(request.getName()));
        if (request.getTel()    != null && !request.getTel().isBlank())
            customer.setTel(request.getTel());
        if (request.getEmail()  != null && !request.getEmail().isBlank())
            customer.setEmail(request.getEmail());
        if (request.getLineId() != null)
            customer.setLineId(request.getLineId());
        customerRepository.save(customer);

        Book saved = bookRepository.save(book);
        log.info("更新預約資料 book_id={}", bookId);
        return BookResponseDTO.from(saved, customer);
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
