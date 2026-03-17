package com.wedding.wedding_management_system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

import com.wedding.wedding_management_system.dto.ProjectResponse;
import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.dto.AssignTaskRequest;
import com.wedding.wedding_management_system.service.ProjectService;
import com.wedding.wedding_management_system.service.ProjectTaskService;

@RestController
@RequestMapping("/api/manager")
@CrossOrigin(originPatterns = "*") // 使用 originPatterns 才能同時支援 credentials + 通配符
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectTaskService projectTaskService;

    @Autowired
    private com.wedding.wedding_management_system.service.CustomerProgressService customerProgressService;

    @Autowired
    private com.wedding.wedding_management_system.service.ProjectCommunicationService projectCommunicationService;

    /**
     * 1. 取得指定 Manager 負責的專案列表
     * 對應前端：project.html Table 列表顯示
     * 測試網址：GET http://localhost:8080/api/manager/{managerId}/projects
     * 例如：GET http://localhost:8080/api/manager/1/projects
     */
    @GetMapping("/{managerId}/projects")
    public ResponseEntity<List<ProjectResponse.ListDTO>> getProjectsByManager(
            @PathVariable("managerId") Integer managerId) {
        List<ProjectResponse.ListDTO> projects = projectService.getProjectsByManagerId(managerId);
        return ResponseEntity.ok(projects);
    }

    /**
     * 2. 取得指定 Manager 的 Dashboard 統計卡片數據
     * 測試網址：GET http://localhost:8080/api/manager/{managerId}/dashboard
     */
    @GetMapping("/{managerId}/dashboard")
    public ResponseEntity<ProjectResponse.DashboardDTO> getDashboardByManager(
            @PathVariable("managerId") Integer managerId) {
        ProjectResponse.DashboardDTO stats = projectService.getDashboardStatsByManagerId(managerId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 3. 取得單一專案的結案紀錄 對應前端：點擊「查看紀錄」跳出的 Modal 彈窗
     * 測試網址：GET http://localhost:8080/api/manager/projects/{projectId}/record
     */
    @GetMapping("/projects/{projectId}/record")
    public ResponseEntity<ProjectResponse.RecordDTO> getProjectRecord(@PathVariable("projectId") Integer projectId) {
        try {
            ProjectResponse.RecordDTO record = projectService.getProjectRecord(projectId);
            return ResponseEntity.ok(record);
        } catch (RuntimeException e) {
            // 【DEBUG 友善】如果 Service 找不到專案拋出錯誤，這裡攔截並回傳 404 Not Found
            return ResponseEntity.notFound().build();
        }
    }

    // 獲取該專案底下的所有任務清單
    // 測試網址：GET http://localhost:8080/api/manager/projects/{projectId}/tasks
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskDTO>> getProjectTasks(@PathVariable("projectId") Integer projectId) {
        try {
            List<TaskDTO> tasks = projectTaskService.getTasksByProjectId(projectId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // Assign project task
    // 測試網址：POST http://localhost:8080/api/manager/projects/tasks/{taskId}/assign
    @PostMapping("/projects/tasks/{taskId}/assign")
    public ResponseEntity<?> assignProjectTask(
            @PathVariable("taskId") Integer taskId, 
            @RequestBody AssignTaskRequest request) {
        try {
            boolean success = projectTaskService.assignTask(
                taskId, 
                request.getAssignees(), 
                request.getDeadline(), 
                request.getManagerContent()
            );

            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "Task successfully assigned");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to assign task");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Update task status
    // 測試網址：POST http://localhost:8080/api/manager/projects/tasks/{taskId}/status
    @PostMapping("/projects/tasks/{taskId}/status")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable("taskId") Integer taskId, 
            @RequestBody Map<String, String> request) {
        try {
            String newStatus = request.get("status");
            boolean success = projectTaskService.updateTaskStatus(taskId, newStatus);
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "Task status updated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to update task status");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get project communication timeline
    @GetMapping("/projects/{projectId}/communication")
    public ResponseEntity<?> getProjectCommunication(
            @PathVariable("projectId") Integer projectId) {
        try {
            com.wedding.wedding_management_system.dto.ProjectProgressDTO progress = customerProgressService.getProjectProgress(projectId);
            Map<String, Object> response = new HashMap<>();
            response.put("pmName", progress.getPmName());
            response.put("customerName", progress.getCustomerName());
            response.put("timeline", progress.getTimeline());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // Post project communication
    @PostMapping("/projects/{projectId}/communication")
    public ResponseEntity<?> postProjectCommunication(
            @PathVariable("projectId") Integer projectId,
            @org.springframework.web.bind.annotation.RequestParam(value = "content", required = false) String content,
            @org.springframework.web.bind.annotation.RequestParam("createBy") String createBy,
            @org.springframework.web.bind.annotation.RequestParam(value = "files", required = false) org.springframework.web.multipart.MultipartFile[] files) {
        try {
            java.util.List<org.springframework.web.multipart.MultipartFile> fileList = new java.util.ArrayList<>();
            if (files != null && files.length > 0) {
                fileList = java.util.Arrays.asList(files);
            }
            projectCommunicationService.addProjectCommunicationWithFiles(projectId, createBy, content, fileList);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
