package com.tcs.Machcare.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Priority {
    _1("1"),
    _2("2"),
    _3("3");

    private final String value;

    Priority(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    // 🎯 THE MAGIC LINK: This translates "3" or "3" with an underscore seamlessly
    @JsonCreator
    public static Priority fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return _3; // Default fallback
        }
        
        // Clean the input string (removes any accidental quotes or spaces)
        String cleanValue = value.replace("\"", "").trim();
        
        // If the database sends "1", "2", "3", map it safely to our enum values
        switch (cleanValue) {
            case "1": case "_1": return _1;
            case "2": case "_2": return _2;
            case "3": case "_3": return _3;
            default: return _3; // Fallback for old mismatched data
        }
    }
}