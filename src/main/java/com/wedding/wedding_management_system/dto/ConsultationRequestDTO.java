package com.wedding.wedding_management_system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ConsultationRequestDTO {

    private String name;
    private String phone;
    private String email;

    // 透過 @JsonProperty 精準對接前端的底線命名
    @JsonProperty("line_id")
    private String lineId;

    @JsonProperty("consultation_date")
    private LocalDate consultationDate;

    @JsonProperty("preferred_time")
    private String preferredTime;

    @JsonProperty("wedding_date")
    private LocalDate weddingDate;

    @JsonProperty("guest_scale")
    private String guestScale;

    // 前端傳來的是陣列，這裡就用 List 接！
    private List<String> styles;
    private List<String> services;

    @JsonProperty("additional_notes")
    private String additionalNotes;
}

