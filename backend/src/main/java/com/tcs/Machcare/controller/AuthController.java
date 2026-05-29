package com.tcs.Machcare.controller;

import com.tcs.Machcare.dto.LoginRequest;
import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.util.Jwtutil;
import com.tcs.Machcare.service.AuthService;
import com.tcs.Machcare.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
    origins = {
        "http://localhost:4200",
        "http://localhost:9090"
    },
    allowCredentials = "true"
)
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private Jwtutil jwtUtil; 
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AuditLogService auditLogService;

    // ==========================================
    // 1. PUBLIC LOGIN API
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return authService.validateUser(request.getEmail(), request.getPassword())
                .map((Employee user) -> {
                    String token = jwtUtil.generateToken(user.getEmpId(), user.getRoleId(), user.getName());
                    auditLogService.logActivity(
                            user.getEmpId(),
                            user.getName(),
                            user.getEmail(),
                            user.getRoleId(),
                            "Login",
                            "SUCCESS",
                            "Login successful!");

                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "token", token,
                        "roleId", user.getRoleId(),
                        "name", user.getName(),
                        "isFirstLogin", user.getIsFirstLogin() != null ? user.getIsFirstLogin() : false,
                        "message", "Login successful!"
                    ));
                })
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        } catch (IllegalArgumentException ex) {
            Employee existing = employeeRepository.findByEmail(request.getEmail()).orElse(null);
            auditLogService.logActivity(
                    existing != null ? existing.getEmpId() : null,
                    existing != null ? existing.getName() : request.getEmail(),
                    request.getEmail(),
                    existing != null ? existing.getRoleId() : null,
                    "Login",
                    "FAILED",
                    ex.getMessage());
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception e) {
            Employee existing = employeeRepository.findByEmail(request.getEmail()).orElse(null);
            auditLogService.logActivity(
                    existing != null ? existing.getEmpId() : null,
                    existing != null ? existing.getName() : request.getEmail(),
                    request.getEmail(),
                    existing != null ? existing.getRoleId() : null,
                    "Login",
                    "FAILED",
                    e.getMessage());
            throw e;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        Long empId = jwtUtil.extractEmpId(token);
        Employee employee = employeeRepository.findById(empId)
            .orElseThrow(() -> new RuntimeException("Employee record missing"));

        return ResponseEntity.ok(Map.of(
            "empId", employee.getEmpId(),
            "name", employee.getName(),
            "email", employee.getEmail(),
            "roleId", employee.getRoleId()
        ));
    }

    // ==========================================
    // 👉 2. BRAND NEW: SETUP ACCOUNT API
    // ==========================================
    @PostMapping("/setup-account")
    public ResponseEntity<?> setupAccount(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> setupData) {
        
        try {
            // Extract the secure ID from the token so they can't change someone else's password
            Long empId = jwtUtil.extractEmpId(token);
            
            authService.setupFirstTimeAccount(
                empId, 
                setupData.get("newPassword"),
                setupData.get("q1"),
                setupData.get("a1"),
                setupData.get("q2"),
                setupData.get("a2")
            );

            Employee employee = employeeRepository.findById(empId).orElse(null);
            auditLogService.logActivity(
                    empId,
                    employee != null ? employee.getName() : jwtUtil.extractName(token),
                    employee != null ? employee.getEmail() : null,
                    employee != null ? employee.getRoleId() : jwtUtil.extractRoleId(token),
                    "First-Time Password Setup",
                    "SUCCESS",
                    "Account successfully secured. Welcome to MachCare!");
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Account successfully secured. Welcome to MachCare!"
            ));
            
        } catch (Exception e) {
            Long empId = jwtUtil.extractEmpId(token);
            Employee employee = employeeRepository.findById(empId).orElse(null);
            auditLogService.logActivity(
                    empId,
                    employee != null ? employee.getName() : jwtUtil.extractName(token),
                    employee != null ? employee.getEmail() : null,
                    employee != null ? employee.getRoleId() : jwtUtil.extractRoleId(token),
                    "First-Time Password Setup",
                    "FAILED",
                    e.getMessage());
            return ResponseEntity.status(400).body(Map.of(
                "success", false, 
                "message", "Failed to setup account: " + e.getMessage()
            ));
        }
    }
    
    
 // ==========================================
    // 👉 FORGOT PASSWORD APIs
    // ==========================================
    
    @GetMapping("/security-questions")
    public ResponseEntity<?> getSecurityQuestions(@RequestParam String email) {
        try {
            Map<String, String> questions = authService.getSecurityQuestions(email);
            return ResponseEntity.ok(Map.of("success", true, "data", questions));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        try {
            authService.resetForgotPassword(
                payload.get("email"),
                payload.get("a1"),
                payload.get("a2"),
                payload.get("newPassword")
            );

            Employee employee = employeeRepository.findByEmail(payload.get("email")).orElse(null);
            auditLogService.logActivity(
                    employee != null ? employee.getEmpId() : null,
                    employee != null ? employee.getName() : payload.get("email"),
                    payload.get("email"),
                    employee != null ? employee.getRoleId() : null,
                    "Password Reset",
                    "SUCCESS",
                    "Password successfully reset!");
            return ResponseEntity.ok(Map.of("success", true, "message", "Password successfully reset!"));
        } catch (Exception e) {
            Employee employee = employeeRepository.findByEmail(payload.get("email")).orElse(null);
            auditLogService.logActivity(
                    employee != null ? employee.getEmpId() : null,
                    employee != null ? employee.getName() : payload.get("email"),
                    payload.get("email"),
                    employee != null ? employee.getRoleId() : null,
                    "Password Reset",
                    "FAILED",
                    e.getMessage());
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
