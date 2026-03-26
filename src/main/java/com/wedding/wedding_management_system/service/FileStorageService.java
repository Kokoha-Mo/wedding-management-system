package com.wedding.wedding_management_system.service;

import org.springframework.web.multipart.MultipartFile;

// 🌟 這是一個介面，定義了「存檔案」的標準動作
public interface FileStorageService {
    
    // 規定所有實作這個介面的類別，都必須實作這個方法
    String storeFile(MultipartFile file, Integer projectId) throws Exception;
    
}