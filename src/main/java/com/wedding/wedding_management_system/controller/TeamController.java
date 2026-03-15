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

    // GET http://localhost:8080/api/team
    // 回傳所有員工的 TeamDto 陣列供前台婚禮團隊頁面使用
    @GetMapping("/team")
    public ResponseEntity<List<TeamDto>> getTeamMembers() {
        List<TeamDto> members = teamService.getAllTeamMembers();
        return ResponseEntity.ok(members);
    }
}
