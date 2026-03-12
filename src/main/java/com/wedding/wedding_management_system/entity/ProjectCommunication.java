package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 案件溝通記錄（project_communications）
 * 記錄專案進行中的溝通訊息
 */
@Entity
@Table(name = "project_communications")
@Data
public class ProjectCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // PK

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project; // FK → projects

    @Column(name = "create_by", length = 50)
    private String createBy; // 建立者（員工/客戶名稱）

    @Column(name = "create_at", insertable = false, updatable = false, columnDefinition = "datetime default now()")
    private LocalDateTime createAt; // 建立時間

    @Column(length = 500)
    private String content; // 溝通內容

    @JsonIgnore
    @OneToMany(mappedBy = "communication")
    private List<ProjectCommunicationDocument> documents;
}
