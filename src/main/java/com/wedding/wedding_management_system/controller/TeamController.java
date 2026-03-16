package com.wedding.wedding_management_system.controller;

import com.wedding.wedding_management_system.dto.TeamDto;
import com.wedding.wedding_management_system.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @GetMapping("/team")
    public ResponseEntity<List<TeamDto>> getTeamMembers() {
        List<TeamDto> members = teamService.getAllTeamMembers();
        return ResponseEntity.ok(members);
    }
}
