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

@Entity
@Table(name = "consultation")
@Data
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = true)
    private Customer customer;

    @Column(name = "client_name")
    private String clientName;          // 姓名（未登入時使用）
    private String phone;               // 電話
    private String email;               // 電子信箱
    @Column(name = "line_id")
    private String lineId;              // Line 聯絡方式
    @Column(name = "wedding_date")
    private LocalDate weddingDate;      // 婚禮日期
    @Column(name = "guest_scale")
    private String guestScale;          // 賓客規模
    private String styles;              // 婚禮風格

    @Column(length = 1000)
    private String services;            // 需要的服務

    @Column(name = "preferred_time", length = 100)
    private String preferredTime;       // 偏好時間

    @Column(length = 100)
    private String budget;              // 預算

    @Column(name = "pollfend_nota", columnDefinition = "text")
    private String pollfendNota;        // 附加需求

    @Column(length = 20, columnDefinition = "varchar(20) default '待處理'")
    private String status;              // 狀態：待處理/已回覆/已完成

    @Column(name = "created_at", insertable = false, updatable = false,
            columnDefinition = "datetime default now()")
    private LocalDateTime createdAt;
}
