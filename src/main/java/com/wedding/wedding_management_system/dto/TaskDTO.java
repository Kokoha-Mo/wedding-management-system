package com.wedding.wedding_management_system.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDTO {

    private Integer taskId;
    private String serviceName;
    private String customerName;
    private LocalDateTime deadline;
    private String managerContent;
    private LocalDateTime updateAt;

    // --- 從 Service 拉出來的資訊 ---
    private Integer serviceId;

    // --- 從 Service -> Category -> Department 拉出來的資訊 ---
    private Integer deptId;

    // --- 負責人清單 (因為一個任務可以有多個負責人) ---
    private List<AssigneeDTO> assignees;

    // 內部類別：負責人資訊
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssigneeDTO {

        private Integer empId;
        private String name;
        private String departmentName; // (可選) 員工所屬部門
    }
}
