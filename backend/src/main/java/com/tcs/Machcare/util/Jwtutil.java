package com.tcs.Machcare.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class Jwtutil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationTime;

    private Key secretKey;

    @PostConstruct
    void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // 1. Generate Token (Now accepts and stores the Name!)
    public String generateToken(Long empId, Integer roleId, String name) {
        return Jwts.builder()
                .setSubject(String.valueOf(empId)) // Employee ID is securely stored as the subject
                .claim("roleId", roleId)           // Role ID is stored here
                .claim("name", name)               // 👉 NEW: Name is securely stored here
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    // 2. Extract Employee ID 
    public Long extractEmpId(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.parseLong(claims.getSubject()); // Fetches the empId from the subject
    }

    // 3. Extract Role ID
    public Integer extractRoleId(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("roleId", Integer.class);
    }

    // 4. 👉 NEW: Extract Name
    public String extractName(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("name", String.class);
    }

    // Helper method to avoid duplicating code
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();
    }
}