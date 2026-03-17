package com.wedding.wedding_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // 前端重設密碼頁面的網址
    @Value("${app.reset-password-url:http://127.0.0.1:5500/src/main/resources/static/client/reset_password.html}")
    private String resetPasswordUrl;

    public void sendResetPasswordEmail(String toEmail, String customerName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("【DREAM VENUES】帳號密碼設定通知");

            String resetLink = resetPasswordUrl + "?token=" + token;

            // 信件內容（HTML 格式）
            String htmlContent = """
                    <div style="font-family: 'Noto Serif TC', serif; max-width: 560px; margin: 0 auto; background: #faf7f2; padding: 48px 40px;">
                        <div style="text-align: center; margin-bottom: 40px;">
                            <p style="font-size: 10px; letter-spacing: 0.4em; color: #C5A059; text-transform: uppercase; margin: 0;">Dream Venues</p>
                            <h1 style="font-size: 24px; font-weight: 300; color: #2b2520; letter-spacing: 0.08em; margin: 12px 0 0;">High-End Wedding Journal</h1>
                        </div>
                        <div style="background: #ffffff; padding: 40px; border-radius: 4px; box-shadow: 0 2px 12px rgba(0,0,0,0.06);">
                            <p style="font-size: 15px; color: #2b2520; line-height: 2; letter-spacing: 0.06em;">
                                親愛的 <strong>%s</strong> 您好，
                            </p>
                            <p style="font-size: 13px; color: #6a6053; line-height: 2.2; letter-spacing: 0.06em;">
                                感謝您選擇 DREAM VENUES，<br>
                                您的婚禮規劃已建立完成。<br>
                                請點擊下方按鈕設定您的帳號密碼，即可開始查看您的專屬婚禮規劃。
                            </p>
                            <div style="text-align: center; margin: 36px 0;">
                                <a href="%s"
                                   style="display: inline-block; background: #2b2520; color: #ffffff; text-decoration: none;
                                          padding: 16px 40px; font-size: 11px; letter-spacing: 0.4em;
                                          text-transform: uppercase; border-radius: 3px;">
                                    設定帳號密碼
                                </a>
                            </div>
                            <p style="font-size: 11px; color: #9c8e7f; line-height: 2; letter-spacing: 0.06em;">
                                此連結將於 <strong>10 分鐘後</strong>失效。<br>
                                若您未申請此服務，請忽略此封郵件。
                            </p>
                            <hr style="border: none; border-top: 1px solid rgba(197,160,89,0.2); margin: 28px 0;">
                            <p style="font-size: 10px; color: #b0a090; letter-spacing: 0.1em; line-height: 1.8;">
                                如按鈕無法點擊，請複製以下連結貼到瀏覽器：<br>
                                <span style="color: #C5A059;">%s</span>
                            </p>
                        </div>
                        <p style="text-align: center; font-size: 10px; color: #b0a090; letter-spacing: 0.2em; margin-top: 32px;">
                            © 2026 DREAM VENUES WEDDING DESIGN
                        </p>
                    </div>
                    """
                    .formatted(customerName, resetLink, resetLink);

            helper.setText(htmlContent, true); // true = 啟用 HTML

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("寄信失敗：" + e.getMessage());
        }
    }
}