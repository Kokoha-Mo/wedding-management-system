package com.wedding.wedding_management_system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 建立預約（books 表）請求 DTO
 *
 * 前端 modal「建立新客戶諮詢」送出的欄位對應：
 *   新人姓名 & 新人姓名    → 新郎 & 新娘姓名（一格）查詢或建立 customer，取得 customer_id
 *   聯絡手機         → customer.tel
 *   LINE ID         → customer.line_id
 *   Email           → customer.email
 *   預計婚期         → books.wedding_date
 *   賓客規模         → books.guest_scale
 *   宴客場地         → books.place
 *   視覺主題         → books.styles
 *   婚宴備註         → books.content
 */

@Data
public class CreateBookRequestDTO {

    // ─── 客戶基本資料（用來找或建立 customer）──────────────────
    @NotBlank(message = "姓名為必填")
    @Size(max = 50, message = "姓名不超過 50 字")
    private String name;

    @NotBlank(message = "手機號碼為必填")
    @Pattern(regexp = "^[0-9\\-+\\s]{7,20}$", message = "手機格式不正確")
    private String tel;

    public String setTel() {
        return tel == null ? null : tel.replaceAll("[^0-9]", "");
    }

    @Email(message = "Email 格式不正確")
    private String email;

    @JsonProperty("line_id")
    private String lineId;

    // ─── 預約細節（存入 books 表）──────────────────────────────
    @JsonProperty("wedding_date")
    private LocalDate weddingDate;

    @JsonProperty("guest_scale")
    private Integer guestScale;

    // 宴客場地
    private String place;

    // 風格（單選，對應 books.styles varchar(20)）
    private String styles;

    // 婚宴備註（對應 books.content）
    private String content;

    @JsonProperty("manager_id")
    private Integer managerId;
}
