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

    @Column(name = "id_number", length = 10)
    private String idNumber; // 身分證字號 — ENUM/VARCHAR

    @Column(name = "line_id", length = 50)
    private String lineId; // Line 聯絡方式

    @Column(length = 20, columnDefinition = "varchar(20) default 'staff'")
    private String role; // 角色（admin / staff）

    @Column(name = "hire_no", length = 20)
    private String hireNo; // 入職編號（員工編號）

    @Column(length = 255)
    private String status; // 在職狀態/備注

    @Column(name = "img_path", length = 1000)
    private String imgPath; // 員工照片路徑

    @JsonIgnore
    @OneToMany(mappedBy = "manager")
    private List<Book> managedBooks;

    @JsonIgnore
    @OneToMany(mappedBy = "uploadedBy")
    private List<Document> uploadedDocuments;
}
