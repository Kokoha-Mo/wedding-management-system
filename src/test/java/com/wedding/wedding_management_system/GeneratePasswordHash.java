package com.wedding.wedding_management_system;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/* 臨時工具 產生 BCrypt 雜湊值 */
public class GeneratePasswordHash {

    private static final String PLAIN_PASSWORD = "$2a$10$custpw001";

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashed = encoder.encode(PLAIN_PASSWORD);

        System.out.println("明碼密碼：" + PLAIN_PASSWORD);
        System.out.println("BCrypt 雜湊值：" + hashed);
    }
}
