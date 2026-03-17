package com.wedding.wedding_management_system.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wedding.wedding_management_system.dto.ProjectProgressDTO;
import com.wedding.wedding_management_system.dto.ProjectResponse;
import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.entity.Project;
import com.wedding.wedding_management_system.entity.ProjectCommunication;
import com.wedding.wedding_management_system.repository.ProjectCommunicationRepository;
import com.wedding.wedding_management_system.repository.ProjectRepository;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectCommunicationRepository communicationRepository;

    /**
     * 共用：將 Project 實體轉換成 ListDTO
     */
    private ProjectResponse.ListDTO toListDTO(Project project) {
        ProjectResponse.ListDTO dto = new ProjectResponse.ListDTO();
        dto.setProjectId(project.getId());
        dto.setStatus(project.getStatus());

        Book book = project.getBook();
        if (book != null) {
            dto.setWeddingDate(book.getWeddingDate());

            if (book.getWeddingDate() != null) {
                long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), book.getWeddingDate());
                dto.setDaysRemaining(daysRemaining);

                String dateStr = book.getWeddingDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                dto.setProjectNo("#WED-" + dateStr + "-" + project.getId());
            }

            if (book.getCustomer() != null) {
                dto.setCustomerName(book.getCustomer().getName());
            }
        }
        dto.setUpdateAt(project.getUpdateAt());

        if (project.getProjectTasks() != null) {
            List<ProjectResponse.ListDTO.TaskSummaryDTO> taskDTOs = project.getProjectTasks().stream().map(task -> {
                ProjectResponse.ListDTO.TaskSummaryDTO ts = new ProjectResponse.ListDTO.TaskSummaryDTO();
                ts.setStatus(task.getStatus());
                if (task.getService() != null) {
                    ts.setTaskName(task.getService().getName());
                    if (task.getService().getDepartment() != null) {
                        ts.setDeptName(task.getService().getDepartment().getDeptName());
                    }
                }
                return ts;
            }).collect(Collectors.toList());
            dto.setTasks(taskDTOs);
        }

        return dto;
    }

    /**
     * 1a. 取得所有專案列表（管理員用，保留備用）
     */
    public List<ProjectResponse.ListDTO> getAllProjectLists() {
        return projectRepository.findAll().stream()
                .map(this::toListDTO)
                .collect(Collectors.toList());
    }

    /**
     * 1b. 取得指定 Manager 負責的專案列表（PM 登入後的主頁）
     */
    public List<ProjectResponse.ListDTO> getProjectsByManagerId(Integer managerId) {
        log.info("[DEBUG] getProjectsByManagerId 被呼叫， managerId = {}", managerId);
        List<Project> projects = projectRepository.findByBook_Manager_Id(managerId);
        log.info("[DEBUG] 查詢到 {} 筆專案", projects.size());
        return projects.stream()
                .map(this::toListDTO)
                .collect(Collectors.toList());
    }

    /**
     * 2a. 取得統計數據 - 全部（管理員用，保留備用）
     */
    public ProjectResponse.DashboardDTO getDashboardStats() {
        ProjectResponse.DashboardDTO dashboard = new ProjectResponse.DashboardDTO();
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = YearMonth.from(today).atEndOfMonth();
        LocalDateTime firstDayOfYear = LocalDateTime.of(today.getYear(), 1, 1, 0, 0);
        LocalDateTime lastDayOfYear = LocalDateTime.of(today.getYear(), 12, 31, 23, 59, 59);

        dashboard.setActiveProjectsCount(projectRepository.countByStatus("進行中"));
        dashboard.setEndingThisMonthCount(projectRepository.countByStatusAndBook_WeddingDateBetween(
                "進行中", firstDayOfMonth, lastDayOfMonth));
        dashboard.setCompletedThisYearCount(projectRepository.countByStatusAndUpdateAtBetween(
                "已完成", firstDayOfYear, lastDayOfYear));
        return dashboard;
    }

    /**
     * 2b. 取得統計數據 - 依指定 Manager（PM 登入後的 Dashboard）
     */
    public ProjectResponse.DashboardDTO getDashboardStatsByManagerId(Integer managerId) {
        ProjectResponse.DashboardDTO dashboard = new ProjectResponse.DashboardDTO();
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = YearMonth.from(today).atEndOfMonth();
        LocalDateTime firstDayOfYear = LocalDateTime.of(today.getYear(), 1, 1, 0, 0);
        LocalDateTime lastDayOfYear = LocalDateTime.of(today.getYear(), 12, 31, 23, 59, 59);

        dashboard.setActiveProjectsCount(
                projectRepository.countByStatusAndBook_Manager_Id("進行中", managerId));
        dashboard.setEndingThisMonthCount(
                projectRepository.countByStatusAndBook_Manager_IdAndBook_WeddingDateBetween(
                        "進行中", managerId, firstDayOfMonth, lastDayOfMonth));
        dashboard.setCompletedThisYearCount(
                projectRepository.countByStatusAndBook_Manager_IdAndUpdateAtBetween(
                        "已完成", managerId, firstDayOfYear, lastDayOfYear));
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

    /**
     * 4. 取得專案整體資訊 (GET) 包含：右側的籌備進度(Tasks)，以及左側的基本資料、歷史留言紀錄(Timeline)
     * 對應頁面：customer_progress.html (畫面初次載入時呼叫)
     */
    public ProjectProgressDTO getProjectProgress(Integer projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("找不到該專案 ID: " + projectId));

        // 注意這裡也要改成 CreateAt
        List<ProjectCommunication> comms = communicationRepository
                .findByProject_IdOrderByCreateAtDesc(projectId);

        ProjectProgressDTO dto = new ProjectProgressDTO();
        dto.setProjectId(project.getId());
        dto.setStatus(project.getStatus());

        // 防呆：從 Book 抓取關聯資料
        Book book = project.getBook();
        if (book != null) {
            dto.setWeddingDate(book.getWeddingDate());
            if (book.getCustomer() != null) {
                dto.setCustomerName(book.getCustomer().getName());
            }
            if (book.getManager() != null) {
                dto.setPmName(book.getManager().getName());
            }

            String dateStr = book.getWeddingDate() != null
                    ? book.getWeddingDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    : "Unknown";
            dto.setProjectNo("#WED-" + dateStr + "-" + project.getId());
        }

        // 使用 Stream API 轉換留言與附件
        List<ProjectProgressDTO.CommunicationDetail> timeline = comms.stream().map(comm -> {
            ProjectProgressDTO.CommunicationDetail commDto = new ProjectProgressDTO.CommunicationDetail();
            commDto.setId(comm.getId());

            // 🌟 核心修改：判斷 createBy 欄位，將「角色標籤」翻譯成「真實姓名」
            String role = comm.getCreateBy();
            if ("客戶".equals(role) && book != null && book.getCustomer() != null) {
                commDto.setCreateBy(book.getCustomer().getName());
            } else if ("公司".equals(role) && book != null && book.getManager() != null) {
                commDto.setCreateBy(book.getManager().getName());
            } else {
                // 防呆：如果是舊資料或是直接寫入的名字，就照原樣顯示
                commDto.setCreateBy(role);
            }

            commDto.setContent(comm.getContent());
            commDto.setCreateAt(comm.getCreateAt());

            if (comm.getDocuments() != null) {
                List<ProjectProgressDTO.DocumentDetail> docs = comm.getDocuments().stream().map(doc -> {
                    ProjectProgressDTO.DocumentDetail docDto = new ProjectProgressDTO.DocumentDetail();
                    docDto.setId(doc.getId());
                    docDto.setName(doc.getName());
                    docDto.setFileType(doc.getFileType());
                    docDto.setFilePath(doc.getFilePath());
                    return docDto;
                }).collect(Collectors.toList());
                commDto.setDocuments(docs);
            }
            return commDto;
        }).collect(Collectors.toList());

        dto.setTimeline(timeline);
        return dto;
    }

}
