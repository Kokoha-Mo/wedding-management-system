package com.wedding.wedding_management_system.util;

import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtToken {
    private static final long EXP_TIME = 60 * 60 * 1000; // 過期時間跟cookie和前端期限一樣
    private static final long RESET_EXP_TIME = 10 * 60 * 1000; // 重設密碼專用 token（10分鐘有效）
    // 讀取環境變數 JWT_SECRET
    private static final String SECRET_ENV = System.getenv("JWT_SECRET");
    // 如果環境變數是 null (本地沒設)，就用一串預設的字串，否則就用環境變數
    private static final String SECRET = (SECRET_ENV != null) ? SECRET_ENV
            : "default_secret_key_for_local_dev_only_1234567890";
    private static final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public static String createToken(String subject) {
        String token = Jwts.builder()
                .setSubject(subject)
                .claim("role", "CUSTOMER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXP_TIME))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        return token;
    }

    // 新增：支持帶 role 的 token 生成（用於員工登入）
    public static String createTokenWithRole(String subject, String role) {
        String token = Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXP_TIME))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        return token;
    }

    // 新增：從 token 中獲取 role
    public static String getRole(String token) {
        String role = parse(token).getBody().get("role", String.class);
        return role != null ? role : "CUSTOMER";
    }

    public static String parseToken(String token) {
        JwtParser parser = Jwts.parserBuilder().setSigningKey(key).build();
        String subject = parser.parseClaimsJws(token).getBody().getSubject();
        return subject;
    }

    public static Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public static String getEmail(String token) {
        return parse(token).getBody().getSubject();
    }

    public static boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String createResetToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("purpose", "reset_password") // 標記用途
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + RESET_EXP_TIME))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    // 驗證是不是重設密碼專用的 token
    public static boolean isResetToken(String token) {
        try {
            Claims claims = parse(token).getBody();
            return "reset_password".equals(claims.get("purpose", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    // 取得 Token 建立時的時間，用來處理重設密碼時60秒後才能再送
    public static Date getIssuedAt(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key) // 這裡的 key 要對應你原本類別裡的 key
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getIssuedAt(); // 抓取當初產生 Token 的時間 (iat)
        } catch (Exception e) {
            // 萬一 Token 是壞的、過期的或被竄改過，解析會報錯
            // 我們回傳「紀元時間 (1970年)」，讓頻率檢查失效，直接讓使用者可以重寄。
            return new Date(0);
        }
    }

}
