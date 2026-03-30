package com.wedding.wedding_management_system.service;

import org.springframework.stereotype.Service;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

@Service
public class EmailValidationService {

    /**
     * 檢查信箱網域是否具有 MX (收信) 紀錄
     */
    public boolean isDomainValid(String email) {
        // 基本防呆：如果是空值或沒有 @ 符號，直接退回
        if (email == null || !email.contains("@")) {
            return false;
        }

        // 擷取 @ 後面的網域 (例如 gmail.com)
        String domain = email.substring(email.indexOf("@") + 1);

        try {
            // 向 DNS 伺服器查詢該網域的 MX 紀錄
            Lookup lookup = new Lookup(domain, Type.MX);
            Record[] records = lookup.run();

            // 如果查詢成功，且有回傳至少一筆紀錄，代表該網域真的可以收信
            return lookup.getResult() == Lookup.SUCCESSFUL && records != null && records.length > 0;

        } catch (Exception e) {
            // 查詢過程中發生任何異常 (例如網域格式完全錯誤)，視為無效
            return false;
        }
    }
}