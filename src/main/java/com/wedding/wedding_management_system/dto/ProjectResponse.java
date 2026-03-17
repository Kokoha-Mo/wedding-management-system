package com.wedding.wedding_management_system.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

public class ProjectResponse {

    // 1. 列表用
    @Data
    public static class ListDTO {

        private Integer projectId;
        private String projectNo;
        private String customerName;
        private String status;
        private LocalDate weddingDate;
        private Long daysRemaining;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime updateAt;

        private List<TaskSummaryDTO> tasks;

        @Data
        public static class TaskSummaryDTO {
            private String deptName;
            private String taskName;
            private String status;
        }
    }

    // 2. 結案紀錄 Modal 用
    @Data
    public static class RecordDTO {

        private String projectNo; // 專案編號
        private String customerName; // 客戶姓名
        private String pmName; // 負責 PM 姓名 (來自 Project -> Book -> Manager)
        private LocalDate weddingDate; // 實際婚期
        private Integer durationDays; // 專案歷時天數 (建立日與結案日的差值)
        private String paymentStatus; // 帳務狀態 (如：尾款已結清)

        // 巢狀清單：最終成果與歸檔文件
        private List<DocumentDTO> documents;

        // 巢狀清單：任務執行稽核軌跡
        private List<TaskHistoryDTO> taskHistories;

        @Data
        public static class DocumentDTO {

            private String fileName; // 檔案名稱
            private String fileType; // 檔案類型 (決定前端顯示 pdf 還是影片 icon)
            private String fileSize; // 檔案大小 (可選，若資料庫沒存可略)
            private String uploadInfo; // 上傳資訊 (例如 "2026-08-10 歸檔" 或 "攝影部上傳")
            private String downloadUrl; // 下載連結路徑
        }

        @Data
        public static class TaskHistoryDTO {

            private String taskName; // 任務名稱 (如：場地佈置撤場與點收)
            private String ownerInfo; // 負責人資訊 (如：李小華 (設計部))
            private String status; // 狀態 (任務完成、立案)
            private String time; // 發生時間 (2026-08-09 10:30)
            private boolean isCompleted; // 用來讓前端判斷要顯示綠色點點還是灰色點點
        }
    }

    // 3. 統計卡片用
    @Data
    public static class DashboardDTO {

        private Long activeProjectsCount;
        private Long endingThisMonthCount;
        private Long completedThisYearCount;
    }
}
