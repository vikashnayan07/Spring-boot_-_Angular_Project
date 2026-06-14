package com.tcs.Machcare.controller;

import com.tcs.Machcare.service.RealtimeEventService;
import com.tcs.Machcare.util.Jwtutil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/realtime")
@CrossOrigin(origins = "*")
public class RealtimeController {
    private final RealtimeEventService realtimeEventService;
    private final Jwtutil jwtUtil;

    public RealtimeController(RealtimeEventService realtimeEventService, Jwtutil jwtUtil) {
        this.realtimeEventService = realtimeEventService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("token") String token, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        Long empId = jwtUtil.extractEmpId(token);
        Integer roleId = jwtUtil.extractRoleId(token);
        return realtimeEventService.connect(empId, roleId);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String queryToken) {
        String token = authorization != null && !authorization.isBlank() ? authorization : queryToken;
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Missing token."));
        }
        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(403).body(Map.of("success", false));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "connections", realtimeEventService.snapshot()
        ));
    }
}
