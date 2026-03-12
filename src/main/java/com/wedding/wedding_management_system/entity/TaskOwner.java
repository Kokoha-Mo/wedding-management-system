package com.wedding.wedding_management_system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

/**
 * 任務負責人（task_owner）
 * ProjectTask 與 Employee 的多對多中間表
 */
@Entity
@Table(name = "task_owner")
@Data
public class TaskOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ownerId;            // PK

    @ManyToOne
    @JoinColumn(name = "task_id")
    private ProjectTask task;           // FK → projects_tasks

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;          // FK → employees
}
