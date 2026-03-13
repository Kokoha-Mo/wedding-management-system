package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "consultation")
@Data
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consultation_id")
    private Integer id; // 諮詢單ID

    private String name; // 顧客姓名
    private String phone; // 連絡電話
    private String email; // Email
    @Column(name = "line_id")
    private String lineId; // Line ID

    @Column(name = "consultation_date")
    private LocalDate consultationDate; // 期望諮詢日期
    @Column(name = "preferred_time", length = 20)
    private String preferredTime; // 期望時段

    @Column(name = "wedding_date")
    private LocalDate weddingDate; // 預定婚期
    @Column(name = "guest_scale")
    private String guestScale; // 賓客規模

    private String styles; // 嚮往婚禮風格
    @Column(length = 200)
    private String services; // 協助服務項目

    @Column(name = "additional_notes", columnDefinition = "text")
    private String additionalNotes; // 其他備註細節

    @Column(length = 20, columnDefinition = "varchar(20) default '待處理'")
    private String status; // 狀態：待處理/已聯絡/轉預約/無效單

    @Column(name = "created_at", columnDefinition = "datetime default current_timestamp")
    private java.time.LocalDateTime createdAt; // 建立時間
}
