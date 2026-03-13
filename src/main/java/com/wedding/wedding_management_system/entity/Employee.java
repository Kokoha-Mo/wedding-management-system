package com.wedding.wedding_management_system.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

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

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name; // 姓名

    @Column(length = 10)
    private String tel; // 電話

    private String email; // 電子信箱

    @Column(length = 20, columnDefinition = "varchar(20) default 'staff'")
    private String role; // 角色（admin / staff）

    @Column(name = "img_path", length = 1000)
    private String imgPath; // 員工照片路徑

    @JsonIgnore
    @OneToMany(mappedBy = "manager")
    private List<Book> managedBooks;

    @JsonIgnore
    @OneToMany(mappedBy = "uploadedBy")
    private List<Document> uploadedDocuments;
}
