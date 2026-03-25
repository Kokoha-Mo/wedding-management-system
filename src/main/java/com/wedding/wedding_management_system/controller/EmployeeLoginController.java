package com.wedding.wedding_management_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.wedding_management_system.dto.EmpLoginDto;
import com.wedding.wedding_management_system.dto.EmployeeLoginResponseDto;
import com.wedding.wedding_management_system.service.EmployeeLoginService;

@RestController
@RequestMapping("/api/employee")
public class EmployeeLoginController {

    @Autowired
    private EmployeeLoginService employeeLoginService;

    // {
    // "emp_email": "user@example.com",
    // "emp_password": "yourpassword"
    // }
    @PostMapping("/login")
    public ResponseEntity<EmployeeLoginResponseDto> login(@RequestBody EmpLoginDto loginDto) {
        try {
            EmployeeLoginResponseDto result = employeeLoginService.login(loginDto);

            ResponseCookie jwtCookie = ResponseCookie.from("jwtToken", result.getToken())
                    .httpOnly(true)
                    .secure(false) // HTTP fallback for local dev
                    .path("/")
                    .maxAge(8 * 60 * 60) // 8 hours matching JwtToken validity
                    .build();

            // Clear the token so it does not appear in the response body
            result.setToken(null);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(result);

        } catch (RuntimeException e) {

            System.out.println("登入失敗原因: " + e.getMessage());
            e.printStackTrace();
            
            EmployeeLoginResponseDto errorBody = new EmployeeLoginResponseDto();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<java.util.Map<String, String>> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwtToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(java.util.Map.of("message", "登出成功"));
    }
}
