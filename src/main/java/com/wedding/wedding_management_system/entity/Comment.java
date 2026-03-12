package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 留言表（comments）
 * 客戶或員工對預約/案件的留言
 */
@Entity
@Table(name = "comments")
@Data
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;                 // PK

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;                  // FK → books（隸屬哪筆預約）

    @ManyToOne
    @JoinColumn(name = "book_owner")
    private Customer bookOwner;         // FK → customers（留言者）

    private LocalDateTime date;         // 留言日期時間

    @Column(length = 1000)
    private String text;                // 留言內容
}
