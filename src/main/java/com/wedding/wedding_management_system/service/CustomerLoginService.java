package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.wedding.wedding_management_system.dto.CustomerLoginDto;
import com.wedding.wedding_management_system.dto.CustomerLoginResponseDto;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.repository.BookRepository;
import com.wedding.wedding_management_system.repository.CustomerRepository;
import com.wedding.wedding_management_system.repository.ProjectRepository;
import com.wedding.wedding_management_system.util.JwtToken;

@Service
public class CustomerLoginService {

    private final PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ProjectRepository projectRepository;

    CustomerLoginService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public CustomerLoginResponseDto login(CustomerLoginDto dto) {

        Customer customer = customerRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("帳號或密碼錯誤！"));

        if (!passwordEncoder.matches(dto.getPassword(), customer.getPassword())) {
            throw new RuntimeException("帳號或密碼錯誤！");
        }

        // 檢查帳號是否已被停用（book status 為「取消」）
        disableAccount(customer.getEmail());

        CustomerLoginResponseDto result = new CustomerLoginResponseDto();
        result.setToken(JwtToken.createToken(customer.getEmail()));

        // 把客人的 ID 放進 DTO 裡
        result.setCustomerId(customer.getId());

        result.setEmail(customer.getEmail());
        result.setName(customer.getName());

        // 新增：查詢該客戶是否有專案，並存入 DTO
        boolean hasProject = projectRepository.existsByBook_Customer_Id(customer.getId());
        result.setHasProject(hasProject);

        return result;
    }

    // 給「登入後強制修改密碼」使用的方法
    public void updatePasswordAfterLogin(String email, String newPassword) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("請完整填寫電子郵件與密碼"));

        // 1. 更新為新密碼 (記得要經過 BCrypt 加密)
        customer.setPassword(passwordEncoder.encode(newPassword));
        // 2. 解除強制修改的鎖定狀態
        customer.setResetToken(null);

        customerRepository.save(customer);
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
                .orElseThrow(() -> new RuntimeException("請完整填寫電子郵件與密碼"));

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
                .orElseThrow(() -> new RuntimeException("請完整填寫電子郵件與密碼"));

        // 1. BCrypt 加密新密碼存入
        customer.setPassword(passwordEncoder.encode(newPassword));

        // 2. 清掉 reset_token → 這個 token 永遠失效
        customer.setResetToken(null);

        customerRepository.save(customer);
    }

    // ── 產生並儲存重設密碼 Token（含 60 秒頻率限制） ──
    public String generateAndSaveResetToken(String email) {
        // 1. 確認這信箱是不是我們的客人
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("查無此電子郵件"));

        // 亮點：頻率限制檢查 (Rate Limiting)
        // 取得資料庫中現有的 Token
        String oldToken = customer.getResetToken();

        // 如果舊 Token 存在且還有效，就檢查它的簽發時間
        if (oldToken != null && JwtToken.isValid(oldToken)) {
            // 解析 JWT 裡面的 iat (Issued At) 欄位
            long issuedAt = JwtToken.getIssuedAt(oldToken).getTime();
            long now = System.currentTimeMillis();

            // 檢查是否小於 60 秒 (60,000 毫秒)
            if (now - issuedAt < 60 * 1000) {
                throw new RuntimeException("請求過於頻繁，請於 60 秒後再試");
            }
        }

        // 2. 產生成新的專屬 Token
        String token = JwtToken.createResetToken(email);

        // 3. 把新 Token 存入資料庫，這會覆蓋掉舊的
        customer.setResetToken(token);
        customerRepository.save(customer);

        return token;
    }

    // ── 停用帳號檢查 ──
    // 若該客戶有任何一筆預約的 status 為「取消」，則拋出例外，禁止登入
    public void disableAccount(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("請完整填寫電子郵件與密碼"));

        boolean isCancelled = bookRepository.findByCustomer(customer)
                .stream()
                .anyMatch(book -> "取消".equals(book.getStatus()));

        if (isCancelled) {
            throw new RuntimeException("此帳號已被停用，如有疑問請聯繫客服");
        }
    }
}