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
@Table(name = "project_communications_documents")
@Data
public class ProjectCommunicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "comm_id")
    private ProjectCommunication communication;

    @Column(length = 255)
    private String name;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_type", length = 50)
    private String fileType;
}
