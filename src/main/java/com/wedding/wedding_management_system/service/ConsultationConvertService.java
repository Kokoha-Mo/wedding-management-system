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

        // 🌟 2. 嚴格清洗與驗證電話號碼
        String cleanedTel = formatAndValidateTel(consultation.getTel());

        // 3. 檢查電話 (使用清洗後的乾淨電話號碼去查)
        if (customerRepository.findFirstByTel(cleanedTel).isPresent()) {
            throw new RuntimeException("資料衝突！此電話 (" + cleanedTel + ") 已有建檔紀錄，請確認是否為舊客戶。");
        }
        // ==========================================

        // 4. 建立新客戶 (Customer)
        Customer customer = new Customer();
        String fullName = consultation.getName();
        if (partnerName != null && !partnerName.trim().isEmpty()) {
            fullName = fullName + " & " + partnerName.trim();
        }

        customer.setName(fullName);
        customer.setEmail(consultation.getEmail());
        customer.setTel(cleanedTel); // 🌟 確保進資料庫的是絕對乾淨的 10 碼數字
        customer.setLineId(consultation.getLineId());

        // 🌟 密碼直接使用乾淨的電話號碼
        customer.setPassword(passwordEncoder.encode(cleanedTel));
        customer = customerRepository.save(customer);

        // 5. 自動分配接案數最少的婚顧部 MANAGER
        Employee manager = employeeRepository.findManagerWithLeastBooks()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("目前沒有可分配的婚顧部業務人員"));

        // 6. 建立新預約單 (Book)
        Book book = new Book();
        book.setCustomer(customer);
        book.setManager(manager); // 綁定負責業務
        book.setWeddingDate(consultation.getWeddingDate());
        book.setStyles(consultation.getStyles());
        book.setStatus("處理中");

        // 綁定客戶的補充說明到預約單的 Content
        book.setContent(consultation.getAdditionalNotes());

        // 使用我們寫好的聰明解析器來處理賓客數
        book.setGuestScale(parseGuestScale(consultation.getGuestScale()));

        book = bookRepository.save(book);

        // 7. 更新諮詢單狀態
        consultation.setStatus("轉預約");
        consultationRepository.save(consultation);

        return new BookResponseDTO();
    }

    // ==========================================
    // 🌟 新增的私有方法：專門處理電話號碼清洗與防呆
    // ==========================================
    private String formatAndValidateTel(String rawTel) {
        if (rawTel == null || rawTel.trim().isEmpty()) {
            throw new RuntimeException("電話號碼不能為空，無法建立客戶檔案！");
        }

        // 利用正則表達式，把所有「非數字」的字元（如 -、空白、括號）全部拔除
        String cleanedTel = rawTel.replaceAll("\\D", "");

        // 驗證是否為 10 碼，且開頭必須是 09
        if (!cleanedTel.matches("^09\\d{8}$")) {
            throw new RuntimeException("電話號碼格式錯誤！請確認客戶留下的號碼為 10 碼數字 (例如: 0912345678)。當前解析結果: " + cleanedTel);
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