package com.wedding.wedding_management_system.service;

import com.wedding.wedding_management_system.entity.ProjectCommunication;
import com.wedding.wedding_management_system.repository.ProjectCommunicationRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

@Service
@EnableScheduling
public class UnreadMessageReminderService {

    @PostConstruct
    void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
    }

    @Autowired
    private ProjectCommunicationRepository communicationRepo;

    @Autowired
    private EmailService emailService;

    // 改：從記錄 comm_id 改成記錄 "projectId_日期"
    private final Set<String> alreadyRemindedToday = ConcurrentHashMap.newKeySet();

    // 每5分鐘巡邏一次
    // @Scheduled(fixedRate = 10*60*1000)
    @Scheduled(fixedRate = 60 * 1000) // 1分鐘巡邏一次（測試用）
    public void patrolUnreadMessages() {
        System.out.println("巡邏員啟動：檢查未回覆訊息...");

        // 撈 60～65 分鐘前，婚顧發的訊息，一天只發送一次
        // LocalDateTime end = LocalDateTime.now().minusMinutes(60);
        // LocalDateTime start = LocalDateTime.now().minusMinutes(70);

        // 撈 3～4 分鐘前的訊息（測試用）
        LocalDateTime end = LocalDateTime.now().minusMinutes(3);
        LocalDateTime start = LocalDateTime.now().minusMinutes(4);

        // 一次查詢：找出未回覆的婚顧訊息（已解決 N+1）
        List<ProjectCommunication> unrepliedMessages = communicationRepo.findUnrepliedManagerMessages("公司", start, end);

        // 依專案分組，只取最後一則
        Map<Integer, ProjectCommunication> lastMsgPerProject = new HashMap<>();
        for (ProjectCommunication msg : unrepliedMessages) {
            lastMsgPerProject.put(msg.getProject().getId(), msg);
        }

        for (ProjectCommunication msg : lastMsgPerProject.values()) {
            Integer projectId = msg.getProject().getId();
            String today = LocalDate.now().toString();
            String key = projectId + "_" + today;

            // 今天已寄過就跳過
            if (alreadyRemindedToday.contains(key))
                continue;

            String customerEmail = msg.getProject().getBook().getCustomer().getEmail();
            String customerName = msg.getProject().getBook().getCustomer().getName();

            emailService.sendUnreadMessageEmail(customerEmail, customerName);
            System.out.println("已寄信通知：" + customerName + "（" + customerEmail + "）");

            alreadyRemindedToday.add(key);
        }

    }

    // 每天半夜 00:00:00 自動清空暫存，防止記憶體洩漏 (Memory Leak)
    @Scheduled(cron = "0 0 0 * * ?")
    public void clearDailyReminders() {
        alreadyRemindedToday.clear();
        System.out.println("午夜系統維護：已清空今日提醒暫存紀錄");
    }
}