package com.wedding.wedding_management_system.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    
    // 🌟 新增：整體進度百分比 (讓圓環動畫動起來)
    private Integer progressPercent; 

    @Data
    public static class CommunicationDetail {
        private Integer id;             // 對應 ProjectCommunication 的 id
        private String createBy;        // 發送者 (對應 createBy)
        private String content;         // 內容 (對應 content)
        // 🌟 核心修改：加上 JsonFormat 標註，強制轉換為台北時區並指定格式
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Taipei")
        private LocalDateTime createAt; // 時間 (對應 createAt)
        
        // 附件列表
        private List<DocumentDetail> documents;
    }

    // 🌟 新增：用來裝單筆付款資訊的類別
    @Data
    public static class PaymentDTO {
        private String title;   // 顯示文字 (例如：訂金 NT$ 19,000)
        private String status;  // 狀態 (PAID, PENDING, NONE)
        private String dueDate; // 期限 (例如：2026.09.24)
    }

    // 🌟 新增：用來裝專案正式文件資訊的類別
    @Data
    public static class DocumentDetail {
        private Integer id;             // 對應 Document 的 id
        private String name;            // 檔名 (對應 name)
        private String fileType;        // 類型 (對應 fileType)
        private String filePath;        // 下載路徑 (對應 filePath)
    }

    // ==========================================
    // 🌟 新增區塊：手風琴時間軸的 DTO 結構
    // ==========================================
    private List<PhaseDTO> phases;

    @Data
    public static class PhaseDTO {
        private Integer id;
        private String title;
        private String status;        // "completed", "active", "pending"
        private String pmMessage;     // 顧問更新的留言
        private String pmUpdateTime;  // 顧問更新的時間
        private List<CategoryDTO> categories; // 用於 Phase 2 (含分類)
        private List<TaskItemDTO> tasks;      // 用於 Phase 1, 3 (無分類的單純列表)
    }

    @Data
    public static class CategoryDTO {
        private String name;
        private List<TaskItemDTO> tasks;
    }

    @Data
public static class TaskItemDTO {
    private Integer id;
    private String name;
    
    // 強制規定 JSON 的 Key 必須叫做 isDone
    @JsonProperty("isDone")
    private boolean isDone; 
}

}