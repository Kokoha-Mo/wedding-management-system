package com.wedding.wedding_management_system.service;

import com.wedding.wedding_management_system.dto.MessageRequestDTO;
import com.wedding.wedding_management_system.dto.TaskCommunicationDTO;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.entity.ProjectTask;
import com.wedding.wedding_management_system.entity.TaskCommunication;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import com.wedding.wedding_management_system.repository.ProjectTaskRepository;
import com.wedding.wedding_management_system.repository.TaskCommunicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TaskCommunicationService {

    @Autowired
    private TaskCommunicationRepository taskCommunicationRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<TaskCommunicationDTO> getCommunicationsByTaskId(Integer taskId) {
        List<TaskCommunicationDTO> dtos = taskCommunicationRepository.findByTaskId(taskId);
        dtos.forEach(dto -> {
            if (dto.getCreateAt() != null) {
                dto.setCreateAt(dto.getCreateAt().plusHours(8));
            }
        });
        return dtos;
    }

    @Transactional
    public boolean saveCommunication(MessageRequestDTO request) {
        try {
            ProjectTask task = projectTaskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found"));
            Employee employee = employeeRepository.findById(request.getCreateBy())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            TaskCommunication tc = new TaskCommunication();
            tc.setTask(task);
            tc.setCreateBy(employee);
            tc.setContent(request.getContent());

            taskCommunicationRepository.save(tc);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
