package com.wedding.wedding_management_system.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateBookDetailsRequestDTO {
    private String notes;
    private List<BookDetailRequestDTO> details;
}
