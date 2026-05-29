package com.tcs.Machcare.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true) // autoApply means Hibernate handles this automatically everywhere
public class PriorityConverter implements AttributeConverter<Priority, String> {

    // What to save into the database column
    @Override
    public String convertToDatabaseColumn(Priority priority) {
        if (priority == null) {
            return null;
        }
        // Save the clean value "1", "2", or "3" (or keep priority.name() if you want "_3")
        return priority.name(); 
    }

    // What to do when reading an old row from the database column
    @Override
    public Priority convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Priority._3; // Default fallback
        }

        String cleanValue = dbData.trim();
        
        // This stops the crash! Maps "3" or "_3" straight to your enum object
        switch (cleanValue) {
            case "1": case "_1": return Priority._1;
            case "2": case "_2": return Priority._2;
            case "3": case "_3": return Priority._3;
            default: return Priority._3; 
        }
    }
}