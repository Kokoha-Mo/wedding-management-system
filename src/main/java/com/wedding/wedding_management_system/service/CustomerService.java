package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.repository.CustomerRepository;

@Service
public class CustomerService {
    @Autowired
    private CustomerRepository custemorRepository;

    public Customer findByEmail(String email) {
        System.out.println(email);
        return custemorRepository.findByEmail(email).orElse(null);
    }

    public Customer register(Customer customer) {
        return custemorRepository.save(customer);
    }
}
