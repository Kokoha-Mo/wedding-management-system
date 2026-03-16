package com.wedding.wedding_management_system.dto;

import lombok.Data;

@Data
public class TaskStatusRequestDTO {
    private Integer taskId;
    private String status;
}
