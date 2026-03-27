package com.wedding.wedding_management_system.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

// 負責「把檔案上傳到 Google Cloud Storage」並「回傳公開網址」的 Service
@Service
@Profile("!local") // 🌟 關鍵 ：只要非本地環境就會啟用這個 Bean
public class GcsFileStorageServiceImpl implements FileStorageService {

    // 只需要在 application.properties 裡面設定 gcp.bucket.name 即可
    @Value("${gcp.bucket.name}")
    private String bucketName;

    // 修正 1：改名為 gcpProjectId (String 型態)，避免跟底下的參數撞名
    @Value("${gcp.project.id}")
    private String gcpProjectId;

    /**
     * 將上傳的檔案存入 GCS 指定的專案虛擬資料夾，並回傳公開讀取網址
     */
    @Override
    public String storeFile(MultipartFile file, Integer projectId) throws Exception {
        if (file.isEmpty()) {
            return null;
        }

        // 修正 2：明確傳入 gcpProjectId (String) 給 Google 認證
        Storage storage = StorageOptions.newBuilder()
                .setProjectId(gcpProjectId)
                .build()
                .getService();

        // 2. 產生獨一無二的檔名避免覆蓋
        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        // 這裡使用的是傳入的 projectId (Integer)，代表你們系統中的婚禮專案編號
        String gcsObjectName = "projects/communications_documents/" + projectId + "/" + uniqueFileName;

        // 4. 設定上傳目標與檔案類型
        BlobId blobId = BlobId.of(bucketName, gcsObjectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        // 5. 執行實體檔案上傳至雲端
        storage.create(blobInfo, file.getBytes());

        // 6. 回傳可供前端直接 <img src="..."> 顯示的公開網址
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, gcsObjectName);
    }
}
