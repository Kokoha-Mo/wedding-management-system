package com.wedding.wedding_management_system.service;

import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.repository.ProjectTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import com.wedding.wedding_management_system.entity.ProjectTask;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ProjectTaskService {

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

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
}
