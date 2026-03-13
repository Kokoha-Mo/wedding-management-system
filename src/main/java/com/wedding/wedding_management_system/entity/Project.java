package com.wedding.wedding_management_system.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "projects")
@Data
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "create_at", insertable = false, updatable = false, columnDefinition = "datetime default now()")
    private LocalDateTime createAt; // 建立日期時間

    @Column(name = "update_at")
    private LocalDateTime updateAt; // 更新日期（date）

    @Column(name = "total_payment")
    private Integer totalPayment; // 總金額

    // 付款狀態
    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    // 進度狀態
    @Column(length = 50)
    private String status;

    @JsonIgnore
    @OneToMany(mappedBy = "project")
    private List<ProjectTask> projectTasks;

    @JsonIgnore
    @OneToMany(mappedBy = "project")
    private List<Document> documents;

    @JsonIgnore
    @OneToMany(mappedBy = "project")
    private List<ProjectCommunication> communications;
}
