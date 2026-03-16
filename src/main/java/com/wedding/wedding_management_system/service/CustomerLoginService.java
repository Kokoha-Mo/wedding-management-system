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

        if (!passwordEncoder.matches(dto.getPassword(), customer.getPassword())) {
            throw new RuntimeException("密碼錯誤");
        }

        // 3. 組裝 DTO（包含 token、email、name）
        CustomerLoginResponseDto result = new CustomerLoginResponseDto();
        result.setToken(JwtToken.createToken(customer.getEmail())); // token 給 Controller 設定 Cookie 用
        result.setEmail(customer.getEmail());
        result.setName(customer.getName());

        return result;
    }
}
