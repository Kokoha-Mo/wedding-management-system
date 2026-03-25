package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.Customer;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    // 🌟 用 email 找客戶
    Optional<Customer> findByEmail(String email);

    // 🌟 用電話找第一個客戶
    Optional<Customer> findFirstByTel(String tel);
}
