package com.wedding.wedding_management_system.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.repository.EmployeeRepository;

@Configuration
public class DataSetForTest {

    @Bean
    CommandLineRunner initTestUser(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // 檢查是否已經有這個測試帳號，沒有的話就幫你建一個
            if (employeeRepository.findByEmail("pm@test.com").isEmpty()) {
                Employee pm = new Employee();
                pm.setEmail("pm@test.com");
                // 這裡一定要透過 encoder 加密，否則 Security 會不認得！
                pm.setPassword(passwordEncoder.encode("123456"));
                pm.setName("測試 PM Joy");
                pm.setRole("MANAGER"); // 對應你的 SecurityConfig 權限

                employeeRepository.save(pm);
                System.out.println("==========================================================");
                System.out.println("✅ 測試 PM 帳號建立完成！");
                System.out.println("✅ 帳號：pm@test.com");
                System.out.println("✅ 密碼：123456");
                System.out.println("==========================================================");
            }
        };
    }
}