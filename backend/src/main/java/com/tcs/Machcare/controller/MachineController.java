package com.tcs.Machcare.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tcs.Machcare.dto.MachineDTO;
import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.entity.Machine;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.service.AuditLogService;
import com.tcs.Machcare.service.MachineService;
import com.tcs.Machcare.util.Jwtutil;

@RestController
@RequestMapping({"/api/admin/machines", "/api/machines"}) // ✅ Upgraded to the secure Admin path!
@CrossOrigin(origins = "*")
public class MachineController {

    private final MachineService service;
    
    @Autowired 
    private Jwtutil jwtUtil; // ✅ Injected your JWT tool
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private AuditLogService auditLogService;

    public MachineController(MachineService service) {
        this.service = service;
    }

    // ==========================================
    // 1. ADMIN ONLY: CREATE MACHINE
    // ==========================================
    @PostMapping
    public ResponseEntity<?> createMachine(
            @RequestBody MachineDTO dto,
            @RequestHeader("Authorization") String token) {

        Employee actor = employeeRepository.findById(jwtUtil.extractEmpId(token)).orElse(null);

        // Strict Check: Only Admin (Role 1)
        if (jwtUtil.extractRoleId(token) != 1) {
            auditLogService.logActivity(
                    actor != null ? actor.getEmpId() : null,
                    actor != null ? actor.getName() : jwtUtil.extractName(token),
                    actor != null ? actor.getEmail() : null,
                    actor != null ? actor.getRoleId() : jwtUtil.extractRoleId(token),
                    "Register Machine",
                    "FAILED",
                    "ACCESS DENIED: Only Admins can create machines.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins can create machines."));
        }

        try {
            Machine machine = service.createMachine(dto);

            auditLogService.logActivity(
                    actor != null ? actor.getEmpId() : null,
                    actor != null ? actor.getName() : jwtUtil.extractName(token),
                    actor != null ? actor.getEmail() : null,
                    actor != null ? actor.getRoleId() : jwtUtil.extractRoleId(token),
                    "Register Machine",
                    "SUCCESS",
                    "Machine created successfully");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Machine created successfully");
            response.put("data", machine);

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException ex) {
            auditLogService.logActivity(
                    actor != null ? actor.getEmpId() : null,
                    actor != null ? actor.getName() : jwtUtil.extractName(token),
                    actor != null ? actor.getEmail() : null,
                    actor != null ? actor.getRoleId() : jwtUtil.extractRoleId(token),
                    "Register Machine",
                    "FAILED",
                    ex.getMessage());
            throw ex;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMachine(
            @PathVariable String id,
            @RequestBody MachineDTO dto,
            @RequestHeader("Authorization") String token) {

        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins can update machines."));
        }

        Machine machine = service.updateMachine(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Machine updated successfully");
        response.put("data", machine);

        return ResponseEntity.ok(response);
    }

 // ==========================================
    // 2. ADMIN & OPERATOR: GET ALL MACHINES
    // ==========================================
    @GetMapping
    public ResponseEntity<?> getAllMachines(
            @RequestHeader("Authorization") String token) {

        int roleId = jwtUtil.extractRoleId(token);

        // 👉 SECURITY CHECK: Allow Admin (1) and Operator (Assuming 3)
        // If the role is NOT 1 AND the role is NOT 3, block them.
        if (roleId != 1 && roleId != 3) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins and Operators can view the machine roster."));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Machines fetched successfully");
        response.put("data", service.getAllMachines());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ==========================================
    // 3. ADMIN & OPERATOR: GET MACHINE BY ID
    // ==========================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getMachineById(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        int roleId = jwtUtil.extractRoleId(token);

        // 👉 SECURITY CHECK: Allow Admin (1) and Operator (Assuming 3)
        if (roleId != 1 && roleId != 3) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins and Operators can view machine details."));
        }

        Machine machine = service.getMachineById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Machine found");
        response.put("data", machine);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
