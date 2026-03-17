package com.wedding.wedding_management_system.dto;

import lombok.Data;

@Data
public class WeddingInfoDTO {
    private String weddingDate;
    private Integer guestCount;
    private String venue;
    private String theme;
    private String notes;
}
