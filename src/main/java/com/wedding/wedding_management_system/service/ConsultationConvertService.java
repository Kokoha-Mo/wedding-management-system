package com.wedding.wedding_management_system.service;

import com.wedding.wedding_management_system.dto.BookResponseDTO;
import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.entity.Consultation;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.repository.BookRepository;
import com.wedding.wedding_management_system.repository.ConsultationRepository;
import com.wedding.wedding_management_system.repository.CustomerRepository;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationConvertService {

    private final ConsultationRepository consultationRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerLoginService customerLoginService;
    private final EmailService emailService;

    @Transactional
    public BookResponseDTO convertFromConsultation(Integer consultationId, String partnerName, String newEmail,
            String newTel, Integer managerId) {

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new RuntimeException("找不到此諮詢單 ID: " + consultationId));

        if ("轉預約".equals(consultation.getStatus())) {
            throw new RuntimeException("此諮詢單已經轉換過了，請勿重複操作！");
        }

        // ==========================================
        // 🌟 1. 優先更新櫃檯人員修改的資料
        // ==========================================
        if (newEmail != null && !newEmail.trim().isEmpty()) {
            consultation.setEmail(newEmail.trim());
        }
        if (newTel != null && !newTel.trim().isEmpty()) {
            consultation.setTel(newTel.trim());
        }

        // ==========================================
        // 🌟 2. 升級版防護網：檢查信箱或電話是否已經存在
        // ==========================================

        // 檢查信箱
        if (consultation.getEmail() != null && !consultation.getEmail().trim().isEmpty()) {
            if (customerRepository.findByEmail(consultation.getEmail()).isPresent()) {
                // 加入 EMAIL_EXISTS 前綴，讓前端能精準抓錯
                throw new RuntimeException("EMAIL_EXISTS:此信箱 (" + consultation.getEmail() + ") 已有建檔紀錄，請修改後再試。");
            }
        }

        // 嚴格清洗與驗證電話號碼
        String cleanedTel = formatAndValidateTel(consultation.getTel());
        consultation.setTel(cleanedTel); // 將清洗乾淨的電話寫回實體

        // 檢查電話
        if (customerRepository.findFirstByTel(cleanedTel).isPresent()) {
            // 加入 TEL_EXISTS 前綴，讓前端能精準抓錯
            throw new RuntimeException("TEL_EXISTS:此電話 (" + cleanedTel + ") 已有建檔紀錄，請修改後再試。");
        }
        // ==========================================

        // 3. 建立新客戶 (Customer)
        Customer customer = new Customer();
        String fullName = consultation.getName();
        if (partnerName != null && !partnerName.trim().isEmpty()) {
            fullName = fullName + " & " + partnerName.trim();
        }

        customer.setName(fullName);
        customer.setEmail(consultation.getEmail());
        customer.setTel(cleanedTel); // 確保進資料庫的是乾淨的 10 碼
        customer.setLineId(consultation.getLineId());
        // 密碼先以手機號碼佔位（僅為滿足 NOT NULL，客戶尚未能登入）
        // TODO【上線前修改】將下方改為：passwordEncoder.encode(UUID.randomUUID().toString())
        customer.setPassword(passwordEncoder.encode(cleanedTel));

        customer = customerRepository.save(customer);

        // 寄送帳號設定驗證信
        if (consultation.getEmail() != null && !consultation.getEmail().isBlank()) {
            try {
                String token = customerLoginService.generateAndSaveResetToken(customer.getEmail());
                emailService.sendResetPasswordEmail(customer.getEmail(), customer.getName(), token);
                log.info("已寄送帳號設定驗證信給新客戶 email={}", customer.getEmail());
            } catch (Exception e) {
                log.warn("驗證信寄送失敗，email={}, 原因={}", customer.getEmail(), e.getMessage());
            }
        }

        // ==========================================
        // 🌟 4. 指派婚顧人員 (判斷是否有指定)
        // ==========================================
        Employee manager;
        if (managerId != null) {
            // 前端有指定婚顧，直接查出來
            manager = employeeRepository.findById(managerId)
                    .orElseThrow(() -> new RuntimeException("找不到指定的婚顧人員！"));
        } else {
            // 前端未選擇，使用原本的自動分配 (接案數最少)
            manager = employeeRepository.findManagerWithLeastBooks()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("目前沒有可分配的婚顧部業務人員"));
        }

        // 5. 建立新預約單 (Book)
        Book book = new Book();
        book.setCustomer(customer);
        book.setManager(manager);
        book.setWeddingDate(consultation.getWeddingDate());
        book.setStyles(consultation.getStyles());
        book.setStatus("處理中");
        book.setContent(consultation.getAdditionalNotes());
        book.setGuestScale(parseGuestScale(consultation.getGuestScale()));
        book = bookRepository.save(book);

        // 6. 更新諮詢單狀態 (這時候也會連同修改後的信箱電話一起存進資料庫)
        consultation.setStatus("轉預約");
        consultationRepository.save(consultation);

        // 🌟 修改這裡：回傳帶有完整資訊的 DTO
        return BookResponseDTO.from(book, customer);
    }

    // ==========================================
    // 專門處理電話號碼清洗與防呆 (加入 TEL_FORMAT 錯誤前綴)
    // ==========================================
    private String formatAndValidateTel(String rawTel) {
        if (rawTel == null || rawTel.trim().isEmpty()) {
            throw new RuntimeException("TEL_FORMAT:電話號碼不能為空，無法建立客戶檔案！");
        }

        String cleanedTel = rawTel.replaceAll("\\D", "");

        if (!cleanedTel.matches("^09\\d{8}$")) {
            throw new RuntimeException("TEL_FORMAT:電話號碼格式錯誤！請確認為 10 碼數字 (如: 0912345678)。");
        }

        return cleanedTel;
    }

    private Integer parseGuestScale(String scaleStr) {
        if (scaleStr == null || scaleStr.isBlank() || scaleStr.contains("尚未")) {
            return null; // 如果是尚未決定，直接回傳 null
        }
        try {
            // 利用正則表達式濾出數字
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
}