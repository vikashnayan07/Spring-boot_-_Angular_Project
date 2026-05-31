package com.tcs.Machcare.controller;

import com.tcs.Machcare.dto.CreatePartRequestDTO;
import com.tcs.Machcare.dto.CsvUploadResponse;
import com.tcs.Machcare.dto.PartDTO;
import com.tcs.Machcare.dto.RegisterRequest;
import com.tcs.Machcare.dto.StockUpdateDTO;
import com.tcs.Machcare.dto.UsePartRequestDTO;

import org.springframework.transaction.annotation.Transactional;

import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.entity.MaintenanceSchedule;
import com.tcs.Machcare.entity.Part;
import com.tcs.Machcare.entity.PartUsage;
import com.tcs.Machcare.exception.CsvValidationException;
import com.tcs.Machcare.exception.InventoryException;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.repository.MaintenanceHistoryRepository;
import com.tcs.Machcare.util.Jwtutil;
import com.tcs.Machcare.service.AuthenticatedUserService;
import com.tcs.Machcare.service.AuthenticatedUserService.AuthenticatedUser;
import com.tcs.Machcare.service.AuthService;
import com.tcs.Machcare.service.AuditLogService;
import com.tcs.Machcare.service.AssetLifecycleService;
import com.tcs.Machcare.service.MachCareCoreService;
import com.tcs.Machcare.service.FaultLogService;
import com.tcs.Machcare.repository.MachineAlertRepository;
import com.tcs.Machcare.repository.FaultAnalysisRepository;
import com.tcs.Machcare.repository.FaultLogRepository;
import com.tcs.Machcare.entity.MachineAlert;
import com.tcs.Machcare.entity.MaintenanceHistory;
import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.entity.FaultAnalysis;
import com.tcs.Machcare.repository.LoginRepository;
import com.tcs.Machcare.repository.PartRepository;
import com.tcs.Machcare.repository.PartUsageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(
    origins = {
        "https://machcare-frontend.vercel.app",
        "http://localhost:4200",
        "http://localhost:8080",
        "http://localhost:9090"
    },
    allowCredentials = "true"
)
public class AdminController {

    @Autowired private MachCareCoreService coreService;
    @Autowired private AuthService authService; 
    @Autowired private EmployeeRepository empRepo; 
    @Autowired private Jwtutil jwtUtil;
    @Autowired private MaintenanceHistoryRepository historyRepo;
    @Autowired private PartRepository partRepository;
    @Autowired private PartUsageRepository partUsageRepository;
    @Autowired private AuditLogService auditLogService;
    @Autowired private AssetLifecycleService assetLifecycleService;
    
    // NEW: Added FaultLogService
    @Autowired private FaultLogService faultService; 
    @Autowired private AuthenticatedUserService authenticatedUserService;
    
    
    
 // 1. ADMIN ONLY: CREATE PART
    // ==========================================
    @PostMapping("/parts")
    public ResponseEntity<?> createPart(
            @RequestBody CreatePartRequestDTO dto,
            @RequestHeader("Authorization") String token) {

        // Strict Check: Only Admin (Role 1)
        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins can create parts."));
        }

        validatePartRequest(dto);

        Part part = new Part();
        part.setPartName(dto.getPartName());
        part.setCategoryId(1L);
        part.setMachineId(dto.getMachineId());
        part.setMinStock(dto.getMinStock());
        part.setCurrentStock(dto.getCurrentStock());
        part.setManufactureDate(dto.getManufactureDate());
        part.setExpiryDate(dto.getExpiryDate());
        part.setWarrantyExpiryDate(dto.getWarrantyExpiryDate());
        part.setShelfLifeDays(dto.getShelfLifeDays());
        part.setConditionStatus(dto.getConditionStatus());

        Long newPartId = assetLifecycleService.refreshPartLifecycle(partRepository.save(part)).getPartId();

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "message", "New part created successfully",
            "partId", newPartId
        ));
    }

    private void validatePartRequest(CreatePartRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Part details are required.");
        }
        if (dto.getManufactureDate() != null && dto.getExpiryDate() != null
                && dto.getExpiryDate().isBefore(dto.getManufactureDate())) {
            throw new IllegalArgumentException("expiryDate must be on or after manufactureDate.");
        }
        if (dto.getShelfLifeDays() != null && dto.getShelfLifeDays() < 0) {
            throw new IllegalArgumentException("shelfLifeDays must be 0 or greater.");
        }
    }

    
    
    
 // 2. ADMIN ONLY: CORRECT STOCK
    // ==========================================
    @PutMapping("/parts/{partId}/stock")
    public ResponseEntity<?> correctStock(
            @PathVariable Long partId, 
            @RequestBody StockUpdateDTO dto,
            @RequestHeader("Authorization") String token) {

        // Strict Check: Only Admin (Role 1)
        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins can manually correct stock."));
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new InventoryException("Part not found"));

        part.setCurrentStock(dto.getNewStock());
        assetLifecycleService.refreshPartLifecycle(partRepository.save(part));
        int clearedRecommendations = coreService.resolveReorderRecommendationsForPart(part.getPartId(), part.getPartName());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Stock updated to " + dto.getNewStock() + " for Part ID: " + partId,
            "clearedRecommendations", clearedRecommendations
        ));
    }

    
    @PostMapping("/parts/use")
    @Transactional
    public ResponseEntity<?> usePart(
            @RequestBody UsePartRequestDTO dto,
            @RequestHeader("Authorization") String token) {

        // 1. Extract the ID of the person making the request
        Long loggedInEmpId = jwtUtil.extractEmpId(token);

        // 👉 THE FIX: Fetch the user and block anyone who isn't an Admin (Role ID 1)
        Employee requestingUser = empRepo.findById(loggedInEmpId)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        if (requestingUser.getRoleId() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "ACCESS DENIED: Only Admins can use or assign parts."
            ));
        }

        // 2. Fetch the part (Only reachable if the user is an Admin!)
        Part part = partRepository.findById(dto.getPartId())
                .orElseThrow(() -> new InventoryException("Part not found"));

        if (part.getCurrentStock() < dto.getQuantity()) {
            throw new InventoryException("Insufficient stock");
        }

        // 3. Update stock
        part.setCurrentStock(part.getCurrentStock() - dto.getQuantity());
        partRepository.save(part);

        // 4. Record usage
        PartUsage usage = new PartUsage();
        usage.setPartId(dto.getPartId());
        usage.setEmpId(loggedInEmpId); 
        usage.setQtyAssigned(dto.getQuantity());
        usage.setLastUpdated(LocalDateTime.now());

        partUsageRepository.save(usage);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Part used successfully. Remaining stock: " + part.getCurrentStock()
        ));
    }
    
    
    @GetMapping("/employees")
    public ResponseEntity<?> getAllEmployees() {
        // Assuming your repository is named empRepo. Change it if it's different!
        return ResponseEntity.ok(empRepo.findAll()); 
    }

    @GetMapping("/employees/active")
    public ResponseEntity<?> getActiveEmployees() {
        return ResponseEntity.ok(empRepo.findByRoleIdAndIsActiveTrue(2));
    }
    
    
    // ==========================================
    @PostMapping("/add-employee")
    public ResponseEntity<Map<String, Object>> addNewEmployee(
            @RequestBody RegisterRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        // 👉 THE FIX: Block Admin creation right at the door
        if (String.valueOf(request.getRole()).equalsIgnoreCase("ADMIN")) {
            Employee actor = empRepo.findById(jwtUtil.extractEmpId(authHeader)).orElse(null);
            auditLogService.logActivity(
                    actor != null ? actor.getEmpId() : null,
                    actor != null ? actor.getName() : jwtUtil.extractName(authHeader),
                    actor != null ? actor.getEmail() : null,
                    actor != null ? actor.getRoleId() : jwtUtil.extractRoleId(authHeader),
                    "Add Employee",
                    "FAILED",
                    "SECURITY BLOCK: Admins are not permitted to create additional Admin accounts.");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "SECURITY BLOCK: Admins are not permitted to create additional Admin accounts.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        Long adminId = jwtUtil.extractEmpId(authHeader);
        
        Employee adminUser = empRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        if (adminUser.getRoleId() != 1) {
            throw new RuntimeException("ACCESS DENIED: Only Admins can register new employees.");
        }

        try {
            authService.registerEmployee(request);

            auditLogService.logActivity(
                adminUser.getEmpId(),
                adminUser.getName(),
                adminUser.getEmail(),
                adminUser.getRoleId(),
                "Add Employee",
                "SUCCESS",
                "New employee " + request.getName() + " securely added to the system.");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "New employee " + request.getName() + " securely added to the system.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            auditLogService.logActivity(
                adminUser.getEmpId(),
                adminUser.getName(),
                adminUser.getEmail(),
                adminUser.getRoleId(),
                "Add Employee",
                "FAILED",
                ex.getMessage());
            throw ex;
        }
    }
    
 // Inject these repositories at the top of your service class
    @Autowired private MachineAlertRepository alertRepo;
    @Autowired private FaultAnalysisRepository analysisRepo;
    @Autowired private FaultLogRepository faultLogRepo;


 // ==========================================
    // GENERATE ALERT FROM FAULT ANALYSIS (ADMIN)
    // ==========================================
    @PostMapping("/alerts/generate/{analysisId}")
    public ResponseEntity<Map<String, Object>> generateAlert(
            @PathVariable Long analysisId,
            @RequestHeader("Authorization") String token) {
            
        // Strict Check: Only Admin (Role 1)
        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED"));
        }

        MachineAlert newAlert = coreService.generateAlertFromAnalysis(analysisId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Alert generated successfully for Machine: " + newAlert.getMachineId());
        response.put("alertId", newAlert.getAlertId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    
    
    
    
 // ==========================================
    // ADMIN DASHBOARD
    // ==========================================
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard(
            @RequestHeader("Authorization") String token) {
            
        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("role", "ADMIN");
        response.put("dashboardData", coreService.getAdminDashboardStats());
        
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. TASK MANAGEMENT
    // ==========================================
    @PostMapping("/alerts/{alertId}/auto-assign")
    public ResponseEntity<Map<String, Object>> autoAssignTask(
            @PathVariable Long alertId,
            @RequestHeader("Authorization") String authHeader) {
        
        Long adminEmpId = jwtUtil.extractEmpId(authHeader);
        Employee adminUser = empRepo.findById(adminEmpId).orElse(null);

        try {
            MaintenanceSchedule newSchedule = coreService.autoAssignAlert(alertId, adminEmpId);

            auditLogService.logActivity(
                    adminUser != null ? adminUser.getEmpId() : adminEmpId,
                    adminUser != null ? adminUser.getName() : jwtUtil.extractName(authHeader),
                    adminUser != null ? adminUser.getEmail() : null,
                    adminUser != null ? adminUser.getRoleId() : jwtUtil.extractRoleId(authHeader),
                    "Auto Assign",
                    "SUCCESS",
                    "Task successfully assigned to least loaded engineer.");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task successfully assigned to least loaded engineer.");
            response.put("scheduleId", newSchedule.getScheduleId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            auditLogService.logActivity(
                    adminUser != null ? adminUser.getEmpId() : adminEmpId,
                    adminUser != null ? adminUser.getName() : jwtUtil.extractName(authHeader),
                    adminUser != null ? adminUser.getEmail() : null,
                    adminUser != null ? adminUser.getRoleId() : jwtUtil.extractRoleId(authHeader),
                    "Auto Assign",
                    "FAILED",
                    ex.getMessage());
            throw ex;
        }
    }

    @GetMapping("/schedules/all")
    public ResponseEntity<List<MaintenanceSchedule>> viewAllTasks(
            @RequestHeader("Authorization") String authHeader) {
        
        Long adminEmpId = jwtUtil.extractEmpId(authHeader);
        return ResponseEntity.ok(coreService.viewAllSchedules(adminEmpId));
    }

    // ==========================================
    // 3. FAULT LOG MANAGEMENT (NEW)
    // ==========================================
    @PostMapping("/faults/upload")
    public ResponseEntity<?> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {

        try {
            AuthenticatedUser admin = authenticatedUserService.requireRole(
                    token,
                    1,
                    "ACCESS DENIED: Only Admins can upload CSVs here."
            );
            CsvUploadResponse response = faultService.uploadCsv(file, admin);
            auditLogService.logActivity(
                admin.getEmpId(),
                admin.getName(),
                empRepo.findById(admin.getEmpId()).map(Employee::getEmail).orElse(null),
                admin.getRoleId(),
                "CSV Upload",
                "SUCCESS",
                "CSV uploaded successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (CsvValidationException ex) {
            AuthenticatedUser admin = authenticatedUserService.fromToken(token);
            auditLogService.logActivity(
                admin.getEmpId(),
                admin.getName(),
                empRepo.findById(admin.getEmpId()).map(Employee::getEmail).orElse(null),
                admin.getRoleId(),
                "CSV Upload",
                "FAILED",
                ex.getMessage());
            return ResponseEntity.badRequest().body(
                    CsvUploadResponse.invalid(ex.getMessage(), ex.getMissingHeaders(), ex.getErrors())
            );
        } catch (RuntimeException ex) {
            AuthenticatedUser admin = authenticatedUserService.fromToken(token);
            auditLogService.logActivity(
                admin.getEmpId(),
                admin.getName(),
                empRepo.findById(admin.getEmpId()).map(Employee::getEmail).orElse(null),
                admin.getRoleId(),
                "CSV Upload",
                "FAILED",
                ex.getMessage());
            throw ex;
        }
    }

    @GetMapping("/faults")
    public ResponseEntity<?> viewAllFaults(@RequestHeader("Authorization") String token) {
        // Strict Check: Only Admin (Role 1)
        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(Map.of("success", true, "data", faultService.getAllFaultLogs()));
    }
 // ==========================================
    // MAINTENANCE HISTORY (ADMIN)
    // ==========================================
 // ==========================================
    // MAINTENANCE HISTORY (ADMIN & ENGINEER)
    // ==========================================
    @GetMapping("/history")
    public ResponseEntity<?> getAllHistory(
            @RequestHeader("Authorization") String token) {

        int roleId = jwtUtil.extractRoleId(token);

        // 👉 STRICT CHECK: Block only if they are NOT an Admin (1) AND NOT an Engineer (2)
        if (roleId != 1 && roleId != 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins and Engineers can view history here."));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", historyRepo.findAll()
        ));
    }

    @GetMapping("/inventory/reorder-recommendations")
    public ResponseEntity<?> getReorderRecommendations(
            @RequestHeader("Authorization") String token) {

        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins can view reorder recommendations."));
        }

        return ResponseEntity.ok(coreService.getReorderRecommendations());
    }
    
    
    
 // ==========================================
     // ADMIN ONLY: DELETE EMPLOYEE (Soft Delete)
 // Make sure you have this injected at the top of your controller!
     @Autowired private LoginRepository loginRepo;

    // ==========================================
    // ADMIN ONLY: DELETE EMPLOYEE (Soft Delete)
    // ==========================================

    @Transactional //  Add this so both deletes happen safely together
    @DeleteMapping("/employees/{empId}")
    public ResponseEntity<?> deleteEmployee(
            @PathVariable Long empId,
            @RequestHeader("Authorization") String token) {

        // Strict Check: Only Admin (Role 1)
        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins can delete employees."));
        }

        if (!empRepo.existsById(empId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "Employee not found."));
        }

        Employee employee = empRepo.findById(empId)
                .orElseThrow(() -> new IllegalStateException("Employee not found."));
        employee.setActive(false);
        employee.setSuspensionEndDate(null);
        empRepo.save(employee);

        Employee admin = empRepo.findById(jwtUtil.extractEmpId(token)).orElse(null);
        auditLogService.logActivity(
                admin != null ? admin.getEmpId() : null,
                admin != null ? admin.getName() : null,
                admin != null ? admin.getEmail() : null,
                admin != null ? admin.getRoleId() : 1,
                "Deactivate Employee",
                "SUCCESS",
                "Employee ID " + empId + " was deactivated and preserved for history.");

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Employee ID " + empId + " has been deactivated and kept for audit history."
        ));
    }

    // ==========================================
 // ==========================================
 // ==========================================
    // ADMIN ONLY: DISABLE ACCOUNT
    // ==========================================
    @PutMapping("/disable-account")
    public ResponseEntity<?> disableAccount(
            @RequestParam Long empId, 
            @RequestParam int days,
            @RequestHeader("Authorization") String token) { // ✅ Force them to pass a token

        // Strict Check: Only Admin (Role 1)
        if (jwtUtil.extractRoleId(token) != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "ACCESS DENIED: Only Admins can disable accounts."));
        }

        authService.disableAccount(empId, days);
        
        return ResponseEntity.ok(Map.of(
            "success", true, 
            "message", "Account suspended for " + days + " days"
        ));
    }
    
    
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAllAlerts() {
        
        // 👉 CHANGE THIS LINE: Only fetch alerts with no assigned engineer
        List<MachineAlert> unassignedAlerts = alertRepo.findByEmpIdIsNull();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", unassignedAlerts);
        
        return ResponseEntity.ok(response);
    }

private PartDTO mapToDTO(Part part) {
    PartDTO dto = new PartDTO();
    dto.setPartId(part.getPartId());
    dto.setPartName(part.getPartName());
    dto.setCurrentStock(part.getCurrentStock());
    return dto;
}
}
