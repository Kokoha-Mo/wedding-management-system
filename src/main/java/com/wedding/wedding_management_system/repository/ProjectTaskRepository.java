package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.wedding.wedding_management_system.entity.ProjectTask;
import com.wedding.wedding_management_system.dto.TaskDTO;
import java.util.List;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Integer> {

    @Query("SELECT new com.wedding.wedding_management_system.dto.TaskDTO(" +
            "t.id, s.name, c.name, t.deadline, t.managerContent, t.updateAt, t.status) " +
            "FROM ProjectTask t " +
            "JOIN t.service s " +
            "JOIN t.project p " +
            "JOIN p.book b " +
            "JOIN b.customer c " +
            "JOIN t.taskOwners own " +
            "WHERE own.employee.id = :empId AND t.status IN :statuses")
    List<TaskDTO> findTasksByEmployeeIdAndStatuses(@Param("empId") Integer empId, @Param("statuses") List<String> statuses);
}
