package com.wedding.wedding_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.wedding.wedding_management_system.entity.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByEmail(String email);


}
