package com.wedding.wedding_management_system.repository;

import com.wedding.wedding_management_system.entity.TaskCommunication;
import com.wedding.wedding_management_system.dto.TaskCommunicationDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskCommunicationRepository extends JpaRepository<TaskCommunication, Integer> {
    
    @Query("SELECT new com.wedding.wedding_management_system.dto.TaskCommunicationDTO(" +
           "e.name, tc.createAt, tc.content) " +
           "FROM TaskCommunication tc " +
           "JOIN tc.createBy e " +
           "WHERE tc.task.id = :taskId " +
           "ORDER BY tc.createAt ASC")
    List<TaskCommunicationDTO> findByTaskId(@Param("taskId") Integer taskId);
}
