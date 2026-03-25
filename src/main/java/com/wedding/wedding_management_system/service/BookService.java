package com.wedding.wedding_management_system.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
import com.wedding.wedding_management_system.repository.*;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final BookDetailRepository bookDetailRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository   employeeRepository;
    private final PasswordEncoder      passwordEncoder;
    private final ServiceRepository serviceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final CustomerLoginService customerLoginService;
    private final EmailService emailService;


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

        // 3. 都查無 → 建立新客戶（密碼先用隨機佔位，之後透過驗證信設定）
        log.info("查無客戶，建立新客戶");
        Customer c = new Customer();
        c.setName(ReName(dto.getName()));
        c.setTel(dto.getTel());
        c.setEmail(dto.getEmail());
        c.setLineId(dto.getLineId());
        // 密碼先以手機號碼佔位（僅為滿足 NOT NULL，客戶尚未能登入）
        // TODO【上線前修改】將下方改為：passwordEncoder.encode(UUID.randomUUID().toString())
        String rawPassword = dto.getTel() != null
                ? dto.getTel().replaceAll("[^0-9]", "")
                : "12345678";
        c.setPassword(passwordEncoder.encode(rawPassword));
        Customer saved = customerRepository.save(c);

        // 寄送帳號設定驗證信（僅在有 email 時才送）
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            try {
                String token = customerLoginService.generateAndSaveResetToken(dto.getEmail());
                emailService.sendResetPasswordEmail(dto.getEmail(), saved.getName(), token);
                log.info("已寄送帳號設定驗證信給新客戶 email={}", dto.getEmail());
            } catch (Exception e) {
                log.warn("驗證信寄送失敗，email={}, 原因={}", dto.getEmail(), e.getMessage());
            }
        }

        return saved;
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

        if (email != null && !email.isBlank()) {
            customerRepository.findByEmail(email).ifPresent(c -> {
                CustomerDTO dto = CustomerDTO.from(c);
                // 找該客戶的 book
                bookRepository.findByCustomer(c).stream()
                        .findFirst()
                        .ifPresent(b -> dto.setBookId(b.getId()));
                result.add(dto);
            });
        }

        if (tel != null && !tel.isBlank() && result.isEmpty()) {
            customerRepository.findFirstByTel(tel).ifPresent(c -> {
                CustomerDTO dto = CustomerDTO.from(c);
                bookRepository.findByCustomer(c).stream()
                        .findFirst()
                        .ifPresent(b -> dto.setBookId(b.getId()));
                result.add(dto);
            });
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

        // Step 3:🌟 分配 manager：自己建的or自動分配接案數最少的 "婚顧部 MANAGER"
        Employee manager;
        if (dto.getManagerId() != null) {
            // 直接指定該婚顧
            manager = employeeRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new RuntimeException("找不到指定的業務人員"));
            log.info("指定分配 manager_id={}", dto.getManagerId());
        } else {
            // 自動分配接案數最少的
            manager = employeeRepository.findManagerWithLeastBooks()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("目前沒有可分配的業務人員"));
            log.info("自動分配 manager_id={}", manager.getId());
        }

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

        // 自動帶入 service_id=1 和 service_id=2（A方案）
        List<Integer> aServiceIds = List.of(1, 2);
        aServiceIds.forEach(serviceId -> {
            serviceRepository.findById(serviceId).ifPresent(service -> {
                BookDetail defaultDetail = new BookDetail();
                defaultDetail.setBook(saved);
                defaultDetail.setService(service);
                defaultDetail.setUnitPrice(service.getPrice());
                bookDetailRepository.save(defaultDetail);
                log.info("自動帶入 A方案服務，service_id={}, book_id={}", serviceId, saved.getId());
            });
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
                "已簽約", bookRepository.countByManager_IdAndStatus(managerId, "已簽約")
        );
    }

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
    public BookResponseDTO updateStatus(Integer bookId, String newStatus, Integer managerId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到預約單，id=" + bookId));
        log.info("更新預約狀態 book_id={}: {} → {}", bookId, book.getStatus(), newStatus);

        // ── 轉簽約前檢查婚宴日期 ──
        if ("已簽約".equals(newStatus) && book.getWeddingDate() == null) {
            throw new IllegalStateException("請先填寫婚宴日期才能轉為簽約");
        }
        book.setStatus(newStatus);
        if (managerId != null) {
            // 🌟 先用 managerId 去資料庫把這個 Employee 實體撈出來
            Employee employee = employeeRepository.findById(managerId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到此員工，id=" + managerId));

            // 🌟 然後再把整個 Employee 物件塞給 Book
            book.setManager(employee);
        }
        Book saved = bookRepository.save(book);

        // ── 狀態改成已簽約時，自動建立 project ──
        if ("已簽約".equals(newStatus)) {
            // 檢查是否已有 project，避免重複建立
            boolean projectExists = projectRepository.findByBook_Id(bookId).isPresent();

            if (!projectExists) {
                // 計算 book_details 總金額
                List<BookDetail> details = bookDetailRepository.findByBookIdOrderByServiceIdAsc(bookId);
                int totalPayment = details.stream()
                        .mapToInt(d -> d.getUnitPrice() != null ? d.getUnitPrice() : 0)
                        .sum();

                Project project = new Project();
                project.setBook(saved);
                project.setTotalPayment(totalPayment);
                project.setPaymentStatus("訂金結清");
                project.setStatus("進行中");
                projectRepository.save(project);

                log.info("自動建立專案，book_id={}, total_payment={}", bookId, totalPayment);

                // ── 把 book_details 每個服務項目建立成 ProjectTask ──
                List<ProjectTask> tasks = details.stream()
                        .filter(detail -> detail.getService() != null)
                        .map(detail -> {
                    ProjectTask task = new ProjectTask();
                    Project savedProject= projectRepository.save(project);;
                    task.setProject(savedProject);
                    task.setService(detail.getService());
                    task.setStatus("待指派");
                    task.setUpdateAt(LocalDateTime.now());
                    return task;
                }).collect(Collectors.toList());

                projectTaskRepository.saveAll(tasks);
                log.info("自動建立任務，共{}筆", tasks.size());
            }
        }

        Customer customer = saved.getCustomer();
        return BookResponseDTO.from(saved, customer);
    }
}
