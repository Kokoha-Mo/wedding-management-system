package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "customers")
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer id;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255, unique = true)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "line_id", length = 50)
    private String lineId;

    @Column(length = 20)
    private String tel;

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    private List<Book> books;
}
