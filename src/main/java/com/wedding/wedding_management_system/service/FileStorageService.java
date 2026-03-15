package com.wedding.wedding_management_system.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// 負責「把檔案寫進電腦裡」並「回傳檔案路徑」的 Service
@Service
public class FileStorageService {

    /**
     * 將上傳的檔案存入指定的專案資料夾，並回傳儲存的路徑
     */
    public String storeFile(MultipartFile file, Integer projectId) throws Exception {
        if (file.isEmpty()) {
            return null;
        }

        // 1. 設定存檔資料夾
        String uploadDir = "uploads/projects/communications_documents/" + projectId + "/";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. 產生獨一無二的檔名避免覆蓋
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString() + extension;

        // 3. 實體檔案寫入硬碟
        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath);

        // 4. 回傳相對路徑供資料庫儲存
        return "/" + uploadDir + newFileName;
    }
}