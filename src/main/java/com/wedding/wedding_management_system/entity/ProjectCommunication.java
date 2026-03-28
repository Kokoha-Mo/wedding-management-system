package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
// 加上複合索引，優化查詢效能
@Table(name = "project_communications", indexes = {
        @Index(name = "idx_sender_time", columnList = "create_by, create_at")
})
@Data
public class ProjectCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comm_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    // 改成純字串對應，保留原欄位名稱 create_by
    @Column(name = "create_by")
    private String createBy;

    @Column(name = "create_at", insertable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(length = 500)
    private String content;

    @JsonIgnore
    @OneToMany(mappedBy = "communication")
    private List<ProjectCommunicationDocument> documents;
}
