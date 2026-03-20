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
        if (tasks == null || tasks.isEmpty()) return;
        for (TaskDTO dto : tasks) {
            List<Document> docs = documentRepository.findByTaskId(dto.getTaskId());
            if (docs != null && !docs.isEmpty()) {
                dto.setDocuments(docs.stream()
                    .map(d -> new TaskDTO.DocumentDTO(d.getId(), d.getName(), d.getFilePath(), d.getFileType(), d.getStatus()))
                    .collect(Collectors.toList()));
            }
        }
    }

    @Transactional
    public boolean updateTaskStatus(Integer taskId, String status) {
        try {
            ProjectTask task = projectTaskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            task.setStatus(status);
            task.setUpdateAt(LocalDateTime.now());
            projectTaskRepository.save(task);
            return true;
        } catch (Exception e) {
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

            // 3. 儲存紀錄到 Document 表
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
