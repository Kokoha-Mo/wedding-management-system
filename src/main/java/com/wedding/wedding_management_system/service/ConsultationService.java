package com.wedding.wedding_management_system.service;

import com.wedding.wedding_management_system.dto.ConsultationRequestDTO;
import com.wedding.wedding_management_system.entity.Consultation;
import com.wedding.wedding_management_system.repository.ConsultationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // 最佳實踐：使用 Lombok 自動生成包含 final 變數的建構子，取代 @Autowired
public class ConsultationService {

    // 注入剛才準備好的 Repository
    private final ConsultationRepository consultationRepository;

    /**
     * 處理新增諮詢單的商業邏輯
     */
    @Transactional // 確保資料庫操作的完整性，發生錯誤時會自動 Rollback
    public Consultation createConsultation(ConsultationRequestDTO dto) {

        Consultation entity = new Consultation();

        // 1. 基本資料映射 (從 DTO 搬到 Entity)
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setLineId(dto.getLineId());

        entity.setConsultationDate(dto.getConsultationDate());
        entity.setPreferredTime(dto.getPreferredTime());
        entity.setWeddingDate(dto.getWeddingDate());
        entity.setGuestScale(dto.getGuestScale());
        entity.setAdditionalNotes(dto.getAdditionalNotes());

        // 2. 核心邏輯：處理陣列轉字串
        // 前端傳來的是 ["森林系", "極簡現代"]，資料庫要存 "森林系,極簡現代"
        if (dto.getStyles() != null && !dto.getStyles().isEmpty()) {
            entity.setStyles(String.join(",", dto.getStyles()));
        }

        if (dto.getServices() != null && !dto.getServices().isEmpty()) {
            entity.setServices(String.join(",", dto.getServices()));
        }

        // 3. 狀態初始化
        // 雖然 MySQL 有設定 Default，但在 Java 程式碼中明確寫出來會讓邏輯更清晰
        entity.setStatus("待處理");

        // 4. 儲存進資料庫並回傳儲存後的結果 (會包含自動生成的 consultation_id)
        return consultationRepository.save(entity);
    }

    /**
     * 獲取所有諮詢單 (依建立時間由新到舊排序)
     */
    public List<Consultation> getAllConsultations() {
        return consultationRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 更新諮詢單的處理狀態
     * @param id 諮詢單ID
     * @param newStatus 新的狀態 (待處理/轉預約/無效單)
     */
    @Transactional
    public Consultation updateStatus(Integer id, String newStatus) {
        // 1. 使用 JPA 內建的 findById 尋找資料。
        // 最佳實踐：搭配 orElseThrow，如果找不到直接拋出例外，讓 Controller 捕捉。
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到 ID 為 " + id + " 的諮詢單"));
        
        // 2. 更新實體的狀態
        consultation.setStatus(newStatus);
        
        // 3. 儲存並回傳
        // 小知識：因為有加上 @Transactional，其實只要 set 完，
        // Spring 發現資料有更動就會自動幫你 update 到資料庫，但寫上 save() 會讓程式碼意圖更直覺！
        return consultationRepository.save(consultation);
    }
}