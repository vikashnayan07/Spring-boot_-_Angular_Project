package com.tcs.Machcare.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tcs.Machcare.dto.ApiResponse;
import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.entity.FaultAnalysis;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.service.AuditLogService;
import com.tcs.Machcare.service.FaultAnalysisService;
import com.tcs.Machcare.util.Jwtutil;


@RestController
@RequestMapping({"/fault-analysis", "/api/fault-analysis"})
public class FaultAnalysisController {

    private final FaultAnalysisService service;
    @Autowired private AuditLogService auditLogService;
    @Autowired private Jwtutil jwtUtil;
    @Autowired private EmployeeRepository employeeRepository;

    public FaultAnalysisController(FaultAnalysisService service) {
        this.service = service;
    }

    //GENERATE ANALYSIS
    @PostMapping("/generate/{faultId}")
    public ResponseEntity<?> generateAnalysis(
            @PathVariable String faultId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Employee actor = resolveActor(token);

        try {
            FaultAnalysis analysis =
                service.generateAnalysis(faultId);

            auditLogService.logActivity(
                actor != null ? actor.getEmpId() : null,
                actor != null ? actor.getName() : null,
                actor != null ? actor.getEmail() : null,
                actor != null ? actor.getRoleId() : null,
                "Analyze Fault",
                "SUCCESS",
                "Fault analysis completed successfully.");

            return ResponseEntity.ok(
                ApiResponse.success("Fault analysis completed successfully.", analysis)
            );
        } catch (RuntimeException ex) {
            auditLogService.logActivity(
                actor != null ? actor.getEmpId() : null,
                actor != null ? actor.getName() : null,
                actor != null ? actor.getEmail() : null,
                actor != null ? actor.getRoleId() : null,
                "Analyze Fault",
                "FAILED",
                ex.getMessage());
            throw ex;
        }
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<?> getAllAnalyses() {

        return ResponseEntity.ok(
                ApiResponse.success("Fault analyses fetched successfully.", service.getAllAnalyses())
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.success("Fault analysis fetched successfully.", service.getAnalysisById(id))
        );
    }

    private Employee resolveActor(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            Long empId = jwtUtil.extractEmpId(token);
            return employeeRepository.findById(empId).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

}
