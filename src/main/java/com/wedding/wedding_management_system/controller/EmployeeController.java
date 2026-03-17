package com.wedding.wedding_management_system.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import com.wedding.wedding_management_system.service.EmployeeService;

@RestController
@RequestMapping("/api/manager/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/email/{email}")
    public ResponseEntity<Employee> getEmployeeByEmail(@PathVariable("email") String email) {
        Employee employee = employeeService.findByEmail(email);
        if (employee == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<List<TaskDTO.AssigneeDTO>> getEmployeesByDept(@PathVariable("deptId") Integer deptId) {
        List<Employee> employees = employeeService.getEmployeesByDeptId(deptId);

        List<TaskDTO.AssigneeDTO> responseList = employees.stream().map(emp -> {
            TaskDTO.AssigneeDTO dto = new TaskDTO.AssigneeDTO();
            dto.setEmpId(emp.getId());
            dto.setName(emp.getName());
            dto.setDepartmentName(emp.getDepartment() != null ? emp.getDepartment().getDeptName() : "未知部門");
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);

    }
}
