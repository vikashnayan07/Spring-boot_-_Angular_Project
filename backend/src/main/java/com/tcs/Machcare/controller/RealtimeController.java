package com.tcs.Machcare.controller;

import com.tcs.Machcare.service.RealtimeEventService;
import com.tcs.Machcare.util.Jwtutil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam("token") String token) {
        Long empId = jwtUtil.extractEmpId(token);
        Integer roleId = jwtUtil.extractRoleId(token);
        return realtimeEventService.connect(empId, roleId);
    }
}
