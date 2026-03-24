package com.wedding.wedding_management_system.dto;

import com.wedding.wedding_management_system.entity.Book;
import com.wedding.wedding_management_system.entity.Customer;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookResponseDTO {

    private Integer bookId;
    private String  customerName;
    private String  tel;
    private String  email;
    private String  lineId;
    private LocalDate weddingDate;
    private Integer guestScale;
    private String  place;
    private String  styles;
    private String  content;
    private String  status;
    private LocalDateTime createAt;
    private String managerName;
    private String signAt;
    private String updateAt;

    public static BookResponseDTO from(Book book, Customer customer) {
        BookResponseDTO dto = new BookResponseDTO();
        dto.bookId       = book.getId();
        dto.weddingDate  = book.getWeddingDate();
        dto.guestScale   = book.getGuestScale();
        dto.place        = book.getPlace();
        dto.styles       = book.getStyles();
        dto.content      = book.getContent();
        dto.status       = book.getStatus();
        dto.createAt     = book.getCreateAt();
        dto.setUpdateAt(book.getUpdateAt() != null ? book.getUpdateAt().toString() : null);

        if (customer != null) {
            dto.customerName = customer.getName();
            dto.tel          = customer.getTel();
            dto.email        = customer.getEmail();
            dto.lineId       = customer.getLineId();
        }

        if (book.getManager() != null) {
            String fullName = book.getManager().getName();
            String englishName = fullName.replaceAll("[^a-zA-Z\\s]", "").trim();
            dto.managerName = englishName.isEmpty() ? fullName : englishName;//
        }
        try {
            if (book.getProject() != null && book.getProject().getCreateAt() != null) {
                dto.signAt = book.getProject().getCreateAt().toLocalDate().toString();
            }
        } catch (Exception e) {
            dto.signAt = null;
        }
        return dto;
    }



}
