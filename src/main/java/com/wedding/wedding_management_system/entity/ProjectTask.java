package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "projects_tasks")
@Data
public class ProjectTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    @Column(name = "update_at")
    private LocalDateTime updateAt; // 更新日期

    private LocalDate deadline; // 截止日期時間

    // 狀態：待開始/進行中/待審核/完成/其他
    @Column(length = 30)
    private String status;

    @Column(name = "manager_content", length = 500)
    private String managerContent; // 管理者說明/要求

    @Column(name = "task_response", length = 500)
    private String taskResponse; // 負責人回饋

    @JsonIgnore
    @OneToMany(mappedBy = "task")
    private List<TaskOwner> taskOwners;

    @JsonIgnore
    @OneToMany(mappedBy = "task")
    private List<TaskCommunication> communications;
}
