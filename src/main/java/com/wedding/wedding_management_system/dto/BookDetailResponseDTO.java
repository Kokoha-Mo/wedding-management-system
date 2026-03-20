package com.wedding.wedding_management_system.dto;

import com.wedding.wedding_management_system.entity.BookDetail;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookDetailResponseDTO {
    private Integer   detailId;
    private Integer   serviceId;
    private String    serviceName;
    private Integer   unitPrice;
    private LocalDate ceremonyDate;

    public static BookDetailResponseDTO from(BookDetail bd) {
        BookDetailResponseDTO dto = new BookDetailResponseDTO();
        dto.detailId     = bd.getId();
        dto.serviceId    = bd.getService().getId();
        dto.serviceName  = bd.getService().getName();
        dto.unitPrice    = bd.getUnitPrice();
        dto.ceremonyDate = bd.getCeremonyDate();
        return dto;
    }
}