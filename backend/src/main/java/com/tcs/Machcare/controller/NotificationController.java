package com.tcs.Machcare.controller;

import com.tcs.Machcare.entity.Notification;
import com.tcs.Machcare.service.NotificationService;
import com.tcs.Machcare.util.Jwtutil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final Jwtutil jwtUtil;

    public NotificationController(NotificationService notificationService, Jwtutil jwtUtil) {
        this.notificationService = notificationService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        if (token == null || token.isBlank()) {
            response.put("success", true);
            response.put("data", Collections.emptyList());
            response.put("unreadCount", 0);
            response.put("warning", "Missing authentication token.");
            return ResponseEntity.ok(response);
        }
        try {
            Long empId = jwtUtil.extractEmpId(token);
            Integer roleId = jwtUtil.extractRoleId(token);
            List<Notification> notifications = notificationService.listFor(empId, roleId);
            response.put("success", true);
            response.put("data", notifications);
            response.put("unreadCount", notificationService.unreadCount(empId, roleId));
        } catch (RuntimeException ex) {
            response.put("success", true);
            response.put("data", Collections.emptyList());
            response.put("unreadCount", 0);
            response.put("warning", "Notifications are temporarily unavailable.");
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markRead(
            @PathVariable Long notificationId,
            @RequestHeader("Authorization") String token) {
        Long empId = jwtUtil.extractEmpId(token);
        Integer roleId = jwtUtil.extractRoleId(token);
        notificationService.markRead(notificationId, empId, roleId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "unreadCount", notificationService.unreadCount(empId, roleId)
        ));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(@RequestHeader("Authorization") String token) {
        Long empId = jwtUtil.extractEmpId(token);
        Integer roleId = jwtUtil.extractRoleId(token);
        notificationService.markAllRead(empId, roleId);
        return ResponseEntity.ok(Map.of("success", true, "unreadCount", 0));
    }
}
