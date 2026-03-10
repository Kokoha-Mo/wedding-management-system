package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.repo.CustemorRepo;

@Service
public class CustomerService {
    @Autowired
    private CustemorRepo custemorRepo;

    public Customer findByEmail(String email) {
        System.out.println(email);
        return custemorRepo.findByEmail(email).orElse(null);
    }

    public Customer register(Customer customer) {
        return custemorRepo.save(customer);
    }
}
