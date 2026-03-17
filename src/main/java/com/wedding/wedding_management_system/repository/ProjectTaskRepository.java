package com.wedding.wedding_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.entity.ProjectTask;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Integer> {

    @Query("SELECT new com.wedding.wedding_management_system.dto.TaskDTO("
            + "t.id, t.status, s.name, c.name, t.deadline, t.managerContent, t.updateAt) "
            + "FROM ProjectTask t "
            + "JOIN t.service s "
            + "JOIN t.project p "
            + "JOIN p.book b "
            + "JOIN b.customer c "
            + "JOIN t.taskOwners to "
            + "WHERE to.employee.id = :empId AND t.status IN :statuses")
    List<TaskDTO> findTasksByEmployeeIdAndStatuses(@Param("empId") Integer empId,
            @Param("statuses") List<String> statuses);

    List<ProjectTask> findByProject_Id(Integer projectId);
}
