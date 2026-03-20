package com.wedding.wedding_management_system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookDetailRequestDTO {

    @JsonProperty("service_id")
    private Integer serviceId;

    @JsonProperty("unit_price")
    private Integer unitPrice;

    @JsonProperty("ceremony_date")
    private LocalDate ceremonyDate;  // 選填，有儀式時間才填
}
