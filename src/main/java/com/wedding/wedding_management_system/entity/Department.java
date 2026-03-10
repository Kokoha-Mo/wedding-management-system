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
@Table(name = "departments")
@Data
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer dept_id;
    private String dept_name;
    
    @JsonIgnore
    @OneToMany(mappedBy = "department")
    private List<Employee> employees;
    
    @JsonIgnore
    @OneToMany(mappedBy = "department")
    private List<Category> categories;
}
