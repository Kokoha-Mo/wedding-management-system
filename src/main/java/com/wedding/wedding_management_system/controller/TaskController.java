package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.dto.MessageRequestDTO;
import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.dto.TaskCommunicationDTO;
import com.wedding.wedding_management_system.service.ProjectTaskService;
import com.wedding.wedding_management_system.service.TaskCommunicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class TaskController {

    @Autowired
    private ProjectTaskService projectTaskService;

    @Autowired
    private TaskCommunicationService taskCommunicationService;

    @GetMapping("/task/{emp_id}")
    public ResponseEntity<List<TaskDTO>> getTasksByEmployee(@PathVariable("emp_id") Integer empId) {
        List<TaskDTO> tasks = projectTaskService.getTasksByEmployeeId(empId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tc/{task_id}")
    public ResponseEntity<List<TaskCommunicationDTO>> getTaskCommunications(@PathVariable("task_id") Integer taskId) {
        List<TaskCommunicationDTO> communications = taskCommunicationService.getCommunicationsByTaskId(taskId);
        return ResponseEntity.ok(communications);
    }

    /*
     * {
     * "taskId": Long,
     * "createBy": Long,
     * "content": "String"
     * }
     */
    @PostMapping("/tc/mesg")
    public ResponseEntity<Map<String, String>> createMessage(@RequestBody MessageRequestDTO request) {
        boolean success = taskCommunicationService.saveCommunication(request);
        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "success", "message", "留言已送出"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "儲存失敗"));
        }
    }
}
