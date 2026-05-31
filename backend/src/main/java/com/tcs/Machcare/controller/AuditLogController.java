package com.tcs.Machcare.controller;

import com.tcs.Machcare.entity.AuditLog;
import com.tcs.Machcare.service.AuditLogService;
import com.tcs.Machcare.util.Jwtutil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit-logs")
@CrossOrigin(
    origins = {
        "https://machcare-frontend.vercel.app",
        "http://localhost:4200",
        "http://localhost:8080",
        "http://localhost:9090"
    },
    allowCredentials = "true"
)
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private Jwtutil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getAuditLogs(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins can view audit logs."));
        }

        List<AuditLog> filteredLogs = auditLogService.getFilteredLogs(date, roleId, status, action);
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 1);
        int totalItems = filteredLogs.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) safeSize));
        int fromIndex = Math.min((safePage - 1) * safeSize, totalItems);
        int toIndex = Math.min(fromIndex + safeSize, totalItems);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", filteredLogs.subList(fromIndex, toIndex));
        response.put("page", safePage);
        response.put("size", safeSize);
        response.put("totalItems", totalItems);
        response.put("totalPages", totalPages);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestHeader("Authorization") String token) {
        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins can view audit logs."));
        }

        return ResponseEntity.ok(Map.of("success", true, "data", auditLogService.getSummary()));
    }
}