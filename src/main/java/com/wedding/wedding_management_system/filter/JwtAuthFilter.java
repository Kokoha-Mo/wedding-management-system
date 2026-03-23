package com.wedding.wedding_management_system.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wedding.wedding_management_system.util.JwtToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String customerToken = null;
        String empToken = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("customerToken".equals(cookie.getName())) {
                    customerToken = cookie.getValue();
                } else if ("jwtToken".equals(cookie.getName())) {
                    empToken = cookie.getValue();
                }
            }
        }

        // 根據請求路徑決定用哪個 token
        String path = request.getRequestURI();
        String token = path.startsWith("/api/customer/") ? customerToken : empToken;

        if (token != null && JwtToken.isValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = JwtToken.getEmail(token);
            String authority = "ROLE_" + JwtToken.getRole(token);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority(authority)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
