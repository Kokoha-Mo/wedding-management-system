package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.wedding.wedding_management_system.dto.CustomerLoginDto;
import com.wedding.wedding_management_system.dto.CustomerLoginResponseDto;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.repository.CustomerRepository;
import com.wedding.wedding_management_system.util.JwtToken;

@Service
public class CustomerLoginService {

    private final PasswordEncoder passwordEncoder;
    @Autowired
    private CustomerRepository customerRepository;

    CustomerLoginService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public CustomerLoginResponseDto login(CustomerLoginDto dto) {

        Customer customer = customerRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("查無此帳號"));

        // 明碼比對
        // if (!customer.getPassword().equals(dto.getPassword())) {
        // throw new RuntimeException("密碼錯誤");
        // }

        // 雜湊比對
        if (!passwordEncoder.matches(dto.getPassword(), customer.getPassword())) {
            throw new RuntimeException("密碼錯誤");
        }

        // 組裝 DTO（包含 token、email、name）
        CustomerLoginResponseDto result = new CustomerLoginResponseDto();
        result.setToken(JwtToken.createToken(customer.getEmail())); // token 給 Controller 設定 Cookie 用
        result.setEmail(customer.getEmail());
        result.setName(customer.getName());

        return result;
    }

    // ── 驗證重設密碼 token ──
    public void verifyResetToken(String token) {

        // 1. JWT 本身有沒有過期
        if (!JwtToken.isValid(token)) {
            throw new RuntimeException("連結已過期");
        }

        // 2. 是不是重設密碼專用的 token
        if (!JwtToken.isResetToken(token)) {
            throw new RuntimeException("無效的連結");
        }

        // 3. DB 裡有沒有這個 token（有沒有被用過）
        String email = JwtToken.getEmail(token);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("查無此帳號"));

        if (!token.equals(customer.getResetToken())) {
            throw new RuntimeException("連結已失效或已使用過");
        }
    }

    // ── 重設密碼 ──
    public void resetPassword(String token, String newPassword) {

        // 再驗證一次（防止有人直接打 API 跳過前端驗證）
        verifyResetToken(token);

        String email = JwtToken.getEmail(token);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("查無此帳號"));

        // 1. BCrypt 加密新密碼存入
        customer.setPassword(passwordEncoder.encode(newPassword));

        // 2. 清掉 reset_token → 這個 token 永遠失效
        customer.setResetToken(null);

        customerRepository.save(customer);
    }

    // ── 產生並儲存重設密碼 Token ──
    public String generateAndSaveResetToken(String email) {
        // 1. 確認這信箱是不是我們的客人
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("查無此帳號"));

        // 2. 產生一組專屬 Token
        String token = JwtToken.createResetToken(email);

        // 3. 把 Token 存入該位客人的資料庫欄位中
        customer.setResetToken(token);
        customerRepository.save(customer);

        return token;
    }
}
