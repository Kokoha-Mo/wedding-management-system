package com.wedding.wedding_management_system.repository;

import com.wedding.wedding_management_system.entity.ProjectCommunication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectCommunicationRepository extends JpaRepository<ProjectCommunication, Integer> {
    
    // 最佳實踐：透過方法名稱自動生成 SQL，依照建立時間「由新到舊」排序
    // SELECT * FROM project_communications WHERE project_id = ? ORDER BY create_at DESC
    // 【修正】依照 Entity 實際變數：Project 的 pk 是 id，時間是 createAt
    List<ProjectCommunication> findByProject_IdOrderByCreateAtDesc(Integer projectId);
    
}