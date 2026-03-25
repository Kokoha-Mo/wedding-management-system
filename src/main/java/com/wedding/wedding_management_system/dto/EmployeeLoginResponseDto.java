package com.wedding.wedding_management_system.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeLoginResponseDto {
    private String token; // For cookie passing, set to null before returning
    private Integer empId;
    private String empName;
    private String position;
    private Integer deptId;
    private String imgPath;
}
