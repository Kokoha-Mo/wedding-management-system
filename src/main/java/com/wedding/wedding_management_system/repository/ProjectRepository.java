package com.wedding.wedding_management_system.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
    // 1. 查詢特定狀態的專案總數 (執行中專案)
    Long countByStatus(String status);

    // 2. 查詢特定狀態，且關聯的 Book 婚期落在指定日期範圍內 (本月即將結案)
    // 注意：Book_WeddingDate 代表尋找關聯實體 Book 裡面的 weddingDate 欄位
    Long countByStatusAndBook_WeddingDateBetween(String status, LocalDate startDate, LocalDate endDate);

    // 3. 查詢特定狀態，且專案更新時間(結案時間)落在指定時間範圍內 (本年度已完成)
    Long countByStatusAndUpdateAtBetween(String status, LocalDateTime startDateTime, LocalDateTime endDateTime);

    // 4. 依指定 manager ID 查詢其所負責的所有專案 (Project → Book → Employee.id)
    List<Project> findByBook_Manager_Id(Integer managerId);

    // 5. 依 manager ID 查詢特定狀態的專案數量
    Long countByStatusAndBook_Manager_Id(String status, Integer managerId);

    // 6. 依 manager ID 查詢特定狀態且婚期在範圍內的專案數量
    Long countByStatusAndBook_Manager_IdAndBook_WeddingDateBetween(
            String status, Integer managerId, LocalDate startDate, LocalDate endDate);

    // 7. 依 manager ID 查詢特定狀態且 updateAt 在範圍內的專案數量
    Long countByStatusAndBook_Manager_IdAndUpdateAtBetween(
            String status, Integer managerId, LocalDateTime startDateTime, LocalDateTime endDateTime);

}
