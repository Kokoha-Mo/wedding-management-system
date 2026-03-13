package com.wedding.wedding_management_system.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectProgressDTO {
    
    // --- 1. 專案基本資訊 ---
    private Integer projectId;
    private String projectName;     // 從 Project 拿 name
    private String status;          // 專案狀態 (如：執行中、已結案)
    private LocalDate startDate;
    private LocalDate endDate;

    // --- 2. 負責人資訊 (可選) ---
    private String managerName;     // 負責此專案的 PM 名字

    // --- 3. 時間軸 / 溝通紀錄 ---
    private List<CommunicationDetail> timeline;

    // --- 內部類別：單筆溝通紀錄 ---
    @Data
    public static class CommunicationDetail {
        private Integer commId;
        private String senderType;  // 發送者類型 (CUSTOMER 或 EMPLOYEE)
        private String message;     // 溝通內容
        private LocalDateTime createdAt;
        
        // --- 4. 該筆留言包含的附件 ---
        private List<DocumentDetail> documents;
    }

    // --- 內部類別：附件資訊 ---
    @Data
    public static class DocumentDetail {
        private Integer documentId;
        private String fileName;
        private String fileType;
        private String filePath;    // 提供給前端下載的 URL
    }
}