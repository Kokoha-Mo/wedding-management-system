package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.wedding_management_system.entity.Project;
import com.wedding.wedding_management_system.entity.ProjectCommunication;
import com.wedding.wedding_management_system.entity.ProjectCommunicationDocument;
import com.wedding.wedding_management_system.repository.ProjectCommunicationDocumentRepository;
import com.wedding.wedding_management_system.repository.ProjectCommunicationRepository;
import com.wedding.wedding_management_system.repository.ProjectRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectCommunicationService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectCommunicationRepository communicationRepository;

    @Autowired
    private ProjectCommunicationDocumentRepository pcdRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // 🌟 注入 WebSocket 訊息發送模板
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void addProjectCommunicationWithFiles(Integer projectId, String createBy, String content,
            List<MultipartFile> files) throws Exception {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("找不到專案 ID: " + projectId));

        // 1. 儲存溝通紀錄 (純文字部分)
        ProjectCommunication newComm = new ProjectCommunication();
        newComm.setProject(project);
        newComm.setCreateBy(createBy);
        newComm.setContent(content == null ? "" : content);
        newComm.setCreateAt(LocalDateTime.now());

        ProjectCommunication savedComm = communicationRepository.save(newComm);
        
        // 用來收集準備推播給前端的檔案資訊
        List<Map<String, String>> uploadedFilesInfo = new ArrayList<>();

        // 2. 處理檔案上傳
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty())
                    continue;

                // 🌟 核心：委託 FileStorageService 處理實體檔案寫入，我們只拿回傳的路徑！
                String savedFilePath = fileStorageService.storeFile(file, projectId);

                if (savedFilePath != null) {
                    // 只儲存到留言專屬附件表
                    ProjectCommunicationDocument pcd = new ProjectCommunicationDocument();
                    pcd.setCommunication(savedComm);
                    pcd.setName(file.getOriginalFilename());
                    pcd.setFilePath(savedFilePath);
                    pcd.setFileType(file.getContentType());

                    ProjectCommunicationDocument savedDoc = pcdRepository.save(pcd);
                    
                    // 🌟 收集檔案資訊供推播使用
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("name", savedDoc.getName());
                    fileInfo.put("filePath", savedDoc.getFilePath());
                    fileInfo.put("fileType", savedDoc.getFileType());
                    uploadedFilesInfo.add(fileInfo);
                }
            }
        }

        // 🌟 3. 核心：打包剛存好的最新訊息，透過 WebSocket 推播出去！
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("id", savedComm.getId());
        wsMessage.put("createBy", savedComm.getCreateBy());
        wsMessage.put("content", savedComm.getContent());
        wsMessage.put("createAt", savedComm.getCreateAt().toString());
        wsMessage.put("documents", uploadedFilesInfo);

        // 推播到專屬頻道 (前端只要訂閱這個路徑就能收到)
        String destination = "/topic/project/" + projectId;
        messagingTemplate.convertAndSend(destination, (Object) wsMessage);
        
        System.out.println("✅ 已透過 WebSocket 廣播新訊息至: " + destination);
    }
}