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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "projects")
@Data
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Integer id;

    @OneToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "create_at", insertable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @Column(name = "total_payment")
    private Integer totalPayment;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @Column(length = 20)
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
