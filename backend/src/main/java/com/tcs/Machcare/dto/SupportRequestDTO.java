package com.tcs.Machcare.dto;

public class SupportRequestDTO {
    private String reason;
    private Integer requiredEngineerCount;
    private String urgency;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getRequiredEngineerCount() {
        return requiredEngineerCount;
    }

    public void setRequiredEngineerCount(Integer requiredEngineerCount) {
        this.requiredEngineerCount = requiredEngineerCount;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }
}
