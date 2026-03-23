package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.dto.EmployeeProfileDTO;
import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.service.EmployeeProfileService;
import com.wedding.wedding_management_system.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeProfileService employeeProfileService;

    @GetMapping("/profile")
    public ResponseEntity<EmployeeProfileDTO> getProfile(Principal principal) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(employeeProfileService.getProfile(principal.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(Principal principal, @RequestBody EmployeeProfileDTO dto) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        employeeProfileService.updateProfile(principal.getName(), dto);
        return ResponseEntity.ok(Map.of("message", "個人資料已更新"));
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(Principal principal,
            @RequestParam("file") MultipartFile file) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            String path = employeeProfileService.updateAvatar(principal.getName(), file);
            return ResponseEntity.ok(Map.of("message", "頭像上傳成功", "path", path));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "頭像上傳失敗"));
        }
    }

    @PutMapping("/profile/password")
    public ResponseEntity<Map<String, String>> changePassword(Principal principal,
            @RequestBody Map<String, String> passwords) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String oldPw = passwords.get("oldPassword");
        String newPw = passwords.get("newPassword");

        boolean success = employeeProfileService.changePassword(principal.getName(), oldPw, newPw);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "密碼修改成功"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "舊密碼錯誤"));
        }
    }

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

        List<TaskDTO.AssigneeDTO> responseList = employees.stream()
                .filter(emp -> !"MANAGER".equalsIgnoreCase(emp.getRole()))
                .map(emp -> {
                    TaskDTO.AssigneeDTO dto = new TaskDTO.AssigneeDTO();
                    dto.setEmpId(emp.getId());
                    dto.setName(emp.getName());
                    dto.setDepartmentName(emp.getDepartment() != null ? emp.getDepartment().getDeptName() : "未知部門");
                    return dto;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }
}
