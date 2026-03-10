package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "projects")
@Data
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer project_id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;
    
    @Column(insertable = false, updatable = false)
    private LocalDate create_at;
    private LocalDate update_at;
    private Integer total_payment;
    private String payment_status;
    private String status;
    
    @JsonIgnore
    @OneToMany(mappedBy = "project")
    private List<ProjectTask> projectTasks;
    
    @JsonIgnore
    @OneToMany(mappedBy = "project")
    private List<Document> documents;
}
