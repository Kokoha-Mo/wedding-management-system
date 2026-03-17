package com.wedding.wedding_management_system.dto;

import lombok.Data;

@Data
public class CustomerLoginResponseDto {
    private String token; // 讓 Controller 拿來設定 Cookie 用，不會直接回傳給前端
    private String email;
    private String name;
    private Integer customerId;
    private boolean forcePasswordChange;
    private Integer projectId;
}
