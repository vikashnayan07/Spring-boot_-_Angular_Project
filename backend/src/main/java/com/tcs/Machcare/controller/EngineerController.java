package com.tcs.Machcare.controller;

import com.tcs.Machcare.dto.ApiResponse;
import com.tcs.Machcare.dto.EngineerRaiseFaultRequestDTO;
import com.tcs.Machcare.dto.PartDTO;

import com.tcs.Machcare.dto.PartRequestDTO;
import com.tcs.Machcare.dto.SupportRequestDTO;

import com.tcs.Machcare.entity.MaintenanceSchedule;
import com.tcs.Machcare.entity.MaintenanceStatus;
import com.tcs.Machcare.entity.Part;
import com.tcs.Machcare.entity.Employee;

import com.tcs.Machcare.repository.MaintenanceHistoryRepository;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.util.Jwtutil;
import com.tcs.Machcare.service.MachCareCoreService;
import com.tcs.Machcare.service.FaultAnalysisService;
import com.tcs.Machcare.service.FaultLogService;
import com.tcs.Machcare.service.AuditLogService;
import com.tcs.Machcare.entity.MachineAlert;
import com.tcs.Machcare.entity.MaintenanceHistory;
import com.tcs.Machcare.repository.MachineAlertRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/engineer")
@CrossOrigin(
    origins = {
        "https://machcare-frontend.vercel.app",
        "http://localhost:4200",
        "http://localhost:8080",
        "http://localhost:9090"
    },
    allowCredentials = "true"
)
public class EngineerController {

    @Autowired
    private MachCareCoreService coreService;

    @Autowired
    private Jwtutil jwtUtil;
    @Autowired 
    private MaintenanceHistoryRepository historyRepo;
    @Autowired private MachineAlertRepository alertRepo;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AuditLogService auditLogService;
    
    // NEW: Added FaultLogService
    @Autowired 
    private FaultLogService faultService;

    @Autowired
    private FaultAnalysisService faultAnalysisService;

    
    
    
    
  
    // ==========================================
    // 1. TASK MANAGEMENT
    // ==========================================
    // GET /api/engineer/my-tasks
    @GetMapping("/my-tasks")
    public ResponseEntity<List<MaintenanceSchedule>> getMyDashboard(
            @RequestHeader("Authorization") String authHeader) {
        
        Long loggedInEmpId = jwtUtil.extractEmpId(authHeader);
        
        return ResponseEntity.ok(coreService.getMyTasks(loggedInEmpId));
    }

 // PUT /api/engineer/schedules/{scheduleId}/status
    @PutMapping("/schedules/{scheduleId}/status")
    public ResponseEntity<Map<String, Object>> updateTaskStatus(
            @PathVariable Long scheduleId,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam MaintenanceStatus status,
            @RequestParam(required = false) String remarks) { // 👉 1. Added the remarks parameter here!
        
        Long loggedInEmpId = jwtUtil.extractEmpId(authHeader);
        Employee actor = employeeRepository.findById(loggedInEmpId).orElse(null);
        String action = resolveTaskAction(status, remarks);

        if (jwtUtil.extractRoleId(authHeader) != 2) {
            logActivity(authHeader, actor, action, "FAILED", "ACCESS DENIED: Only Engineers can update tasks.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "ACCESS DENIED: Only Engineers can update tasks."));
        }

        try {
            coreService.updateScheduleStatus(scheduleId, loggedInEmpId, status, remarks);

            logActivity(authHeader, actor, action, "SUCCESS", "Status successfully updated to " + status.name());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Status successfully updated to " + status.name());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            logActivity(authHeader, actor, action, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    // POST /api/engineer/schedules/{scheduleId}/parts
    @PostMapping("/schedules/{scheduleId}/parts")
    public ResponseEntity<Map<String, Object>> requestParts(
            @PathVariable Long scheduleId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody List<PartRequestDTO> parts) {
        
        Long loggedInEmpId = jwtUtil.extractEmpId(authHeader);
        Employee actor = employeeRepository.findById(loggedInEmpId).orElse(null);

        if (jwtUtil.extractRoleId(authHeader) != 2) {
            logActivity(authHeader, actor, "Request Part", "FAILED", "ACCESS DENIED: Only Engineers can request parts.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "ACCESS DENIED: Only Engineers can request parts."));
        }

        try {
            Map<String, Object> result = coreService.requestParts(scheduleId, loggedInEmpId, parts);
            logActivity(authHeader, actor, "Request Part", "SUCCESS", (String) result.getOrDefault("message", "Parts requested successfully."));
            return ResponseEntity.ok(result);
        } catch (RuntimeException ex) {
            logActivity(authHeader, actor, "Request Part", "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @GetMapping("/schedules/{scheduleId}/raise-context")
    public ResponseEntity<?> getRaiseRequestContext(
            @PathVariable Long scheduleId,
            @RequestHeader("Authorization") String authHeader) {

        if (jwtUtil.extractRoleId(authHeader) != 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "ACCESS DENIED: Only Engineers can use raise requests."));
        }

        Long loggedInEmpId = jwtUtil.extractEmpId(authHeader);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", coreService.getRaiseRequestContext(scheduleId, loggedInEmpId)
        ));
    }

    @PostMapping("/schedules/{scheduleId}/raise-fault")
    public ResponseEntity<?> raiseFaultFromTask(
            @PathVariable Long scheduleId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody EngineerRaiseFaultRequestDTO request) {

        Long loggedInEmpId = jwtUtil.extractEmpId(authHeader);
        Employee actor = employeeRepository.findById(loggedInEmpId).orElse(null);

        if (jwtUtil.extractRoleId(authHeader) != 2) {
            logActivity(authHeader, actor, "Raise Fault", "FAILED", "ACCESS DENIED: Only Engineers can raise faults from tasks.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "ACCESS DENIED: Only Engineers can raise faults from tasks."));
        }

        String engineerName = jwtUtil.extractName(authHeader);
        try {
            Map<String, Object> response = coreService.raiseFaultFromSchedule(scheduleId, loggedInEmpId, engineerName, request);
            logActivity(authHeader, actor, "Raise Fault", "SUCCESS", (String) response.getOrDefault("message", "Fault raised successfully."));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException ex) {
            logActivity(authHeader, actor, "Raise Fault", "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/schedules/{scheduleId}/support-request")
    public ResponseEntity<?> requestAdditionalSupport(
            @PathVariable Long scheduleId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SupportRequestDTO request) {

        Long loggedInEmpId = jwtUtil.extractEmpId(authHeader);
        Employee actor = employeeRepository.findById(loggedInEmpId).orElse(null);

        if (jwtUtil.extractRoleId(authHeader) != 2) {
            logActivity(authHeader, actor, "Request Support", "FAILED", "ACCESS DENIED: Only Engineers can request support.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "ACCESS DENIED: Only Engineers can request support."));
        }

        try {
            Map<String, Object> response = coreService.requestAdditionalSupport(scheduleId, loggedInEmpId, request);
            logActivity(authHeader, actor, "Request Support", "SUCCESS", (String) response.getOrDefault("message", "Support requested successfully."));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException ex) {
            logActivity(authHeader, actor, "Request Support", "FAILED", ex.getMessage());
            throw ex;
        }
    }
    
    
    
    
 // ==========================================
    // ENGINEER DASHBOARD
    // ==========================================
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getEngineerDashboard(
            @RequestHeader("Authorization") String token) {
            
        if (jwtUtil.extractRoleId(token) != 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long empId = jwtUtil.extractEmpId(token);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("role", "ENGINEER");
        response.put("dashboardData", coreService.getEngineerDashboardStats(empId));
        
        return ResponseEntity.ok(response);
    }

    
 // ==========================================
    // GENERATE ALERT FROM FAULT ANALYSIS
    // ==========================================
    @PostMapping("/alerts/generate/{analysisId}")
    public ResponseEntity<Map<String, Object>> generateAlert(
            @PathVariable Long analysisId,
            @RequestHeader("Authorization") String token) {
            
        // Strict Check: Only Engineer (Role 2) can access this specific endpoint
        if (jwtUtil.extractRoleId(token) != 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Engineers can generate alerts here."));
        }

        // Reusing the exact same service logic!
        MachineAlert newAlert = coreService.generateAlertFromAnalysis(analysisId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Alert generated successfully by Engineer for Machine: " + newAlert.getMachineId());
        response.put("alertId", newAlert.getAlertId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    // ==========================================
    // 2. FAULT LOG MANAGEMENT (NEW)
    // ==========================================
    // GET /api/engineer/faults
    @GetMapping("/faults")
    public ResponseEntity<?> viewFaults(@RequestHeader("Authorization") String token) {
        
        // Strict Check: Only Engineer (Role 2)
        if (jwtUtil.extractRoleId(token) != 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Engineers can view this dashboard."));
        }

        return ResponseEntity.ok(Map.of("success", true, "data", faultService.getAllFaultLogs()));
    }

    @GetMapping("/faults/pending")
    public ResponseEntity<?> viewPendingFaultQueue(@RequestHeader("Authorization") String token) {
        if (jwtUtil.extractRoleId(token) != 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure("ACCESS DENIED: Only Engineers can view the pending queue.", null));
        }

        return ResponseEntity.ok(
                ApiResponse.success("Pending fault queue fetched successfully.", faultService.getEngineerPendingQueue())
        );
    }

    @PostMapping("/faults/{faultId}/analyze")
    public ResponseEntity<?> analyzeFault(
            @PathVariable String faultId,
            @RequestHeader("Authorization") String token) {
        Long empId = jwtUtil.extractEmpId(token);
        Employee actor = employeeRepository.findById(empId).orElse(null);

        if (jwtUtil.extractRoleId(token) != 2) {
            logActivity(token, actor, "Analyze Fault", "FAILED", "ACCESS DENIED: Only Engineers can analyze faults.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure("ACCESS DENIED: Only Engineers can analyze faults.", null));
        }

        try {
            Object analysis = faultAnalysisService.generateAnalysis(faultId);
            logActivity(token, actor, "Analyze Fault", "SUCCESS", "Fault analysis completed successfully.");
            return ResponseEntity.ok(
                    ApiResponse.success("Fault analysis completed successfully.", analysis)
            );
        } catch (RuntimeException ex) {
            logActivity(token, actor, "Analyze Fault", "FAILED", ex.getMessage());
            throw ex;
        }
    }
 // ==========================================
    // MAINTENANCE HISTORY (ENGINEER)
    // ==========================================
    @GetMapping("/history")
    public ResponseEntity<?> getAllHistory(
            @RequestHeader("Authorization") String token) {

        // Strict Check: Only Engineer (Role 2)
        if (jwtUtil.extractRoleId(token) != 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Engineers can view history here."));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", historyRepo.findAll()
        ));
    }
    @GetMapping("/alerts")
    public ResponseEntity<?> getAllAlerts() {

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", alertRepo.findAll()
        ));
    }
    
    
 // ==========================================
    // 1. TASK MANAGEMENT
    // ==========================================
    // 👉 1. Changed endpoint to match Angular perfectly!
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getMyTasks(
            @RequestHeader("Authorization") String authHeader) {
        
        Long loggedInEmpId = jwtUtil.extractEmpId(authHeader);
        List<MaintenanceSchedule> myTasks = coreService.getMyTasks(loggedInEmpId);
        
        // 👉 2. Wrapped the list in a Map so Angular can read 'response.data'
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", myTasks);
        
        return ResponseEntity.ok(response);
    }

    private void logActivity(String token, Employee actor, String action, String status, String message) {
        Long empId = actor != null ? actor.getEmpId() : null;
        String name = actor != null ? actor.getName() : null;
        String email = actor != null ? actor.getEmail() : null;
        Integer roleId = actor != null ? actor.getRoleId() : null;

        try {
            if (token != null && empId == null) {
                empId = jwtUtil.extractEmpId(token);
            }
            if (token != null && name == null) {
                name = jwtUtil.extractName(token);
            }
            if (token != null && roleId == null) {
                roleId = jwtUtil.extractRoleId(token);
            }
            if (email == null && empId != null) {
                email = employeeRepository.findById(empId).map(Employee::getEmail).orElse(null);
            }
        } catch (Exception ignored) {
        }

        auditLogService.logActivity(empId, name, email, roleId, action, status, message);
    }

    private String resolveTaskAction(MaintenanceStatus status, String remarks) {
        if (status == MaintenanceStatus.Completed) {
            return "Complete Task";
        }
        if (status == MaintenanceStatus.In_progress) {
            return (remarks == null || remarks.trim().isEmpty()) ? "Start Work" : "Update Progress";
        }
        return "Update Progress";
    }
    
    
    
    
}




