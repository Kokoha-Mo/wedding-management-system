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

@Entity
@Table(name = "consultation")
@Data
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consultation_id")
    private Integer id;

    @Column(length = 50)
    private String name;

    @Column(length = 30)
    private String tel;

    @Column(length = 100)
    private String email;

    @Column(name = "line_id", length = 50)
    private String lineId;

    @Column(name = "wedding_date")
    private LocalDate weddingDate;

    @Column(name = "guest_scale", length = 50)
    private String guestScale;

    @Column(length = 200)
    private String styles;

    @Column(length = 200)
    private String services;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(name = "consultation_date")
    private LocalDate consultationDate;

    @Column(name = "preferred_time", length = 20)
    private String preferredTime;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 20)
    private String status;
}
