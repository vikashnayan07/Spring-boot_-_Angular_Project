package com.tcs.Machcare.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class Jwtutil {

    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 86400000; // 1 Day

    // 1. Generate Token (Now accepts and stores the Name!)
    public String generateToken(Long empId, Integer roleId, String name) {
        return Jwts.builder()
                .setSubject(String.valueOf(empId)) // Employee ID is securely stored as the subject
                .claim("roleId", roleId)           // Role ID is stored here
                .claim("name", name)               // 👉 NEW: Name is securely stored here
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
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
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();
    }
}