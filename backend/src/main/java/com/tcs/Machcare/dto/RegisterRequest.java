package com.tcs.Machcare.dto;

import com.tcs.Machcare.entity.RoleType;

public class RegisterRequest {
    
    private String name;
    private String email;
    private String password;
    private RoleType role; 

    // --- Getters and Setters ---

    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }

    public String getEmail() { 
        return email; 
    }
    
    public void setEmail(String email) { 
        this.email = email; 
    }

    public String getPassword() { 
        return password; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }

    public RoleType getRole() { 
        return role; 
    }
    
    public void setRole(RoleType role) { 
        this.role = role; 
    }
}