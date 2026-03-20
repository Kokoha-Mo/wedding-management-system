package com.wedding.wedding_management_system.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.entity.Document;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.entity.ProjectTask;
import com.wedding.wedding_management_system.entity.TaskOwner;
import com.wedding.wedding_management_system.repository.DocumentRepository;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import com.wedding.wedding_management_system.repository.ProjectTaskRepository;
import com.wedding.wedding_management_system.repository.TaskOwnerRepository;

@Service
public class ProjectTaskService {

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaskOwnerRepository taskOwnerRepository;

    private final String UPLOAD_DIR = "uploads/";

    public List<TaskDTO> getInProgressTasksByEmployeeId(Integer empId) {
        List<TaskDTO> tasks = projectTaskRepository.findTasksByEmployeeIdAndStatuses(empId, List.of("進行中"));
        populateDocuments(tasks);
        return tasks;
    }

    public List<TaskDTO> getHistoryTasksByEmployeeId(Integer empId) {
        List<TaskDTO> tasks = projectTaskRepository.findTasksByEmployeeIdAndStatuses(empId, List.of("待審核", "已完成"));
        populateDocuments(tasks);
        return tasks;
    }

    private void populateDocuments(List<TaskDTO> tasks) {
        if (tasks == null || tasks.isEmpty())
            return;
        for (TaskDTO dto : tasks) {
            List<Document> docs = documentRepository.findByTask_Id(dto.getTaskId());
            if (docs != null && !docs.isEmpty()) {
                dto.setDocuments(docs.stream()
                        .map(d -> new TaskDTO.DocumentDTO(d.getId(), d.getName(), d.getFilePath(), d.getFileType(),
                                d.getStatus()))
                        .collect(Collectors.toList()));
            }
        }
    }

    @Transactional
    public boolean updateTaskStatus(Integer taskId, String status) {
        try {
            ProjectTask task = projectTaskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            // 【駁回邏輯】狀態改回「待指派」時，清除此任務的待審核附檔（DB 記錄 + 實體檔案）
            if ("待指派".equals(status)) {
                List<Document> pendingDocs = documentRepository.findByTask_IdAndStatus(taskId, "待審核");
                for (Document doc : pendingDocs) {
                    if (doc.getFilePath() != null) {
                        try {
                            Path fileToDelete = Paths.get(doc.getFilePath());
                            Files.deleteIfExists(fileToDelete);
                        } catch (IOException ioEx) {
                            // 實體檔案刪除失敗只記錄 log，不影響主流程
                            ioEx.printStackTrace();
                        }
                    }
                }
                documentRepository.deleteByTaskIdAndStatus(taskId, "待審核");
            }

            // 【審核通過邏輯】狀態改為「已完成」時，將附檔由「待審核」改為「已核准」
            if ("已完成".equals(status)) {
                documentRepository.updateStatusByTaskIdAndOldStatus(taskId, "待審核", "已核准");
            }

            task.setStatus(status);
            task.setUpdateAt(LocalDateTime.now());
            projectTaskRepository.save(task);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public boolean uploadTaskFile(MultipartFile file, Integer taskId, Integer empId) {
        try {
            // 1. 查找相關實體
            ProjectTask task = projectTaskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            Employee employee = employeeRepository.findById(empId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            // 2. 處理檔案儲存
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            // 3. 儲存紀錄到 Document 表（同時記錄 task 關聯，供管理端「查看成果」精準抓取）
            Document document = new Document();
            document.setProject(task.getProject());
            document.setTask(task);
            document.setUploadedBy(employee);
            document.setName(originalFilename);
            document.setFilePath(UPLOAD_DIR + fileName);
            document.setFileType(file.getContentType());
            document.setStatus("待審核");

            documentRepository.save(document);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<TaskDTO> getTasksByProjectId(Integer projectId) {
        List<ProjectTask> tasks = projectTaskRepository.findByProjectId(projectId);
        List<TaskDTO> resultList = new ArrayList<>();

        for (ProjectTask task : tasks) {
            TaskDTO dto = new TaskDTO();

            dto.setTaskId(task.getId()); // 注意：如果你的主鍵是 id，這裡就寫 getId()
            dto.setStatus(task.getStatus());
            dto.setDeadline(task.getDeadline());
            dto.setManagerContent(task.getManagerContent());
            dto.setUpdateAt(task.getUpdateAt());

            // 處理服務與部門
            if (task.getService() != null) {
                dto.setServiceId(task.getService().getId()); // 假設是 getId()
                dto.setServiceName(task.getService().getName());

                if (task.getService().getDepartment() != null) {
                    dto.setDeptId(task.getService().getDepartment().getId()); // 假設是 getId()
                    dto.setDeptName(task.getService().getDepartment().getDeptName());
                }
            }

            // Fetch assignees
            if (task.getTaskOwners() != null && !task.getTaskOwners().isEmpty()) {
                List<TaskDTO.AssigneeDTO> assignees = task.getTaskOwners().stream().map(owner -> {
                    TaskDTO.AssigneeDTO assignee = new TaskDTO.AssigneeDTO();
                    if (owner.getEmployee() != null) {
                        assignee.setEmpId(owner.getEmployee().getId());
                        assignee.setName(owner.getEmployee().getName());
                        if (owner.getEmployee().getDepartment() != null) {
                            assignee.setDepartmentName(owner.getEmployee().getDepartment().getDeptName());
                        }
                    }
                    return assignee;
                }).collect(Collectors.toList());
                dto.setAssignees(assignees);
            }
            
            // 處理成果附檔 (移到 if (taskOwners) 之外，確保不論是否有指派人員都能顯示附檔)
            // 根據任務狀態決定查詢哪種 status 的附檔
            // - 待審核：員工已上傳尚未審核的附檔
            // - 已完成 / 已結案：審核通過後已更新為已核准的附檔
            String docStatus = null;
            String taskStatus = task.getStatus();
            if ("待審核".equals(taskStatus)) {
                docStatus = "待審核";
            } else if ("已完成".equals(taskStatus) || "已結案".equals(taskStatus)) {
                docStatus = "已核准";
            }

            if (docStatus != null) {
                List<Document> docs = documentRepository.findByTask_IdAndStatus(task.getId(), docStatus);
                if (!docs.isEmpty()) {
                    List<TaskDTO.DocumentDTO> docDTOs = docs.stream().map(d -> {
                        TaskDTO.DocumentDTO docDto = new TaskDTO.DocumentDTO();
                        docDto.setId(d.getId());
                        docDto.setName(d.getName());
                        docDto.setFilePath(d.getFilePath());
                        docDto.setFileType(d.getFileType());
                        docDto.setStatus(d.getStatus());
                        return docDto;
                    }).collect(Collectors.toList());
                    dto.setDocuments(docDTOs);
                }
            }

            resultList.add(dto);
        }
        return resultList; // 🌟 確保最後回傳的是轉換好的 DTO 陣列！
    }

    @Transactional
    public boolean assignTask(Integer taskId, List<Integer> assignees, LocalDateTime deadline, String managerContent) {
        try {
            ProjectTask task = projectTaskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            // 【修正】先刪除舊的 TaskOwner 記錄，避免重複指派時出現舊數據
            if (task.getTaskOwners() != null && !task.getTaskOwners().isEmpty()) {
                taskOwnerRepository.deleteAll(task.getTaskOwners());
            }

            // Save task owners
            if (assignees != null && !assignees.isEmpty()) {
                for (Integer empId : assignees) {
                    Employee employee = employeeRepository.findById(empId)
                            .orElseThrow(() -> new RuntimeException("Employee not found"));
                    TaskOwner owner = new TaskOwner();
                    owner.setTask(task);
                    owner.setEmployee(employee);
                    taskOwnerRepository.save(owner);
                }
            }

            // Update task status, deadline, managerContent
            task.setStatus("進行中");
            task.setUpdateAt(LocalDateTime.now());
            if (deadline != null) {
                task.setDeadline(deadline);
            }
            if (managerContent != null) {
                task.setManagerContent(managerContent);
            }
            projectTaskRepository.save(task);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
