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
    private Employee uploadedBy;

    @Column(length = 255)
    private String name;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    // TODO: 之後要將 file_type 欄位長度擴充到 255，因為有些檔案類型可能會比較長
    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(length = 20)
    private String status;
}
