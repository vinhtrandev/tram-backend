package com.vinhtran.tram.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    private Key cachedKey;

    /**
     * FIX: key() được gọi mỗi request → tạo object mới mỗi lần.
     * Cache lại sau khi khởi tạo để tránh tạo Key lặp đi lặp lại.
     *
     * FIX: secret quá ngắn sẽ ném WeakKeyException khi dùng HS256 (cần >= 256 bit / 32 ký tự).
     * Validate ngay khi khởi động để fail-fast.
     */
    @PostConstruct
    private void init() {
        if (secret == null || secret.length() < 32)
            throw new IllegalStateException(
                    "app.jwt.secret phải dài ít nhất 32 ký tự (256 bit) để dùng HS256");
        cachedKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String nickname) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(nickname)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(cachedKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getNickname(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(cachedKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(cachedKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}