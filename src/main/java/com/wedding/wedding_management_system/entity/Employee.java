package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
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
    @Column(name = "emp_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String tel;

    @Column(length = 255, unique = true)
    private String email;

    @Column(length = 20)
    private String role; // MANAGER/STAFF

    @Column(name = "year_exp", length = 20)
    private String yearExp;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 255)
    private String styles;

    @Column(name = "img_path", length = 1000)
    private String imgPath;

    @JsonIgnore
    @OneToMany(mappedBy = "manager")
    private List<Book> managedBooks;

    @JsonIgnore
    @OneToMany(mappedBy = "uploadedBy")
    private List<Document> uploadedDocuments;

    @JsonIgnore
    @OneToMany(mappedBy = "employee")
    private List<TaskOwner> taskOwners;

    @JsonIgnore
    @OneToMany(mappedBy = "createBy")
    private List<TaskCommunication> taskCommunications;
}
