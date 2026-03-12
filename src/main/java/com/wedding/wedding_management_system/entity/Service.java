package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.persistence.OneToMany;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "services")
@Data
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;                // 服務名稱（NOT NULL）

    private Integer price;              // 單價

    @Column(columnDefinition = "text")
    private String content;             // 服務說明

    @Column(name = "estimated_days")
    private Integer estimatedDays;      // 估計天數

    @Column(name = "ceremony_date")
    private LocalDate ceremonyDate;     // 典禮日期

    @Column(name = "phone", length = 10)
    private String phone;               // 電話（nullable）

    @Column(name = "contact", length = 255)
    private String contact;             // 聯絡方式（nullable）

    @JsonIgnore
    @OneToMany(mappedBy = "service")
    private List<BookDetail> bookDetails;

    @JsonIgnore
    @OneToMany(mappedBy = "service")
    private List<ProjectTask> projectTasks;
}
