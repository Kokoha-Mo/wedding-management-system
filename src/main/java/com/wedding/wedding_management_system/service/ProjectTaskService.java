package com.wedding.wedding_management_system.service;

import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.repository.ProjectTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectTaskService {

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    public List<TaskDTO> getTasksByEmployeeId(Integer empId) {
        return projectTaskRepository.findTasksByEmployeeId(empId);
    }
}
