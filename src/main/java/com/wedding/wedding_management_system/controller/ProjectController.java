package com.wedding.wedding_management_system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.wedding_management_system.dto.ProjectResponse;
import com.wedding.wedding_management_system.dto.TaskDTO;
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
}
