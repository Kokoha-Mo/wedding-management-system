package com.wedding.wedding_management_system.service;

import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.repository.ProjectTaskRepository;
import com.wedding.wedding_management_system.repository.DocumentRepository;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import com.wedding.wedding_management_system.entity.Document;
import com.wedding.wedding_management_system.entity.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.entity.ProjectTask;
import com.wedding.wedding_management_system.repository.ProjectTaskRepository;

@Service
public class ProjectTaskService {

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private final String UPLOAD_DIR = "uploads/";

    public List<TaskDTO> getInProgressTasksByEmployeeId(Integer empId) {
        return projectTaskRepository.findTasksByEmployeeIdAndStatuses(empId, List.of("進行中"));
    }

    public List<TaskDTO> getHistoryTasksByEmployeeId(Integer empId) {
        return projectTaskRepository.findTasksByEmployeeIdAndStatuses(empId, List.of("待審核", "已完成"));
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
        List<ProjectTask> tasks = projectTaskRepository.findByProject_Id(projectId);
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
            resultList.add(dto);
        }
        return resultList; // 🌟 確保最後回傳的是轉換好的 DTO 陣列！
    }
}
