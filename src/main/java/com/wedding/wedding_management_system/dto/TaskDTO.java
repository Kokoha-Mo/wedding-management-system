package com.wedding.wedding_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDTO {
    private Integer taskId;
    private String serviceName;
    private String customerName;
    private LocalDateTime deadline;
    private String managerContent;
}
