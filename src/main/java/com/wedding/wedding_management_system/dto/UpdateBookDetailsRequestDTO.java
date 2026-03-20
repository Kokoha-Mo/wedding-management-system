package com.wedding.wedding_management_system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateBookDetailsRequestDTO {
    private String name;        // 新人姓名（同時更新 customer.name）
    private String tel;

    @NotBlank(message = "Email 為必填")
    @Email(message = "Email 格式不正確")
    private String email;

    @JsonProperty("line_id")
    private String lineId;

    @JsonProperty("wedding_date")
    private LocalDate weddingDate;

    @JsonProperty("guest_scale")
    private Integer guestScale;

    private String place;
    private String styles;

    private String notes;
    private List<BookDetailRequestDTO> details;
}
