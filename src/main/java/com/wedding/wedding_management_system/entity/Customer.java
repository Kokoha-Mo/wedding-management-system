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
    private Integer id;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String cname;   // 公司名稱

    @Column(nullable = false)
    private String name;    // 聯絡人

    private String email;   // 電子信箱

    private String address; // 地址

    @Column(columnDefinition = "varchar(20)")
    private String tel;     // 電話（含 0 結頭）

    @Column(name = "line_id", length = 50)
    private String lineId;  // Line 聯絡方式

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    private List<Book> books;

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    private List<Consultation> consultations;
}
