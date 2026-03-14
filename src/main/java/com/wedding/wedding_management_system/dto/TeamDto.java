package com.wedding.wedding_management_system.dto;

import lombok.Data;

/**
 * DTO for wedding_team.html
 *
 * Frontend field → Entity field:
 * dept_id → department.id
 * name → name
 * years → yearExp
 * bio → bio
 * styles → styles
 * photo_url → imgPath
 */
@Data
public class TeamDto {

    private Integer deptId;
    private String name;
    private String years;
    private String bio;
    private String styles;
    private String photoUrl;

}
