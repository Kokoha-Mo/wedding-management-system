package com.wedding.wedding_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfileDTO {
    private Integer empId;
    private String name;
    private String role;
    private String email;
    private String tel;
    private String imgPath;
    private String departmentName;
}
