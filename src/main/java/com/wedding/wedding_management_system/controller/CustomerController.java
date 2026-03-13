package com.wedding.wedding_management_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.wedding_management_system.dto.ProjectProgressDTO;
import com.wedding.wedding_management_system.entity.Customer;
import com.wedding.wedding_management_system.service.CustomerService;
import com.wedding.wedding_management_system.service.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 開發階段允許跨域
public class CustomerController {
    // 使用 Lombok 的 @RequiredArgsConstructor 來自動生成建構子，並注入 CustomerService 和
    // ProjectService
    private final CustomerService customerService;

    private final ProjectService projectService;

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

    /**
     * 取得客戶專案的籌備進度與溝通留言
     * 對應前端：新人客戶端 customer_progress.html
     * 測試網址：GET http://localhost:8080/api/customers/projects/{projectId}/progress
     */
    @GetMapping("/projects/{projectId}/progress")
    public ResponseEntity<ProjectProgressDTO> getCustomerProjectProgress(@PathVariable Integer projectId) {
        try {
            // 呼叫同一個 Service，這就是共用商業邏輯的優雅之處！
            ProjectProgressDTO progress = projectService.getProjectProgress(projectId);
            return ResponseEntity.ok(progress);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // 找不到專案時回傳 404
        }
    }
}
