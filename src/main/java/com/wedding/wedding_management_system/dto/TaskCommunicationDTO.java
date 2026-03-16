package com.wedding.wedding_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCommunicationDTO {
    private Integer creatorId;
    private String creatorName;
    private String creatorPosition;
    private LocalDateTime createAt;
    private String content;
}
