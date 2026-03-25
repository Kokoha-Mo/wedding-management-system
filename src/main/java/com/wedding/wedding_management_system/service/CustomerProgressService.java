package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.wedding_management_system.entity.Project;
import com.wedding.wedding_management_system.entity.ProjectCommunication;
import com.wedding.wedding_management_system.entity.ProjectTask;
import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.repository.ProjectCommunicationRepository;
import com.wedding.wedding_management_system.repository.ProjectRepository;
import com.wedding.wedding_management_system.repository.ProjectTaskRepository;
import com.wedding.wedding_management_system.dto.ProjectProgressDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CustomerProgressService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectCommunicationRepository communicationRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private ProjectCommunicationService projectCommunicationService;

    /**
     * 1. 透過客戶 Email 取得專案整體資訊 (給客戶端前台使用)
     * 利用 JWT Token 解析出的 Email 來尋找專案，徹底取代 URL 上的 ?id=
     * 
     */
    @Transactional(readOnly = true)
    public ProjectProgressDTO getProgressByCustomerEmail(String email) {
        Project project = projectRepository.findFirstByBook_Customer_EmailOrderByCreateAtDesc(email)
                .orElseThrow(() -> new RuntimeException("找不到該客戶的專案紀錄，Email: " + email));
        return getProjectProgress(project.getId());
    }

    /**
     * 2. 取得專案整體資訊 (複雜的組裝邏輯)
     * 包含：右側的籌備進度(Tasks)，以及左側的基本資料、歷史留言紀錄(Timeline)
     * 對應頁面：customer_progress.html (畫面初次載入時呼叫)
     */
    @Transactional(readOnly = true)
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
            if (book.getCustomer() != null)
                dto.setCustomerName(book.getCustomer().getName());
            if (book.getManager() != null)
                dto.setPmName(book.getManager().getName());

            String dateStr = book.getWeddingDate() != null
                    ? book.getWeddingDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    : "Unknown";
            dto.setProjectNo("#WED-" + dateStr + "-" + project.getId());
            // ==========================================
            // 🌟 新增區塊：計算真實的付款資訊 (基於 projects 資料表)
            // ==========================================
            List<ProjectProgressDTO.PaymentDTO> paymentList = new java.util.ArrayList<>();

            // 取得專案總金額與付款狀態 (加上 Null 防呆)
            Integer total = project.getTotalPayment() != null ? project.getTotalPayment() : 0;
            String pStatus = project.getPaymentStatus() != null ? project.getPaymentStatus() : "";

            // 計算各階段金額 (訂金 20%, 期中款 50%, 尾款 30%)
            int deposit = (int) (total * 0.2);
            int midPayment = (int) (total * 0.5);
            int finalPayment = total - deposit - midPayment; // 確保加總不漏小數點

            // 狀態判定邏輯 (來自資料庫的真實狀態)
            boolean isDepositPaid = pStatus.contains("訂金") || pStatus.contains("期中") || pStatus.contains("尾款");
            boolean isMidPaid = pStatus.contains("期中") || pStatus.contains("尾款");
            boolean isFinalPaid = pStatus.contains("尾款");

            // 基準日 (婚期)
            LocalDate wedDate = book != null ? book.getWeddingDate() : null;

            // ==========================================
            // 🌟 商業邏輯：計算是否逾期 (OVERDUE)
            // ==========================================
            LocalDate today = LocalDate.now();
            boolean isMidOverdue = false;
            boolean isFinalOverdue = false;

            if (wedDate != null) {
                // 如果今天 >= 婚期前 3 個月，且「期中款未繳清」-> 標記為逾期
                if (!today.isBefore(wedDate.minusMonths(3)) && !isMidPaid) {
                    isMidOverdue = true;
                }
                // 如果今天 >= 婚期前 14 天，且「尾款未繳清」-> 標記為逾期
                if (!today.isBefore(wedDate.minusDays(14)) && !isFinalPaid) {
                    isFinalOverdue = true;
                }
            }
            // ==========================================

            // 1. 訂金 20% (通常立約即繳，若未繳視為待繳納)
            ProjectProgressDTO.PaymentDTO p1 = new ProjectProgressDTO.PaymentDTO();
            p1.setTitle("訂金 20% (NT$ " + String.format("%,d", deposit) + ")");
            p1.setStatus(isDepositPaid ? "PAID" : "PENDING");
            p1.setDueDate(isDepositPaid ? "" : "請盡速繳納");
            paymentList.add(p1);

            // 2. 期中款 50%
            ProjectProgressDTO.PaymentDTO p2 = new ProjectProgressDTO.PaymentDTO();
            p2.setTitle("期中款 50% (NT$ " + String.format("%,d", midPayment) + ")");
            if (isMidPaid) {
                p2.setStatus("PAID");
                p2.setDueDate(""); // 已繳清不顯示日期
            } else if (isMidOverdue) {
                p2.setStatus("OVERDUE");
                p2.setDueDate(wedDate != null
                        ? "已逾期 (" + wedDate.minusMonths(3).format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + " 前)"
                        : "已逾期 (繳款日期待確認)");
            } else {
                p2.setStatus(isDepositPaid ? "PENDING" : "NONE");
                p2.setDueDate(wedDate != null ? wedDate.minusMonths(3).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                        : "繳款日期待確認");
            }
            paymentList.add(p2);

            // 3. 尾款 30%
            ProjectProgressDTO.PaymentDTO p3 = new ProjectProgressDTO.PaymentDTO();
            p3.setTitle("尾款 30% (NT$ " + String.format("%,d", finalPayment) + ")");
            if (isFinalPaid) {
                p3.setStatus("PAID");
                p3.setDueDate("");
            } else if (isFinalOverdue) {
                p3.setStatus("OVERDUE");
                p3.setDueDate(wedDate != null
                        ? "已逾期 (" + wedDate.minusDays(14).format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + " 前)"
                        : "已逾期 (繳款日期待確認)");
            } else {
                p3.setStatus(isMidPaid ? "PENDING" : "NONE");
                p3.setDueDate(wedDate != null ? wedDate.minusDays(14).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                        : "繳款日期待確認");
            }
            paymentList.add(p3);

            // 將結果存入 DTO
            dto.setPayments(paymentList);
        }

        // ==========================================
        // 🌟 新增區塊：載入專案正式文件 (Documents)
        // ==========================================
        if (project.getDocuments() != null) {
            List<ProjectProgressDTO.DocumentDetail> formalDocs = project.getDocuments().stream()
                    // 🌟 核心修改：只過濾出 status 為 null 的檔案（PM 預設上傳的狀態）
                    .filter(doc -> doc.getStatus() == null)
                    .map(doc -> {
                        ProjectProgressDTO.DocumentDetail docDto = new ProjectProgressDTO.DocumentDetail();
                        docDto.setId(doc.getId());
                        docDto.setName(doc.getName());
                        docDto.setFileType(doc.getFileType());
                        docDto.setFilePath(doc.getFilePath());
                        return docDto;
                    }).collect(Collectors.toList());
            dto.setDocuments(formalDocs);
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
                commDto.setCreateBy(role != null ? role : "");
            }

            commDto.setContent(comm.getContent());
            commDto.setCreateAt(comm.getCreateAt() != null ? comm.getCreateAt().plusHours(8) : null);

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
        // ==========================================
        // 🌟 新增區塊：動態產生 Phases (手風琴時間軸) 與進度條
        // ==========================================
        List<ProjectTask> allTasks = projectTaskRepository.findByProjectId(projectId);
        List<ProjectProgressDTO.PhaseDTO> phases = new java.util.ArrayList<>();

        // --- Phase 1: 初步規劃 (假設專案成立即完成) ---
        ProjectProgressDTO.PhaseDTO p1 = new ProjectProgressDTO.PhaseDTO();
        p1.setId(1);
        p1.setTitle("初步規劃");
        p1.setStatus("completed");
        List<ProjectProgressDTO.TaskItemDTO> p1Tasks = new java.util.ArrayList<>();
        ProjectProgressDTO.TaskItemDTO t1 = new ProjectProgressDTO.TaskItemDTO();
        t1.setId(101);
        t1.setName("婚宴資訊確認");
        t1.setDone(true);
        ProjectProgressDTO.TaskItemDTO t2 = new ProjectProgressDTO.TaskItemDTO();
        t2.setId(102);
        t2.setName("專屬顧問指派");
        t2.setDone(true);
        p1Tasks.add(t1);
        p1Tasks.add(t2);
        p1.setTasks(p1Tasks);
        phases.add(p1);

        // --- Phase 2: 婚禮籌備 (動態從資料庫撈取) ---
        ProjectProgressDTO.PhaseDTO p2 = new ProjectProgressDTO.PhaseDTO();
        p2.setId(2);
        p2.setTitle("婚禮籌備");

        // 💡 巧思：將 Timeline 中「最新的一筆 PM 留言」抓出來，顯示在進度框裡！
        String pmNameStr = book != null && book.getManager() != null ? book.getManager().getName() : "公司";
        timeline.stream()
                .filter(c -> Objects.equals(c.getCreateBy(), pmNameStr))
                .findFirst() // 因為 timeline 已經是 OrderByDesc，第一筆就是最新
                .ifPresent(latestComm -> {
                    p2.setPmMessage(latestComm.getContent());
                    if (latestComm.getCreateAt() != null) {
                        p2.setPmUpdateTime(latestComm.getCreateAt().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")));
                    }
                });

        // 依照 Department (部門/分類) 來分組任務
        Map<String, List<ProjectTask>> groupedTasks = allTasks.stream()
                .filter(t -> t.getService() != null)
                .collect(Collectors.groupingBy(t -> {
                    // 🌟 除錯：這裡把 getName() 改成 getDeptName() 就沒問題了！
                    if (t.getService().getDepartment() != null
                            && t.getService().getDepartment().getDeptName() != null) {
                        return t.getService().getDepartment().getDeptName();
                    }
                    return "其他籌備項目";
                }));

        List<ProjectProgressDTO.CategoryDTO> categories = new java.util.ArrayList<>();
        int totalTasks = allTasks.size();
        int completedTasks = 0;

        for (Map.Entry<String, List<ProjectTask>> entry : groupedTasks.entrySet()) {
            ProjectProgressDTO.CategoryDTO cat = new ProjectProgressDTO.CategoryDTO();
            cat.setName(entry.getKey());
            List<ProjectProgressDTO.TaskItemDTO> catTasks = new java.util.ArrayList<>();

            for (ProjectTask pt : entry.getValue()) {
                ProjectProgressDTO.TaskItemDTO taskDto = new ProjectProgressDTO.TaskItemDTO();
                taskDto.setId(pt.getId());
                taskDto.setName(pt.getService().getName()); // 任務名稱等於 Service 名稱

                boolean isDone = "已完成".equals(pt.getStatus());
                taskDto.setDone(isDone);
                if (isDone)
                    completedTasks++;

                catTasks.add(taskDto);
            }
            cat.setTasks(catTasks);
            categories.add(cat);
        }
        p2.setCategories(categories);

        // 判斷 Phase 2 狀態：如果任務 > 0 且全部完成，就變成 completed
        if (totalTasks > 0 && completedTasks == totalTasks) {
            p2.setStatus("completed");
        } else {
            p2.setStatus("active");
        }
        phases.add(p2);

        // --- Phase 3: Happy Ending ---
        ProjectProgressDTO.PhaseDTO p3 = new ProjectProgressDTO.PhaseDTO();
        p3.setId(3);
        p3.setTitle("Happy Ending");
        p3.setStatus(p2.getStatus().equals("completed") ? "active" : "pending");
        p3.setTasks(new java.util.ArrayList<>());
        phases.add(p3);

        dto.setPhases(phases);

        // ==========================================
        // 🌟 修改區塊：計算圓環進度條百分比 (依據任務完成度計算)
        // ==========================================
        int calculatedProgress = 0;

        int totalTasksCount = projectTaskRepository.countByProjectId(projectId);
        int completedTasksCount = projectTaskRepository.countByProjectIdAndStatus(projectId, "已完成");

        // 🌟 將 Phase 1 預設完成的 2 個任務納入計算基礎
        int defaultTasks = 2;

        if (totalTasksCount > 0) {
            // 分母：實際總任務數 = DB撈出的任務 + 預設的2個任務
            int realTotalTasks = totalTasksCount + defaultTasks;
            // 分子：實際完成數 = DB完成的任務 + 預設的2個任務
            int realCompletedTasks = completedTasksCount + defaultTasks;

            // 計算真實比例
            calculatedProgress = (int) (((double) realCompletedTasks / realTotalTasks) * 100);
        } else {
            // 如果專案剛成立，DB 裡還沒有 PM 建立的籌備任務，給定一個「基礎起始進度」(10%)。
            // 這在 UX 體驗上代表：「初步規劃已完成，專案已啟動，正在等待顧問排程」
            calculatedProgress = 10;
        }

        // 🌟 防呆：確保算出來的數字絕對落在 0 ~ 100 之間，以免前端 SVG 圓環爆掉
        dto.setProgressPercent(Math.min(Math.max(calculatedProgress, 0), 100));

        return dto; // 完美回傳！
    }

    /**
     * 3. 透過客戶 Email 新增專案留言與附件 (橋樑模式)
     */
    @Transactional
    public void addCommunicationByCustomerEmail(String email, String content, String createBy, MultipartFile[] files)
            throws Exception {
        Project project = projectRepository.findFirstByBook_Customer_EmailOrderByCreateAtDesc(email)
                .orElseThrow(() -> new RuntimeException("找不到該客戶的專案紀錄，無法新增留言"));

        List<MultipartFile> fileList = new java.util.ArrayList<>();
        if (files != null && files.length > 0) {
            fileList = java.util.Arrays.asList(files);
        }

        projectCommunicationService.addProjectCommunicationWithFiles(project.getId(), createBy, content, fileList);
    }
}