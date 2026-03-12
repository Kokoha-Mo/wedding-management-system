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

/**
 * 案件溝通附件（project_communications_documents）
 * 溝通訊息中的附加文件
 */
@Entity
@Table(name = "project_communications_documents")
@Data
public class ProjectCommunicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // PK

    @ManyToOne
    @JoinColumn(name = "comm_id")
    private ProjectCommunication communication; // FK → project_communications

    @Column(nullable = false)
    private String name; // 文件說明標題

    @Column(name = "file_path", length = 1000)
    private String filePath; // 文件路徑（nullable）

    @Column(name = "file_type", length = 50)
    private String fileType; // 文件類型（如 pdf/jpg/docx）
}
