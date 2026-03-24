package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "books")
@Data
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "create_at", insertable = false, updatable = false)
    private LocalDateTime createAt;

    @Column(name = "wedding_date")
    private LocalDate weddingDate;

    @Column(name = "guest_scale")
    private Integer guestScale;

    @Column(length = 100)
    private String place;

    @Column(length = 30)
    private String styles;

    @Column(length = 20)
    private String status;

    @Column(length = 1000)
    private String content;

    @LastModifiedDate
    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @JsonIgnore
    @OneToMany(mappedBy = "book")
    private List<BookDetail> bookDetails;

    @JsonIgnore
    @OneToOne(mappedBy = "book")
    private Project project;
}
