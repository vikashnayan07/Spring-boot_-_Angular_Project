package com.tcs.Machcare.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class RealtimeEvent {
    private String type;
    private Object payload;
    private LocalDateTime timestamp;

    public RealtimeEvent() {}

    public RealtimeEvent(String type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static RealtimeEvent refresh(String type) {
        return new RealtimeEvent(type, Map.of("refresh", true));
    }
}
