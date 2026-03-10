package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "employees")
@Data
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer emp_id;
    
    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;
    
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;
    
    private String password;
    private String name;
    private String tel;
    private String email;
    private String id_number;
    private String role;
    
    @JsonIgnore
    @OneToMany(mappedBy = "manager")
    private List<Book> managedBooks;
}
