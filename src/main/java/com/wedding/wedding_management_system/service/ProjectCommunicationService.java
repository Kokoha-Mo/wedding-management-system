package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;

@Service
public class ProjectCommunicationService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectCommunicationRepository communicationRepository;

    @Autowired
    private ProjectCommunicationDocumentRepository pcdRepository;

    // 🌟 注入我們剛剛寫好的檔案處理小幫手
    @Autowired
    private FileStorageService fileStorageService;

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

        // 2. 處理檔案上傳
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty())
                    continue;

                // 🌟 核心：委託 FileStorageService 處理實體檔案寫入，我們只拿回傳的路徑！
                String savedFilePath = fileStorageService.storeFile(file, projectId);

                if (savedFilePath != null) {
                    // 3. 只儲存到留言專屬附件表
                    ProjectCommunicationDocument pcd = new ProjectCommunicationDocument();
                    pcd.setCommunication(savedComm);
                    pcd.setName(file.getOriginalFilename());
                    pcd.setFilePath(savedFilePath);
                    pcd.setFileType(file.getContentType());

                    pcdRepository.save(pcd);
                }
            }
        }
    }
}