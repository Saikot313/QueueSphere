package com.qflow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(String secret, long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMinutes * 60_000L;
    }

    public String generate(Long userId, Long tenantId, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenantId", String.valueOf(tenantId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    public JwtPrincipal parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new JwtPrincipal(
                Long.valueOf(c.getSubject()),
                Long.valueOf(c.get("tenantId", String.class)),
                c.get("username", String.class),
                c.get("role", String.class));
    }
}