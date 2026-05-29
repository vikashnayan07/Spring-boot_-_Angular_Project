package com.tcs.Machcare.dto;

public class AuthResponse {
    private boolean success;
    private String role;
    private String name;

    public AuthResponse(boolean success, String role, String name) {
        this.success = success;
        this.role = role;
        this.name = name;
    }
    // Getters and Setters...
}