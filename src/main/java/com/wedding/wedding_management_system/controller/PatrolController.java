package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.service.UnreadMessageReminderService;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/patrol")
public class PatrolController {

    @Autowired
    private UnreadMessageReminderService reminderService;

    // 給 GCP Cloud Scheduler 呼叫的 API
    @GetMapping("/execute")
    public ResponseEntity<String> executePatrol() {
        reminderService.patrolUnreadMessages();
        return ResponseEntity.ok("巡邏員已成功執行未讀訊息檢查！");
    }
}