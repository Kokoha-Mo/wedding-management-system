package com.wedding.wedding_management_system.repository;

import com.wedding.wedding_management_system.entity.ProjectCommunication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProjectCommunicationRepository extends JpaRepository<ProjectCommunication, Integer> {

        // 最佳實踐：透過方法名稱自動生成 SQL，依照建立時間「由新到舊」排序
        // SELECT * FROM project_communications WHERE project_id = ? ORDER BY create_at
        // DESC
        // 【修正】依照 Entity 實際變數：Project 的 pk 是 id，時間是 createAt
        List<ProjectCommunication> findByProject_IdOrderByCreateAtDesc(Integer projectId);

        // 巡邏員用：一次查詢找出未回覆的婚顧訊息（解決 N+1）
        @Query("""
                        SELECT pc FROM ProjectCommunication pc
                        WHERE pc.createBy = :companyName
                        AND pc.createAt BETWEEN :start AND :end
                        AND NOT EXISTS (
                            SELECT 1 FROM ProjectCommunication reply
                            WHERE reply.project.id = pc.project.id
                            AND reply.createBy <> :companyName
                            AND reply.createAt > pc.createAt
                        )
                        ORDER BY pc.createAt ASC
                        """)
        List<ProjectCommunication> findUnrepliedManagerMessages(
                        @Param("companyName") String companyName,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

}