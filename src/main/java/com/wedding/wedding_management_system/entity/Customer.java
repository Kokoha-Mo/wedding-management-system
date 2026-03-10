package com.wedding.wedding_management_system.entity;

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
    private Integer id;
    private String password;
    private String cname;
    private String name;
    private String email;
    private String address;
    private String tel;

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    private List<Book> books;

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    private List<Consultation> consultations;
}
