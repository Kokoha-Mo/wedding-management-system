package com.wedding.wedding_management_system.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectProgressDTO {
    
    private Integer projectId;
    private String projectNo;       // 專案編號 (用婚期+ID組裝)
    private String customerName;    // 來自 Book
    private String pmName;          // 來自 Book
    private String status;          // 來自 Project
    private LocalDate weddingDate;  // 來自 Book

    // 🌟 新增：付款資訊陣列
    private List<PaymentDTO> payments;

    // 🌟 新增：專案正式文件列表 (對應 documents 表)
    private List<DocumentDetail> documents;
    
    // 時間軸 / 溝通紀錄
    private List<CommunicationDetail> timeline;

    @Data
    public static class CommunicationDetail {
        private Integer id;             // 對應 ProjectCommunication 的 id
        private String createBy;        // 發送者 (對應 createBy)
        private String content;         // 內容 (對應 content)
        private LocalDateTime createAt; // 時間 (對應 createAt)
        
        // 附件列表
        private List<DocumentDetail> documents;
    }

    @Data
    public static class DocumentDetail {
        private Integer id;             // 對應 Document 的 id
        private String name;            // 檔名 (對應 name)
        private String fileType;        // 類型 (對應 fileType)
        private String filePath;        // 下載路徑 (對應 filePath)
    }

    // 🌟 新增：用來裝單筆付款資訊的類別
    @Data
    public static class PaymentDTO {
        private String title;   // 顯示文字 (例如：訂金 NT$ 19,000)
        private String status;  // 狀態 (PAID, PENDING, NONE)
        private String dueDate; // 期限 (例如：2026.09.24)
    }
}