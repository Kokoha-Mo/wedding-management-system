package com.wedding.wedding_management_system.dto;

import lombok.Data;

@Data
public class MessageRequestDTO {
    private Integer taskId;
    private Integer createBy;
    private String content;
}
