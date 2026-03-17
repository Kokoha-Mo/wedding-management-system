package com.wedding.wedding_management_system.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.repository.EmployeeRepository;

@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;

    public Employee findByEmail(String email) {
        return employeeRepository.findByEmail(email).orElse(null);
    }

    public List<Employee> getEmployeesByDeptId(Integer deptId) {
        return employeeRepository.findByDepartment_Id(deptId);
    }
}
