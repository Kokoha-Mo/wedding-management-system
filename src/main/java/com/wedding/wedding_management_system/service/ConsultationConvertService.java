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
}