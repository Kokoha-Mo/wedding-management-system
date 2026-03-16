package com.wedding.wedding_management_system.config;

import com.wedding.wedding_management_system.repository.CustomerRepository;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final CustomerRepository customerRepository;
        private final EmployeeRepository employeeRepository;

        public SecurityConfig(CustomerRepository customerRepository, EmployeeRepository employeeRepository) {
                this.customerRepository = customerRepository;
                this.employeeRepository = employeeRepository;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        // Customer UserDetailsService
        @Bean
        public UserDetailsService customerUserDetailsService() {
                return username -> customerRepository.findByEmail(username)
                                .map(customer -> User.builder()
                                                .username(customer.getEmail())
                                                .password(customer.getPassword())
                                                .roles("CUSTOMER") // Translates to ROLE_CUSTOMER
                                                .build())
                                .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));
        }

        // Employee UserDetailsService
        @Bean
        @Primary
        public UserDetailsService employeeUserDetailsService() {
                return username -> employeeRepository.findByEmail(username)
                                .map(employee -> User.builder()
                                                .username(employee.getEmail())
                                                .password(employee.getPassword())
                                                .roles(employee.getRole().toUpperCase()) // Translates to ROLE_ADMIN,
                                                                                         // ROLE_STAFF
                                                .build())
                                .orElseThrow(() -> new UsernameNotFoundException("Employee not found"));
        }

        // AuthenticationManager beans to support dual login
        @Bean
        public AuthenticationManager authenticationManager(
                        PasswordEncoder passwordEncoder,
                        UserDetailsService customerUserDetailsService,
                        UserDetailsService employeeUserDetailsService) {

                DaoAuthenticationProvider customerProvider = new DaoAuthenticationProvider(customerUserDetailsService);
                customerProvider.setPasswordEncoder(passwordEncoder);

                DaoAuthenticationProvider employeeProvider = new DaoAuthenticationProvider(employeeUserDetailsService);
                employeeProvider.setPasswordEncoder(passwordEncoder);

                return new ProviderManager(customerProvider, employeeProvider);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults()) // 啟用 CORS
                                .csrf(csrf -> csrf.disable()) // Disable CSRF for API usage
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER")
                                                .requestMatchers("/api/staff/**").hasAnyRole("MANAGER", "STAFF")
                                                .requestMatchers("/api/customer/login").permitAll() // 開放客戶登入 API
                                                .requestMatchers("/api/employee/login").permitAll() // 開放員工登入 API
                                                .requestMatchers("/api/employee/logout").permitAll() // 開放員工登出 API
                                                .requestMatchers("/api/employee/**").authenticated() // 其他員工路由需要 JWT 驗證
                                                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                                                .anyRequest().permitAll());
                // .httpBasic(Customizer.withDefaults()); // 可以移除 Basic Auth，改用 JWT

                http.addFilterBefore(new com.wedding.wedding_management_system.filter.JwtFilter(),
                                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
