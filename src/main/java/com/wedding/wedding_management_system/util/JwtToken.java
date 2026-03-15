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

}
