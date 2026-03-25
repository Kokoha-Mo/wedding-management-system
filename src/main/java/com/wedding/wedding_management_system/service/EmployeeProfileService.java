package com.wedding.wedding_management_system.service;

import com.wedding.wedding_management_system.dto.EmployeeProfileDTO;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class EmployeeProfileService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String AVATAR_DIR = "uploads/avatars/";

    public EmployeeProfileDTO getProfile(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        return EmployeeProfileDTO.builder()
                .empId(employee.getId())
                .name(employee.getName())
                .role(employee.getRole())
                .email(employee.getEmail())
                .tel(employee.getTel())
                .imgPath(employee.getImgPath())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getDeptName() : "無部門")
                .build();
    }

    @Transactional
    public void updateProfile(String email, EmployeeProfileDTO dto) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // 只更新允許修改的欄位
        employee.setTel(dto.getTel());
        employee.setEmail(dto.getEmail()); // 這裡假設 email 也可以改，但通常 email 是帳號，改了會影響登入。
        // 如果 email 是領證帳號且不允許修改，應移除上一行。
        
        employeeRepository.save(employee);
    }

    @Transactional
    public String updateAvatar(String email, MultipartFile file) throws IOException {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // 1. 處理舊檔刪除 (如果不是預設圖片)
        String oldPathStr = employee.getImgPath();
        if (oldPathStr != null && !oldPathStr.isEmpty() && !oldPathStr.contains("smile.jpg")) {
            try {
                Path oldPath = Paths.get(oldPathStr);
                Files.deleteIfExists(oldPath);
            } catch (Exception e) {
                System.err.println("刪除舊頭像失敗: " + e.getMessage());
            }
        }

        // 2. 儲存新檔
        Path uploadPath = Paths.get(AVATAR_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        String savedPath = AVATAR_DIR + fileName;
        employee.setImgPath(savedPath);
        employeeRepository.save(employee);
        
        return savedPath;
    }

    @Transactional
    public void updateAvatarUrl(String email, String url) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // 如果原本是本地檔案，則嘗試刪除
        String oldPathStr = employee.getImgPath();
        if (oldPathStr != null && !oldPathStr.isEmpty() && !oldPathStr.startsWith("http") && !oldPathStr.contains("smile.jpg")) {
            try {
                Path oldPath = Paths.get(oldPathStr);
                Files.deleteIfExists(oldPath);
            } catch (Exception e) {
                System.err.println("刪除舊頭像失敗: " + e.getMessage());
            }
        }

        employee.setImgPath(url);
        employeeRepository.save(employee);
    }

    @Transactional
    public boolean changePassword(String email, String oldPassword, String newPassword) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
            return false;
        }

        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);
        return true;
    }
}
