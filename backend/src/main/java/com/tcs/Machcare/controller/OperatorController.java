package com.tcs.Machcare.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tcs.Machcare.dto.CsvUploadResponse;
import com.tcs.Machcare.dto.FaultDTO;
import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.entity.Machine;
import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.exception.CsvValidationException;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.service.AuthenticatedUserService;
import com.tcs.Machcare.service.AuthenticatedUserService.AuthenticatedUser;
import com.tcs.Machcare.service.AuditLogService;
import com.tcs.Machcare.service.FaultLogService;
import com.tcs.Machcare.service.MachCareCoreService;
import com.tcs.Machcare.util.Jwtutil;

@RestController
@RequestMapping("/api/operator/faults")
@CrossOrigin(origins = "*")
public class OperatorController {

    @Autowired private FaultLogService faultService;
    @Autowired private Jwtutil jwtUtil;
    @Autowired private EmployeeRepository empRepo; // Used to fetch the user's name
    @Autowired private AuthenticatedUserService authenticatedUserService;
    @Autowired private AuditLogService auditLogService;

    @Autowired 
    private MachCareCoreService coreService;
    // 1. MANUAL LOG (OPERATOR ONLY)
    @PostMapping("/log")
    public ResponseEntity<?> logFault(
            @RequestBody FaultDTO dto,
            @RequestHeader("Authorization") String token) {
        try {
            AuthenticatedUser operator = authenticatedUserService.requireRole(
                    token,
                    3,
                    "ACCESS DENIED: Only Operators can manually log faults."
            );

            FaultLog savedFault = faultService.createFault(dto, operator.getEmpId(), operator.getName());
            auditLogService.logActivity(
                    operator.getEmpId(),
                    operator.getName(),
                    empRepo.findById(operator.getEmpId()).map(Employee::getEmail).orElse(null),
                    operator.getRoleId(),
                    "Manual Fault Log",
                    "SUCCESS",
                    "Fault created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "data", savedFault
            ));
        } catch (RuntimeException ex) {
            AuthenticatedUser actor = null;
            try {
                actor = authenticatedUserService.fromToken(token);
            } catch (Exception ignored) {
            }

            Long empId = actor != null ? actor.getEmpId() : null;
            Integer roleId = actor != null ? actor.getRoleId() : null;
            String name = actor != null ? actor.getName() : null;
            String email = empId != null ? empRepo.findById(empId).map(Employee::getEmail).orElse(null) : null;
            auditLogService.logActivity(
                    empId,
                    name,
                    email,
                    roleId,
                    "Manual Fault Log",
                    "FAILED",
                    ex.getMessage());
            throw ex;
        }
    }
    
    
    
    
 // ==========================================
    // OPERATOR DASHBOARD
    // ==========================================
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getOperatorDashboard(
            @RequestHeader("Authorization") String token) {
            
        if (jwtUtil.extractRoleId(token) != 3) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long empId = jwtUtil.extractEmpId(token);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("role", "OPERATOR");
        response.put("dashboardData", coreService.getOperatorDashboardStats(empId));
        
        return ResponseEntity.ok(response);
    }

    // 2. CSV UPLOAD (OPERATOR)
    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
            
        try {
            AuthenticatedUser operator = authenticatedUserService.requireRole(
                    token,
                    3,
                    "ACCESS DENIED: Role mismatch."
            );
            CsvUploadResponse response = faultService.uploadCsv(file, operator);
            auditLogService.logActivity(
                    operator.getEmpId(),
                    operator.getName(),
                    empRepo.findById(operator.getEmpId()).map(Employee::getEmail).orElse(null),
                    operator.getRoleId(),
                    "CSV Upload",
                    "SUCCESS",
                    "CSV uploaded successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (CsvValidationException ex) {
            AuthenticatedUser actor = null;
            try {
                actor = authenticatedUserService.fromToken(token);
            } catch (Exception ignored) {
            }
            Long empId = actor != null ? actor.getEmpId() : null;
            Integer roleId = actor != null ? actor.getRoleId() : null;
            String name = actor != null ? actor.getName() : null;
            String email = empId != null ? empRepo.findById(empId).map(Employee::getEmail).orElse(null) : null;
            auditLogService.logActivity(
                    empId,
                    name,
                    email,
                    roleId,
                    "CSV Upload",
                    "FAILED",
                    ex.getMessage());
            return ResponseEntity.badRequest().body(
                    CsvUploadResponse.invalid(ex.getMessage(), ex.getMissingHeaders(), ex.getErrors())
            );
        } catch (RuntimeException ex) {
            AuthenticatedUser actor = null;
            try {
                actor = authenticatedUserService.fromToken(token);
            } catch (Exception ignored) {
            }
            Long empId = actor != null ? actor.getEmpId() : null;
            Integer roleId = actor != null ? actor.getRoleId() : null;
            String name = actor != null ? actor.getName() : null;
            String email = empId != null ? empRepo.findById(empId).map(Employee::getEmail).orElse(null) : null;
            auditLogService.logActivity(
                    empId,
                    name,
                    email,
                    roleId,
                    "CSV Upload",
                    "FAILED",
                    ex.getMessage());
            throw ex;
        }
    }
    
    // 3. VIEW ALL FAULTS
    @GetMapping
    public ResponseEntity<?> viewFaults(@RequestHeader("Authorization") String token) {
        if (jwtUtil.extractRoleId(token) != 3) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(Map.of("success", true, "data", faultService.getAllFaultLogs()));
    }
}





