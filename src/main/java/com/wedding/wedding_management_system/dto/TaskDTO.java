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
    private String status;
    private String serviceName;
    private String customerName;
    private LocalDateTime deadline;
    private String managerContent;
    private LocalDateTime updateAt;

    // --- 從 Service 拉出來的資訊 ---
    private Integer serviceId;

    // --- 從 Service -> Category -> Department 拉出來的資訊 ---
    private Integer deptId;
    private String deptName;

    // --- 負責人清單 (因為一個任務可以有多個負責人) ---
    private List<AssigneeDTO> assignees;

    // --- 成果附檔清單（待審核狀態的 documents）---
    private List<DocumentDTO> documents;

    // 給 EmployeeController / ProjectTaskRepository 的 JPQL 使用的建構子！
    public TaskDTO(Integer taskId, String status, String serviceName, String customerName, LocalDateTime deadline,
            String managerContent, LocalDateTime updateAt) {
        this.taskId = taskId;
        this.status = status;
        this.serviceName = serviceName;
        this.customerName = customerName;
        this.deadline = deadline;
        this.managerContent = managerContent;
        this.updateAt = updateAt;
    }

    // 內部類別：負責人資訊
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssigneeDTO {
        private Integer empId;
        private String name;
        private String departmentName;
    }

    // 內部類別：附檔資訊
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentDTO {
        private Integer id;
        private String name;
        private String filePath;
        private String fileType;
        private String status;
    }
}
