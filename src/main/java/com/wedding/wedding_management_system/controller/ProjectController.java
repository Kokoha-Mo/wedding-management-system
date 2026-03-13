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
import com.wedding.wedding_management_system.service.ProjectService;

@RestController
@RequestMapping("/api/manager/projects")
@CrossOrigin(origins = "*") // 【DEBUG 救星】開發初期先加上這個，避免你的 HTML fetch 時出現 CORS 跨域錯誤
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * 1. 取得專案列表 對應前端：Table 列表顯示 測試網址：GET
     * http://localhost:8080/api/manager/projects
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse.ListDTO>> getAllProjects() {
        List<ProjectResponse.ListDTO> projects = projectService.getAllProjectLists();
        return ResponseEntity.ok(projects); // 回傳 HTTP 200 與 JSON 資料
    }

    /**
     * 2. 取得單一專案的結案紀錄 對應前端：點擊「查看紀錄」跳出的 Modal 彈窗 測試網址：GET
     * http://localhost:8080/api/manager/projects/{projectId}/record (例如
     * /api/projects/1/record)
     */
    @GetMapping("/{projectId}/record")
    public ResponseEntity<ProjectResponse.RecordDTO> getProjectRecord(@PathVariable Integer projectId) {
        try {
            ProjectResponse.RecordDTO record = projectService.getProjectRecord(projectId);
            return ResponseEntity.ok(record);
        } catch (RuntimeException e) {
            // 【DEBUG 友善】如果 Service 找不到專案拋出錯誤，這裡攔截並回傳 404 Not Found
            // 未來如果我們加上「全域例外處理 (Global Exception Handler)」，這層 try-catch 就可以完全省略！
            return ResponseEntity.notFound().build();
        }
    }
}
