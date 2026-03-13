package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wedding.wedding_management_system.entity.Project;
import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.repository.ProjectRepository;
import com.wedding.wedding_management_system.dto.ProjectResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    /**
     * 1. 取得專案列表 (對應前端的 Table)
     */
    public List<ProjectResponse.ListDTO> getAllProjectLists() {
        List<Project> projects = projectRepository.findAll();

        return projects.stream().map(project -> {
            ProjectResponse.ListDTO dto = new ProjectResponse.ListDTO();
            dto.setProjectId(project.getId());
            dto.setStatus(project.getStatus());

            // 處理關聯資料 (加上 Null 防呆)
            Book book = project.getBook();
            if (book != null) {
                dto.setWeddingDate(book.getWeddingDate());

                // 計算剩餘天數
                if (book.getWeddingDate() != null) {
                    long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), book.getWeddingDate());
                    dto.setDaysRemaining(daysRemaining);

                    // 產生假想的專案編號 (例如: #WED-20261015-1)
                    // 如果你未來資料表有獨立的 project_no 欄位，直接 get 即可
                    String dateStr = book.getWeddingDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    dto.setProjectNo("#WED-" + dateStr + "-" + project.getId());
                }

                if (book.getCustomer() != null) {
                    dto.setCustomerName(book.getCustomer().getName());
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 2. 取得統計數據 (對應前端上方三個數字卡片)
     */
    public ProjectResponse.DashboardDTO getDashboardStats() {
        ProjectResponse.DashboardDTO dashboard = new ProjectResponse.DashboardDTO();

        // 1. 執行中專案數量 (直接算狀態)
        dashboard.setActiveProjectsCount(projectRepository.countByStatus("進行中"));

        // --- 準備時間範圍基準 ---
        LocalDate today = LocalDate.now();

        // 取得本月的第一天與最後一天
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = YearMonth.from(today).atEndOfMonth();

        // 取得本年度的第一天與最後一天 (因為 updateAt 是 LocalDateTime，所以要加上時間)
        LocalDateTime firstDayOfYear = LocalDateTime.of(today.getYear(), 1, 1, 0, 0);
        LocalDateTime lastDayOfYear = LocalDateTime.of(today.getYear(), 12, 31, 23, 59, 59);

        // 2. 本月即將結案
        // 條件：狀態為「進行中」，且婚期 (weddingDate) 落在本月
        Long endingThisMonth = projectRepository.countByStatusAndBook_WeddingDateBetween(
                "進行中",
                firstDayOfMonth,
                lastDayOfMonth);
        dashboard.setEndingThisMonthCount(endingThisMonth);

        // 3. 本年度已完成
        // 條件：狀態為「已結案」，且結案(最後更新)時間落在今年內
        Long completedThisYear = projectRepository.countByStatusAndUpdateAtBetween(
                "已結案",
                firstDayOfYear,
                lastDayOfYear);
        dashboard.setCompletedThisYearCount(completedThisYear);

        return dashboard;
    }

    /**
     * 3. 取得單一專案結案紀錄 (對應前端的 Modal)
     */
    public ProjectResponse.RecordDTO getProjectRecord(Integer projectId) {
        // 透過 ID 尋找，找不到就拋出例外 (稍後可以配合全域例外處理)
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("找不到該專案 ID: " + projectId));

        ProjectResponse.RecordDTO dto = new ProjectResponse.RecordDTO();

        // 基本資料對應...
        Book book = project.getBook();
        if (book != null) {
            String dateStr = book.getWeddingDate() != null
                    ? book.getWeddingDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    : "Unknown";
            dto.setProjectNo("#WED-" + dateStr + "-" + project.getId());
            dto.setCustomerName(book.getCustomer() != null ? book.getCustomer().getName() : "");
            dto.setPmName(book.getManager() != null ? book.getManager().getName() : "");
            dto.setWeddingDate(book.getWeddingDate());

            // 計算專案歷時 (建立日到現在，或結案日)
            if (project.getCreateAt() != null) {
                LocalDate createDate = project.getCreateAt().toLocalDate();
                LocalDate endDate = project.getUpdateAt() != null ? project.getUpdateAt().toLocalDate()
                        : LocalDate.now();
                int duration = (int) ChronoUnit.DAYS.between(createDate, endDate);
                dto.setDurationDays(duration);
            }
        }

        dto.setPaymentStatus(project.getPaymentStatus());

        // 轉換 Documents 列表
        if (project.getDocuments() != null) {
            List<ProjectResponse.RecordDTO.DocumentDTO> docDTOs = project.getDocuments().stream().map(doc -> {
                ProjectResponse.RecordDTO.DocumentDTO docDto = new ProjectResponse.RecordDTO.DocumentDTO();
                docDto.setFileName(doc.getName());
                docDto.setFileType(doc.getFileType());
                docDto.setUploadInfo("上傳者: " + (doc.getUploadedBy() != null ? doc.getUploadedBy().getName() : "未知"));
                docDto.setDownloadUrl(doc.getFilePath());
                return docDto;
            }).collect(Collectors.toList());
            dto.setDocuments(docDTOs);
        }

        // 轉換 Tasks 歷史軌跡列表 (同理轉換...)
        // 如果你需要，我也可以把 TaskHistoryDTO 的 mapping 寫出來

        return dto;
    }
}