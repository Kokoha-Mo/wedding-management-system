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

import java.util.List;

import com.wedding.wedding_management_system.entity.ProjectTask;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

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
}
