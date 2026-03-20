package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.wedding.wedding_management_system.entity.Document;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    /**
     * 根據 task_id 和 status 查詢附檔
     * 用於「查看成果」時精準取得特定任務的待審核附檔
     */
    List<Document> findByTask_IdAndStatus(Integer taskId, String status);

    /**
     * 根據 task_id 和 status 刪除附檔（駁回時清除待審核附檔）
     */
    @Modifying
    @Query("DELETE FROM Document d WHERE d.task.id = :taskId AND d.status = :status")
    void deleteByTaskIdAndStatus(@Param("taskId") Integer taskId, @Param("status") String status);
}
