package com.wedding.wedding_management_system.service;

import com.wedding.wedding_management_system.dto.TeamDto;
import com.wedding.wedding_management_system.entity.Employee;
import com.wedding.wedding_management_system.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {

    @Autowired
    private EmployeeRepository employeeRepository;

    // 撈所有員工，轉成前台需要的 TeamDto
    public List<TeamDto> getAllTeamMembers() {
        List<Employee> employees = employeeRepository.findAll();

        return employees.stream()
                .filter(emp -> emp.getDepartment() != null) // 避免 dept 為 null
                .map(emp -> {
                    TeamDto dto = new TeamDto();
                    dto.setDeptId(emp.getDepartment().getId());
                    dto.setName(emp.getName());
                    dto.setYears(emp.getYearExp());
                    dto.setBio(emp.getBio());
                    dto.setStyles(emp.getStyles());
                    dto.setPhotoUrl(emp.getImgPath());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
