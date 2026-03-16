package com.wedding.wedding_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCommunicationDTO {
    private String createBy;
    private LocalDateTime createAt;
    private String content;
}
