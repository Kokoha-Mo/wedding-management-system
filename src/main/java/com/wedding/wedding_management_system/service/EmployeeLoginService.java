package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.wedding.wedding_management_system.dto.EmpLoginDto;
import com.wedding.wedding_management_system.dto.EmployeeLoginResponseDto;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import com.wedding.wedding_management_system.util.JwtToken;

@Service
public class EmployeeLoginService {

    private final PasswordEncoder passwordEncoder;

    @Autowired
    private EmployeeRepository employeeRepository;

    public EmployeeLoginService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public EmployeeLoginResponseDto login(EmpLoginDto dto) {
        // Query by email using the incoming empEmail
        Employee employee = employeeRepository.findByEmail(dto.getEmpEmail())
                .orElseThrow(() -> new RuntimeException("查無此帳號"));

        // Match encrypted password
        if (!passwordEncoder.matches(dto.getEmpPassword(), employee.getPassword())) {
            throw new RuntimeException("密碼錯誤");
        }

        EmployeeLoginResponseDto result = new EmployeeLoginResponseDto();
        result.setToken(JwtToken.createToken(employee.getEmail()));
        result.setEmpId(employee.getId());
        result.setEmpName(employee.getName());
        result.setPosition(employee.getRole()); // Assumed 'role' serves as 'position'

        if (employee.getDepartment() != null) {
            result.setDeptId(employee.getDepartment().getId());
        }

        return result;
    }
}
