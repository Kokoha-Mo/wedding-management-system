package com.wedding.wedding_management_system.util;

import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtToken {
	private static final long EXP_TIME = 10 * 60 * 1000; // 過期時間
	private static final long RESET_EXP_TIME = 10 * 60 * 1000; // 重設密碼
	private static final String SECURT = "JoyChu1223334444555556666667777777"; // JS要設定
	private static final Key key = Keys.hmacShaKeyFor(SECURT.getBytes());

	public static String createToken(String subject) {
		String token = Jwts.builder()
				.setSubject(subject)
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

	public static String getRole(String token){
		String role = parse(token).getBody().get("role", String.class);
		return role != null ? role: "CUSTOMER";

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

}
