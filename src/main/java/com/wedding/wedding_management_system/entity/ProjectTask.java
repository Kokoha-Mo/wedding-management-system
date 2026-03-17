package com.wedding.wedding_management_system.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "projects_tasks")
@Data
public class ProjectTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    private LocalDateTime deadline;

    @Column(length = 20)
    private String status;

    @Column(name = "manager_content", length = 500)
    private String managerContent;

    @JsonIgnore
    @OneToMany(mappedBy = "task")
    private List<TaskOwner> taskOwners;

    @JsonIgnore
    @OneToMany(mappedBy = "task")
    private List<TaskCommunication> communications;
}
