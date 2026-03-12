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
 * 任務溝通記錄（task_communications）
 * 任務執行中的溝通訊息
 */
@Entity
@Table(name = "task_communications")
@Data
public class TaskCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer commId;             // PK

    @ManyToOne
    @JoinColumn(name = "task_id")
    private ProjectTask task;           // FK → projects_tasks

    @Column(name = "create_at", insertable = false, updatable = false,
            columnDefinition = "datetime default now()")
    private LocalDateTime createAt;     // 建立時間

    @Column(length = 500)
    private String content;             // 訊息內容
}
