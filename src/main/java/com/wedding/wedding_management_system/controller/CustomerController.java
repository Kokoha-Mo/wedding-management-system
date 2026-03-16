package com.wedding.wedding_management_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.service.CustomerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {
    /*
     * 使用 Lombok 的 @RequiredArgsConstructor 來自動生成建構子，並注入
     * CustomerService。這樣就不需要手動寫建構子或使用 @Autowired。
     */
    private final CustomerService customerService;

    @GetMapping("/email/{email}")
    public ResponseEntity<Customer> getCustomerByEmail(@PathVariable String email) {
        Customer customer = customerService.findByEmail(email);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customer);
    }

    @PostMapping("/register")
    public ResponseEntity<Customer> register(@RequestBody Customer customer) {
        Customer savedCustomer = customerService.register(customer);
        return ResponseEntity.ok(savedCustomer);
    }
}
