package com.tcs.Machcare.dto;

public class LoginRequest {
    private String email;
    private String password;

    // Default Constructor
    public LoginRequest() {}

    // ADD THESE GETTERS
    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    // ADD THESE SETTERS (Required for Spring to fill the DTO)
    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}