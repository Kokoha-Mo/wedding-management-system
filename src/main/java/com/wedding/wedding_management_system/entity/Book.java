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
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "books")
@Data
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bookId;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "create_at", insertable = false, updatable = false,
            columnDefinition = "datetime default now()")
    private LocalDateTime createAt;     // 建立日期

    @Column(name = "wedding_date")
    private LocalDate weddingDate;      // 婚禮日期

    @Column(name = "guest_scale")
    private Integer guestScale;         // 賓客規模（人數）

    @Column(length = 10)
    private String styles;              // 婚禮風格

    @Column(name = "place")
    private String weddingplace;        // 婚禮地點

    @Column(length = 100)
    private String status;              // 狀態

    @Column(length = 1000)
    private String content;             // 備註

    @JsonIgnore
    @OneToMany(mappedBy = "book")
    private List<BookDetail> bookDetails;

    @JsonIgnore
    @OneToOne(mappedBy = "book")
    private Project project;
}
