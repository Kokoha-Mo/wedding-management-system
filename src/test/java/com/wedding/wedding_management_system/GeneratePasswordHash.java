package com.wedding.wedding_management_system;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 【臨時工具】用來產生 BCrypt 雜湊值
 * 步驟：
 * 1. 把 PLAIN_PASSWORD 換成你資料庫裡的明碼密碼
 * 2. 用 JUnit 執行這個 Class 的 main() 方法（右鍵 → Run As → Java Application）
 * 3. 複製 Console 印出的雜湊值，貼到資料庫的 password 欄位
 * 4. 測試完成後可以刪掉這個檔案
 */
public class GeneratePasswordHash {

    // ✏️ 把這裡換成你資料庫裡的實際明碼密碼
    private static final String PLAIN_PASSWORD = "$2a$10$custpw001";

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashed = encoder.encode(PLAIN_PASSWORD);

        System.out.println("明碼密碼：" + PLAIN_PASSWORD);
        System.out.println("BCrypt 雜湊值：" + hashed);
    }
}
