package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.wedding.wedding_management_system.dto.CustomerLoginDto;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.repository.CustomerRepository;
import com.wedding.wedding_management_system.util.JwtToken;

@Service
public class CustomerLoginService {
    @Autowired
    private CustomerRepository customerRepository;

    public String login(CustomerLoginDto dto) {
        Customer customer = customerRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("查無此帳號"));

        // 目前資料庫是明碼，先用 equals 比對測試
        if (!customer.getPassword().equals(dto.getPassword())) {
            throw new RuntimeException("密碼錯誤");
        }

        return JwtToken.createToken(customer.getEmail());
    }
}
