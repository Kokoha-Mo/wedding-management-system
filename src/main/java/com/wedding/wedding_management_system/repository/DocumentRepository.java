package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.wedding.wedding_management_system.entity.Document;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    /**
     * 根據 project_id、status 以及上傳者 empId 清單查詢文件
     * 用於「查看成果」時取得任務負責人所上傳的待審核附檔
     */
    @Query("SELECT d FROM Document d WHERE d.project.id = :projectId AND d.status = :status AND d.uploadedBy.id IN :uploaderIds")
    List<Document> findByProjectIdAndStatusAndUploaderIds(
            @Param("projectId") Integer projectId,
            @Param("status") String status,
            @Param("uploaderIds") List<Integer> uploaderIds);
}
