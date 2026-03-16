package com.wedding.wedding_management_system.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class EmpLoginDto {
    @JsonProperty("emp_email")
    private String empEmail;
    
    @JsonProperty("emp_password")
    private String empPassword;
}
