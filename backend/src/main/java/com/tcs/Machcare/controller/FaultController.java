/*package com.tcs.Machcare.controller;

import java.util.HashMap;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Autowired;
import com.tcs.Machcare.dto.FaultDTO;
import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.service.FaultLogService;

@RestController
@RequestMapping({"/faultlogs", "/api/faultlogs"})
public class FaultController {

    private final FaultLogService service;

    public FaultController(
            FaultLogService service) {

        this.service = service;
    }

    // ====================================
    // GET NEXT FAULT ID
    // ====================================

    @GetMapping("/next-id")
    public ResponseEntity<?> getNextFaultId() {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "faultId",
                service.getNextFaultId()
        );

        return ResponseEntity.ok(response);
    }

    // ====================================
    // GET CURRENT TIMESTAMP
    // ====================================

    @GetMapping("/current-time")
    public ResponseEntity<?> getCurrentTime() {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "timestamp",
                service.getCurrentTimestamp()
        );

        return ResponseEntity.ok(response);
    }

    // ====================================
    // CREATE FAULT
    // ====================================

    @PostMapping
    public ResponseEntity<?> createFault(
            @RequestBody FaultDTO dto) {

        FaultLog savedFault =
                service.createFault(dto);

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "Fault created successfully"
        );

        response.put(
                "data",
                savedFault
        );

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    // ====================================
    // GET ALL
    // ====================================

    @GetMapping
    public ResponseEntity<?> getAllFaultLogs() {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "data",
                service.getAllFaultLogs()
        );

        return ResponseEntity.ok(response);
    }

    // ====================================
    // GET BY ID
    // ====================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getFaultById(
            @PathVariable String id) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "data",
                service.getFaultById(id)
        );

        return ResponseEntity.ok(response);
    }

    // ====================================
    // CSV UPLOAD
    // ====================================

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsv(
            @RequestParam("file")
            MultipartFile file) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "CSV uploaded successfully"
        );

        response.put(
                "data",
                service.uploadCsv(file)
        );

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }
}*/
package com.tcs.Machcare.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import com.tcs.Machcare.dto.FaultDTO;
import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.entity.Employee; // Make sure to import your Employee entity
import com.tcs.Machcare.service.AuditLogService;
import com.tcs.Machcare.service.FaultLogService;

@RestController
@RequestMapping({"/faultlogs", "/api/faultlogs"})
@CrossOrigin(origins = "*")
public class FaultController {

    private final FaultLogService service;
        @Autowired private AuditLogService auditLogService;

    public FaultController(FaultLogService service) {
        this.service = service;
    }

    @GetMapping("/next-id")
    public ResponseEntity<?> getNextFaultId() {
        return ResponseEntity.ok(Map.of("success", true, "faultId", service.getNextFaultId()));
    }

    @GetMapping("/current-time")
    public ResponseEntity<?> getCurrentTime() {
        return ResponseEntity.ok(Map.of("success", true, "timestamp", service.getCurrentTimestamp()));
    }

    // ====================================
    // CREATE FAULT (OPERATOR ONLY)
    // ====================================
    @PostMapping("/faults/log")
    public ResponseEntity<?> createFault(
            @RequestBody FaultDTO dto,
            @RequestAttribute("loggedInEmployee") Employee loggedInEmp) { // Injected by your JWT Filter

        // STRICT AUTHORIZATION: Only Operators (Role ID = 3) can log manual faults
        if (loggedInEmp.getRoleId() != 3) {
                        auditLogService.logActivity(
                                loggedInEmp.getEmpId(),
                                loggedInEmp.getName(),
                                loggedInEmp.getEmail(),
                                loggedInEmp.getRoleId(),
                                "Manual Fault Log",
                                "FAILED",
                                "ACCESS DENIED: Only Operators can log manual faults.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false, 
                "message", "ACCESS DENIED: Only Operators can log manual faults."
            ));
        }

                try {
                        // Pass the authenticated context down to the service
                        FaultLog savedFault = service.createFault(dto, loggedInEmp.getEmpId(), loggedInEmp.getName());

                        auditLogService.logActivity(
                                loggedInEmp.getEmpId(),
                                loggedInEmp.getName(),
                                loggedInEmp.getEmail(),
                                loggedInEmp.getRoleId(),
                                "Manual Fault Log",
                                "SUCCESS",
                                "Fault created successfully");

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Fault created successfully");
                        response.put("data", savedFault);

                        return new ResponseEntity<>(response, HttpStatus.CREATED);
                } catch (RuntimeException ex) {
                        auditLogService.logActivity(
                                loggedInEmp.getEmpId(),
                                loggedInEmp.getName(),
                                loggedInEmp.getEmail(),
                                loggedInEmp.getRoleId(),
                                "Manual Fault Log",
                                "FAILED",
                                ex.getMessage());
                        throw ex;
                }
    }

    @GetMapping
    public ResponseEntity<?> getAllFaultLogs() {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getAllFaultLogs()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFaultById(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getFaultById(id)));
    }

    // ====================================
    // CSV UPLOAD (ADMIN & OPERATOR ONLY)
    // ====================================
    @PostMapping("/upload")
    public ResponseEntity<?> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("loggedInEmployee") Employee loggedInEmp) {

        // STRICT AUTHORIZATION: Only Admins (Role ID = 1) and Operators (Role ID = 3)
        if (loggedInEmp.getRoleId() != 1 && loggedInEmp.getRoleId() != 3) {
                        auditLogService.logActivity(
                                loggedInEmp.getEmpId(),
                                loggedInEmp.getName(),
                                loggedInEmp.getEmail(),
                                loggedInEmp.getRoleId(),
                                "CSV Upload",
                                "FAILED",
                                "ACCESS DENIED: Only Admins and Operators can upload CSV logs.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false, 
                "message", "ACCESS DENIED: Only Admins and Operators can upload CSV logs."
            ));
        }

                try {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "CSV uploaded successfully");
                        response.put("data", service.uploadCsv(file));

                        auditLogService.logActivity(
                                loggedInEmp.getEmpId(),
                                loggedInEmp.getName(),
                                loggedInEmp.getEmail(),
                                loggedInEmp.getRoleId(),
                                "CSV Upload",
                                "SUCCESS",
                                "CSV uploaded successfully");

                        return new ResponseEntity<>(response, HttpStatus.CREATED);
                } catch (RuntimeException ex) {
                        auditLogService.logActivity(
                                loggedInEmp.getEmpId(),
                                loggedInEmp.getName(),
                                loggedInEmp.getEmail(),
                                loggedInEmp.getRoleId(),
                                "CSV Upload",
                                "FAILED",
                                ex.getMessage());
                        throw ex;
                }
    }
}
