package com.wedding.wedding_management_system.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.wedding_management_system.entity.Customer;

public interface CustemorRepo extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByEmail(String email);
}
