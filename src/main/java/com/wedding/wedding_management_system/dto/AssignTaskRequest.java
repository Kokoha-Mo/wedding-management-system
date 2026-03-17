package com.wedding.wedding_management_system.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class AssignTaskRequest {
    private List<Integer> assignees;
    private LocalDateTime deadline;
    private String managerContent;
}
