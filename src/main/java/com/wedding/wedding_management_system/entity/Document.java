package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Table(name = "documents")
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private Employee uploadedBy;        // 上傳者（員工）

    @Column(nullable = false)
    private String name;                // 文件說明標題

    @Column(name = "content_by", length = 255)
    private String contentBy;           // 文字說明

    @Column(name = "file_path", length = 1000, nullable = false)
    private String filePath;            // 文件路徑

    @Column(name = "file_type", length = 50)
    private String fileType;            // 文件類型

    // 狀態：審核中/已批准/已拒絕/其他
    @Column(length = 30, columnDefinition = "varchar(30) default '審核中'")
    private String status;
}
