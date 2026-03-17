package com.wedding.wedding_management_system.dto;

import com.wedding.wedding_management_system.entity.Customer;
import lombok.Data;

@Data
public class CustomerDTO {
    private Integer customerId;
    private String name;
    private String tel;
    private String email;


    public static CustomerDTO from(Customer c) {
        CustomerDTO dto = new CustomerDTO();
        dto.customerId = c.getId();
        dto.name       = c.getName();
        dto.tel        = c.getTel();
        dto.email      = c.getEmail();
        return dto;
    }
}
