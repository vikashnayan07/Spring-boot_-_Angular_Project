package com.tcs.Machcare.service;

import com.tcs.Machcare.dto.PartRequestDTO;
import com.tcs.Machcare.dto.EngineerRaiseFaultRequestDTO;
import com.tcs.Machcare.dto.FaultDTO;
import com.tcs.Machcare.dto.SupportRequestDTO;
import com.tcs.Machcare.exception.ConflictException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import java.util.HashMap;
import com.tcs.Machcare.entity.*;
import com.tcs.Machcare.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import java.time.LocalTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MachCareCoreService {

    private static final String REORDER_RECOMMENDATION_PREFIX = "Type: REORDER_RECOMMENDATION";

    private static final DateTimeFormatter REORDER_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConcurrentHashMap<Long, ShortageRecommendation> reorderRecommendations = new ConcurrentHashMap<>();
    private final AtomicLong reorderSequence = new AtomicLong(0);

    @Autowired private MachineAlertRepository alertRepo;
    @Autowired private MaintenanceScheduleRepository scheduleRepo;
    @Autowired private EmployeeRepository empRepo;
    @Autowired private MachineRepository machineRepo;
    @Autowired private PartRepository partRepo;
    @Autowired private PartUsageRepository partUsageRepo;
    @Autowired private MaintenanceHistoryRepository historyRepo;
    @Autowired private FaultAnalysisRepository analysisRepo;
    @Autowired private FaultLogRepository faultLogRepo;
    @Autowired private RiskBasedAlertService riskBasedAlertService;
    @Autowired private RaiseRequestRepository raiseRequestRepo;
    @Autowired private FaultLogService faultLogService;
    @Autowired private AssetLifecycleService assetLifecycleService;

    // ==========================================
    // 1. ADMIN DASHBOARD
    // ==========================================
    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalFaultsLogged", faultLogRepo.count());
        stats.put("totalMachineAlerts", alertRepo.count());
        stats.put("unassignedAlerts", alertRepo.countByEmpIdIsNull());

        List<Machine> machines = machineRepo.findAll();
        long serviceOverdue = machines.stream()
            .filter(machine -> "SERVICE_OVERDUE".equals(assetLifecycleService.computeMachineLifecycleStatus(machine)))
            .count();
        long outOfWarranty = machines.stream()
            .filter(machine -> "OUT_OF_WARRANTY".equals(assetLifecycleService.computeMachineLifecycleStatus(machine)))
            .count();
        long maintenanceDue = machines.stream()
            .filter(machine -> "MAINTENANCE_DUE".equals(assetLifecycleService.computeMachineHealthStatus(machine)))
            .count();
        long atRisk = machines.stream()
            .filter(machine -> "AT_RISK".equals(assetLifecycleService.computeMachineHealthStatus(machine)))
            .count();

        List<Part> parts = partRepo.findAll();
        long partsExpired = parts.stream()
            .filter(part -> "EXPIRED".equals(assetLifecycleService.computePartLifecycleStatus(part)))
            .count();
        long partsExpiringSoon = parts.stream()
            .filter(part -> "EXPIRING_SOON".equals(assetLifecycleService.computePartLifecycleStatus(part)))
            .count();
        long partsOutOfWarranty = parts.stream()
            .filter(part -> "OUT_OF_WARRANTY".equals(assetLifecycleService.computePartLifecycleStatus(part)))
            .count();
        long partsEndOfLife = parts.stream()
            .filter(part -> "END_OF_LIFE".equals(assetLifecycleService.computePartLifecycleStatus(part)))
            .count();
        long partsOutOfStock = parts.stream()
            .filter(part -> "OUT_OF_STOCK".equals(assetLifecycleService.computePartConditionStatus(part)))
            .count();
        long partsReorder = parts.stream()
            .filter(part -> "REORDER".equals(assetLifecycleService.computePartConditionStatus(part)))
            .count();

        stats.put("machinesServiceOverdue", serviceOverdue);
        stats.put("machinesOutOfWarranty", outOfWarranty);
        stats.put("machinesMaintenanceDue", maintenanceDue);
        stats.put("machinesAtRisk", atRisk);
        stats.put("partsExpired", partsExpired);
        stats.put("partsExpiringSoon", partsExpiringSoon);
        stats.put("partsOutOfWarranty", partsOutOfWarranty);
        stats.put("partsEndOfLife", partsEndOfLife);
        stats.put("partsOutOfStock", partsOutOfStock);
        stats.put("partsReorder", partsReorder);
        
        return stats;
    }

    // ==========================================
    // 2. ENGINEER DASHBOARD
    // ==========================================
    public Map<String, Object> getEngineerDashboardStats(Long empId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<MaintenanceSchedule> myTasks = getMyTasks(empId);
        
        long pending = myTasks.stream().filter(t -> t.getStatus() == MaintenanceStatus.Pending).count();
        long completed = myTasks.stream().filter(t -> t.getStatus() == MaintenanceStatus.Completed).count();

        stats.put("totalAssignedTasks", myTasks.size());
        stats.put("pendingTasks", pending);
        stats.put("completedTasks", completed);

        List<String> assignedMachineIds = myTasks.stream()
            .map(MaintenanceSchedule::getMachineId)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();

        List<Machine> assignedMachines = assignedMachineIds.isEmpty()
            ? List.of()
            : machineRepo.findAllById(assignedMachineIds).stream()
                .map(assetLifecycleService::applyMachineStatuses)
                .toList();

        long warrantyExpiring = assignedMachines.stream()
            .filter(machine -> "EXPIRING_SOON".equals(machine.getWarrantyStatus()))
            .count();
        long warrantyExpired = assignedMachines.stream()
            .filter(machine -> "EXPIRED".equals(machine.getWarrantyStatus()))
            .count();

        List<Part> assignedParts = assignedMachineIds.isEmpty()
            ? List.of()
            : partRepo.findByMachineIdIn(assignedMachineIds);

        long partsExpiringSoon = assignedParts.stream()
            .filter(part -> "EXPIRING_SOON".equals(assetLifecycleService.computePartLifecycleStatus(part)))
            .count();
        long partsExpired = assignedParts.stream()
            .filter(part -> "EXPIRED".equals(assetLifecycleService.computePartLifecycleStatus(part)))
            .count();
        long replacementRequired = assignedParts.stream()
            .filter(part -> "END_OF_LIFE".equals(assetLifecycleService.computePartLifecycleStatus(part)))
            .count();

        stats.put("machineWarrantyExpiring", warrantyExpiring);
        stats.put("machineWarrantyExpired", warrantyExpired);
        stats.put("partsExpiringSoon", partsExpiringSoon);
        stats.put("partsExpired", partsExpired);
        stats.put("replacementRequired", replacementRequired);
        
        return stats;
    }

    // ==========================================
    // 3. OPERATOR DASHBOARD
    // ==========================================
    public Map<String, Object> getOperatorDashboardStats(Long empId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("myReportedFaults", faultLogRepo.countByReportedBy(empId));
        stats.put("factoryTotalFaults", faultLogRepo.count());
        stats.put("faultsRaisedToday", faultLogRepo.countByFaultDate(LocalDate.now()));

        List<Machine> machines = machineRepo.findAll().stream()
            .map(assetLifecycleService::applyMachineStatuses)
            .toList();

        long criticalMaintenance = machines.stream()
            .filter(machine -> "MAINTENANCE_DUE".equals(machine.getHealthStatus())
                || "OFFLINE".equals(machine.getHealthStatus())
                || "SERVICE_OVERDUE".equals(machine.getLifecycleStatus()))
            .count();
        long warrantyExpired = machines.stream()
            .filter(machine -> "EXPIRED".equals(machine.getWarrantyStatus()))
            .count();
        long warrantyExpiring = machines.stream()
            .filter(machine -> "EXPIRING_SOON".equals(machine.getWarrantyStatus()))
            .count();

        stats.put("criticalMaintenanceRequired", criticalMaintenance);
        stats.put("machineWarrantyExpired", warrantyExpired);
        stats.put("machineWarrantyExpiring", warrantyExpiring);
        
        return stats;
    }

    // ==========================================
    // GENERATE ALERT FROM ANALYSIS
    // ==========================================
    public MachineAlert generateAlertFromAnalysis(Long analysisId) {
        FaultAnalysis analysis = analysisRepo.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Analysis not found with ID: " + analysisId));

        MachineAlert existingAlert = alertRepo.findByLinkedAnalysisId(analysisId).orElse(null);
        if (existingAlert != null) {
            throw new ConflictException("Alert already exists for analysis ID: " + analysisId);
        }

        FaultLog faultLog = faultLogRepo.findById(analysis.getFaultId())
                .orElseThrow(() -> new RuntimeException("Original Fault Log not found with ID: " + analysis.getFaultId()));

        MachineAlert generatedAlert = riskBasedAlertService.evaluateAndGenerate(analysis, faultLog);
        if (generatedAlert == null) {
            throw new IllegalArgumentException("Alert not generated because this analysis does not meet risk conditions.");
        }

        return generatedAlert;
    }

    // ==========================================
    // ADMIN FUNCTIONS
    // ==========================================

    @Transactional
    public MaintenanceSchedule autoAssignAlert(Long alertId, Long adminEmpId) {
        Employee admin = empRepo.findById(adminEmpId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
        
        if (admin.getRoleId() != 1) { 
            throw new RuntimeException("ACCESS DENIED: Only Admins can assign tasks.");
        }

        Long bestEngineerId = getLeastLoadedEngineer();

        MachineAlert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setEmpId(bestEngineerId);
        alertRepo.save(alert);

        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setAlertId(alert.getAlertId());
        schedule.setMachineId(alert.getMachineId());
        schedule.setEmpId(bestEngineerId);
        schedule.setScheduleDate(LocalDate.now());
        schedule.setStatus(MaintenanceStatus.Pending); 

        MaintenanceSchedule savedSchedule = scheduleRepo.save(schedule);
        upsertMaintenanceHistory(
                savedSchedule,
                bestEngineerId,
                MaintenanceStatus.Pending,
                "Work order created and assigned to engineer.",
                false
        );

        return savedSchedule;
    }

    public List<MaintenanceSchedule> viewAllSchedules(Long adminEmpId) {
        Employee admin = empRepo.findById(adminEmpId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
                
        if (admin.getRoleId() != 1) {
            throw new RuntimeException("ACCESS DENIED: Only Admins can view all schedules.");
        }
        return scheduleRepo.findAll();
    }

    private Long getLeastLoadedEngineer() {
        List<Employee> engineers = empRepo.findByRoleIdAndIsActiveTrue(2);
        
        if (engineers.isEmpty()) {
            throw new RuntimeException("Task cannot be assigned to inactive or suspended employee.");
        }

        Long bestEngineerId = null;
        int minTasks = Integer.MAX_VALUE;

        for (Employee eng : engineers) {
            ensureAssignableEngineer(eng);
            int activeTasks = scheduleRepo.countByEmpIdAndStatusNot(eng.getEmpId(), MaintenanceStatus.Completed);
            
            if (activeTasks < minTasks) {
                minTasks = activeTasks;
                bestEngineerId = eng.getEmpId();
            }
        }
        return bestEngineerId;
    }

    private void ensureAssignableEngineer(Employee engineer) {
        if (engineer == null || !engineer.isActive() || engineer.getSuspensionEndDate() != null) {
            throw new RuntimeException("Task cannot be assigned to inactive or suspended employee.");
        }
    }

    // ==========================================
    // ENGINEER FUNCTIONS
    // ==========================================

    public List<MaintenanceSchedule> getMyTasks(Long loggedInEmpId) {
        List<MaintenanceSchedule> schedules = scheduleRepo.findByEmpId(loggedInEmpId);
        schedules.forEach(this::applyMachineLifecycleToSchedule);
        return schedules;
    }

    private void applyMachineLifecycleToSchedule(MaintenanceSchedule schedule) {
        if (schedule == null || isBlank(schedule.getMachineId())) {
            return;
        }
        Machine machine = machineRepo.findById(schedule.getMachineId()).orElse(null);
        if (machine == null) {
            return;
        }
        assetLifecycleService.applyMachineStatuses(machine);
        schedule.setMachineHealthStatus(machine.getHealthStatus());
        schedule.setMachineLifecycleStatus(machine.getLifecycleStatus());
        schedule.setMachineWarrantyStatus(machine.getWarrantyStatus());
    }

    public Map<String, Object> getRaiseRequestContext(Long scheduleId, Long loggedInEmpId) {
        MaintenanceSchedule schedule = validateStartedEngineerSchedule(scheduleId, loggedInEmpId);
        Machine machine = machineRepo.findById(schedule.getMachineId())
                .orElseThrow(() -> new RuntimeException("Machine not found for this work order."));

        Map<String, Object> data = new HashMap<>();
        data.put("scheduleId", schedule.getScheduleId());
        data.put("machineId", schedule.getMachineId());
        data.put("machineName", machine.getMachineName());
        data.put("alertId", schedule.getAlertId());
        data.put("nextFaultId", faultLogService.getNextFaultId());
        data.put("currentDate", LocalDate.now().toString());
        data.put("currentTime", LocalTime.now().withNano(0).toString());
        data.put("faultTypes", machineFaultTypes(machine));
        return data;
    }

    @Transactional
    public Map<String, Object> raiseFaultFromSchedule(
            Long scheduleId,
            Long loggedInEmpId,
            String engineerName,
            EngineerRaiseFaultRequestDTO request) {

        MaintenanceSchedule schedule = validateStartedEngineerSchedule(scheduleId, loggedInEmpId);
        Machine machine = machineRepo.findById(schedule.getMachineId())
                .orElseThrow(() -> new RuntimeException("Machine not found for this work order."));

        if (request == null || isBlank(request.getFaultType())) {
            throw new IllegalArgumentException("Fault type is required.");
        }
        if (isBlank(request.getSeverity())) {
            throw new IllegalArgumentException("Severity is required.");
        }

        String selectedType = request.getFaultType().trim();
        String description;
        if ("Other".equalsIgnoreCase(selectedType)) {
            if (isBlank(request.getRemark())) {
                throw new IllegalArgumentException("Remark is required when fault type is Other.");
            }
            description = request.getRemark().trim();
        } else {
            boolean allowed = machineFaultTypes(machine).stream()
                    .anyMatch(type -> type.equalsIgnoreCase(selectedType));
            if (!allowed) {
                throw new IllegalArgumentException("Selected fault type is not configured for this machine.");
            }
            description = selectedType;
        }

        FaultDTO dto = new FaultDTO();
        dto.setMachineId(schedule.getMachineId());
        dto.setDescription(description);
        dto.setSeverity(request.getSeverity());

        FaultLog raisedFault = faultLogService.createFault(dto, loggedInEmpId, engineerName);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("engineerRaised", true);
        response.put("message", "Fault raised successfully and sent to analysis queue.");
        response.put("faultId", raisedFault.getFaultId());
        response.put("machineId", raisedFault.getMachineId());
        response.put("machineName", machine.getMachineName());
        response.put("priorityLevel", raisedFault.getPriorityLevel());
        response.put("priorityScore", raisedFault.getPriorityScore());
        return response;
    }

    @Transactional
    public Map<String, Object> requestAdditionalSupport(
            Long scheduleId,
            Long loggedInEmpId,
            SupportRequestDTO request) {

        MaintenanceSchedule schedule = validateStartedEngineerSchedule(scheduleId, loggedInEmpId);
        Machine machine = machineRepo.findById(schedule.getMachineId())
                .orElseThrow(() -> new RuntimeException("Machine not found for this work order."));
        Employee engineer = empRepo.findById(loggedInEmpId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));

        if (request == null || isBlank(request.getReason())) {
            throw new IllegalArgumentException("Reason is required.");
        }

        int requiredCount = request.getRequiredEngineerCount() == null ? 0 : request.getRequiredEngineerCount();
        if (requiredCount < 1 || requiredCount > 3) {
            throw new IllegalArgumentException("Required engineer count must be between 1 and 3.");
        }

        String urgency = normalizeUrgency(request.getUrgency());
        String structuredDescription = supportDescription(
                schedule,
                request.getReason().trim(),
                requiredCount,
                urgency
        );

        RaiseRequest raiseRequest = new RaiseRequest();
        raiseRequest.setEmpId(loggedInEmpId);
        raiseRequest.setMachineId(schedule.getMachineId());
        raiseRequest.setMaintenanceDate(LocalDate.now());
        raiseRequest.setMaintenanceTime(LocalTime.now().withNano(0));
        raiseRequest.setDescription(structuredDescription);
        RaiseRequest savedRequest = raiseRequestRepo.save(raiseRequest);

        List<Long> alertIds = new ArrayList<>();
        for (int index = 0; index < requiredCount; index++) {
            MachineAlert alert = new MachineAlert();
            alert.setMachineId(schedule.getMachineId());
            alert.setIssueName("Support Escalation");
            alert.setSeverity(severityFromUrgency(urgency));
            alert.setPriority(priorityFromUrgency(urgency));
            alert.setAlertPriority("SUPPORT_ESCALATION");
            alert.setAlertReason(structuredDescription + "\nEngineer: EMP-" + engineer.getEmpId() + " - " + engineer.getName());
            alert.setGeneratedBySystem(false);
            alert.setEmpId(null);
            MachineAlert savedAlert = alertRepo.save(alert);
            alertIds.add(savedAlert.getAlertId());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", requiredCount + " support escalation alert(s) created for admin auto assignment.");
        response.put("requestId", savedRequest.getRequestId());
        response.put("scheduleId", schedule.getScheduleId());
        response.put("machineId", schedule.getMachineId());
        response.put("machineName", machine.getMachineName());
        response.put("createdAlertIds", alertIds);
        return response;
    }

    @Transactional
    public void updateScheduleStatus(Long scheduleId, Long loggedInEmpId, MaintenanceStatus newStatus, String remarks) {

        // 1. Fetch and validate the schedule task
        MaintenanceSchedule schedule = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        if (!schedule.getEmpId().equals(loggedInEmpId)) {
            throw new RuntimeException("UNAUTHORIZED: You can only update your own assigned tasks.");
        }

        // 2. Update and save the operational task status
        schedule.setStatus(newStatus);
        scheduleRepo.save(schedule);

        MaintenanceHistory savedHistory = upsertMaintenanceHistory(
                schedule,
                loggedInEmpId,
                newStatus,
                remarks,
                newStatus == MaintenanceStatus.Completed
        );

        linkPartUsageToHistory(scheduleId, savedHistory.getHistoryId());
    }

    @Transactional
    public Map<String, Object> requestParts(Long scheduleId, Long loggedInEmpId, List<PartRequestDTO> requestedParts) {
        MaintenanceSchedule schedule = validateStartedEngineerSchedule(scheduleId, loggedInEmpId);

        Employee engineer = empRepo.findById(loggedInEmpId)
                .orElseThrow(() -> new RuntimeException("Engineer not found"));

        String machineName = machineRepo.findById(schedule.getMachineId())
                .map(Machine::getMachineName)
                .orElse(schedule.getMachineId());

        List<Map<String, Object>> allocatedParts = new ArrayList<>();
        List<ShortageRecommendation> shortageRecommendations = new ArrayList<>();

        for (PartRequestDTO req : requestedParts) {
            Part part = partRepo.findById(req.getPartId())
                    .orElseThrow(() -> new RuntimeException("Part not found"));

            int requestedQty = req.getQuantity() != null ? req.getQuantity() : 0;
            int currentStock = part.getCurrentStock() != null ? part.getCurrentStock() : 0;

            if (requestedQty > currentStock) {
                shortageRecommendations.add(recordShortageRecommendation(
                        schedule,
                        machineName,
                    part.getPartId(),
                        part.getPartName(),
                        requestedQty,
                        currentStock,
                        engineer
                ));
                continue;
            }

            part.setCurrentStock(currentStock - requestedQty);
            partRepo.save(part);

            PartUsage usage = new PartUsage();
            usage.setScheduleId(scheduleId); 
            usage.setPartId(part.getPartId());
            usage.setEmpId(loggedInEmpId);
            usage.setQtyAssigned(requestedQty);
            
            partUsageRepo.save(usage);

            Map<String, Object> allocatedPart = new HashMap<>();
            allocatedPart.put("partId", part.getPartId());
            allocatedPart.put("partName", part.getPartName());
            allocatedPart.put("quantity", requestedQty);
            allocatedPart.put("remainingStock", part.getCurrentStock());
            allocatedParts.add(allocatedPart);
        }

        String partRemarks = requestedParts.stream()
                .map(req -> {
                    Part part = partRepo.findById(req.getPartId()).orElse(null);
                    String partName = part != null ? part.getPartName() : "Part " + req.getPartId();
                    return "Requested " + partName + " x" + req.getQuantity();
                })
                .collect(Collectors.joining("; "));

        if (partRemarks == null || partRemarks.trim().isEmpty()) {
            partRemarks = "Requested spare parts.";
        }

        MaintenanceHistory savedHistory = upsertMaintenanceHistory(
                schedule,
                loggedInEmpId,
                schedule.getStatus(),
                partRemarks,
                schedule.getStatus() == MaintenanceStatus.Completed
        );

        linkPartUsageToHistory(scheduleId, savedHistory.getHistoryId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("partialSuccess", !shortageRecommendations.isEmpty());
        response.put("allocated", allocatedParts);
        response.put("unavailable", shortageRecommendations);
        response.put("allocatedCount", allocatedParts.size());
        response.put("shortageCount", shortageRecommendations.size());
        response.put("message", buildPartsMessage(allocatedParts, shortageRecommendations));
        return response;
    }

    public List<ShortageRecommendation> getReorderRecommendations() {
        return raiseRequestRepo.findByDescriptionStartingWith(REORDER_RECOMMENDATION_PREFIX).stream()
            .map(this::toShortageRecommendation)
            .filter(recommendation -> recommendation != null)
            .sorted(Comparator.comparing(ShortageRecommendation::getSequence).reversed())
            .collect(Collectors.toList());
    }

    private ShortageRecommendation recordShortageRecommendation(
            MaintenanceSchedule schedule,
            String machineName,
            Long partId,
            String partName,
            int requestedQty,
            int availableQty,
            Employee engineer) {
        String createdAt = LocalDateTime.now().format(REORDER_TIMESTAMP_FORMAT);
        String description = buildReorderDescription(
            schedule,
            partId,
            partName,
            requestedQty,
            availableQty,
            engineer,
            createdAt
        );

        RaiseRequest reorderRequest = new RaiseRequest();
        reorderRequest.setEmpId(engineer.getEmpId());
        reorderRequest.setMachineId(schedule.getMachineId());
        reorderRequest.setMaintenanceDate(LocalDate.now());
        reorderRequest.setMaintenanceTime(LocalTime.now().withNano(0));
        reorderRequest.setDescription(description);

        RaiseRequest savedRequest = raiseRequestRepo.save(reorderRequest);
        long sequence = savedRequest.getRequestId() != null
            ? savedRequest.getRequestId()
            : reorderSequence.incrementAndGet();

        ShortageRecommendation recommendation = new ShortageRecommendation(
            sequence,
            partId,
            schedule.getMachineId(),
            machineName,
            partName,
            requestedQty,
            availableQty,
            engineer.getEmpId(),
            engineer.getName(),
            createdAt,
            "REORDER_RECOMMENDED"
        );

        reorderRecommendations.put(sequence, recommendation);
        return recommendation;
    }

    private String buildPartsMessage(
            List<Map<String, Object>> allocatedParts,
            List<ShortageRecommendation> shortages) {

        if (shortages.isEmpty()) {
            return "Parts allocated and inventory deducted successfully.";
        }

        ShortageRecommendation firstShortage = shortages.get(0);
        String message = firstShortage.getPartName() + " unavailable. Reorder escalation sent to inventory.";

        if (shortages.size() > 1) {
            message += " " + (shortages.size() - 1) + " additional shortage(s) captured.";
        }

        if (!allocatedParts.isEmpty()) {
            message += " " + allocatedParts.size() + " part(s) allocated successfully.";
        }

        return message;
    }

    public int resolveReorderRecommendationsForPart(Long partId, String partName) {
        List<RaiseRequest> reorderRequests = raiseRequestRepo.findByDescriptionStartingWith(REORDER_RECOMMENDATION_PREFIX);
        List<RaiseRequest> toRemove = reorderRequests.stream()
            .filter(request -> matchesRecommendationPart(toShortageRecommendation(request), partId, partName))
            .collect(Collectors.toList());

        toRemove.forEach(raiseRequestRepo::delete);

        List<Long> toRemoveSequences = toRemove.stream()
            .map(RaiseRequest::getRequestId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
        toRemoveSequences.forEach(reorderRecommendations::remove);
        return toRemove.size();
    }

    private boolean matchesRecommendationPart(
            ShortageRecommendation recommendation,
            Long partId,
            String partName) {

        if (recommendation == null) {
            return false;
        }

        if (recommendation.getPartId() != null && partId != null) {
            return recommendation.getPartId().equals(partId);
        }

        if (partName == null || partName.trim().isEmpty()) {
            return false;
        }

        String normalizedPartName = partName.trim().toLowerCase();
        String normalizedRecommended = recommendation.getPartName() == null
                ? ""
                : recommendation.getPartName().trim().toLowerCase();
        return normalizedPartName.equals(normalizedRecommended);
    }

    private MaintenanceSchedule validateStartedEngineerSchedule(Long scheduleId, Long loggedInEmpId) {
        MaintenanceSchedule schedule = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        if (!schedule.getEmpId().equals(loggedInEmpId)) {
            throw new RuntimeException("UNAUTHORIZED: You can only use raise request actions for your own assigned tasks.");
        }

        if (schedule.getStatus() != MaintenanceStatus.In_progress) {
            throw new IllegalArgumentException("Raise request actions are allowed only after Start Work. Current status: " + schedule.getStatus());
        }

        return schedule;
    }

    private List<String> machineFaultTypes(Machine machine) {
        List<String> types = new ArrayList<>();
        if (machine.getFaultType() != null) {
            types.addAll(Arrays.stream(machine.getFaultType().split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .distinct()
                    .collect(Collectors.toList()));
        }
        if (types.stream().noneMatch(type -> type.equalsIgnoreCase("Other"))) {
            types.add("Other");
        }
        return types;
    }

    private String normalizeUrgency(String urgency) {
        if (isBlank(urgency)) {
            throw new IllegalArgumentException("Urgency is required.");
        }
        String normalized = urgency.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "low" -> "Low";
            case "medium" -> "Medium";
            case "high" -> "High";
            case "critical" -> "Critical";
            default -> throw new IllegalArgumentException("Urgency must be Low, Medium, High or Critical.");
        };
    }

    private Severity severityFromUrgency(String urgency) {
        return switch (urgency) {
            case "Critical" -> Severity.Critical;
            case "High" -> Severity.High;
            case "Medium" -> Severity.Medium;
            default -> Severity.Low;
        };
    }

    private Priority priorityFromUrgency(String urgency) {
        return switch (urgency) {
            case "Critical", "High" -> Priority._1;
            case "Medium" -> Priority._2;
            default -> Priority._3;
        };
    }

    private String supportDescription(
            MaintenanceSchedule schedule,
            String reason,
            int requiredCount,
            String urgency) {
        return String.join("\n",
                "Type: SUPPORT_REQUEST",
                "ScheduleId: " + schedule.getScheduleId(),
                "AlertId: " + (schedule.getAlertId() == null ? "N/A" : schedule.getAlertId()),
                "Reason: " + reason,
                "RequiredEngineerCount: " + requiredCount,
                "Urgency: " + urgency
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

        private String buildReorderDescription(
            MaintenanceSchedule schedule,
            Long partId,
            String partName,
            int requestedQty,
            int availableQty,
            Employee engineer,
            String createdAt) {
        int shortageQty = Math.max(0, requestedQty - availableQty);
            String machineId = schedule == null ? "" : schedule.getMachineId();
            String machineName = "";
            if (machineId != null && !machineId.isBlank()) {
                machineName = machineRepo.findById(machineId)
                    .map(Machine::getMachineName)
                    .orElse("");
            }
        return String.join("\n",
                REORDER_RECOMMENDATION_PREFIX,
                "PartId: " + (partId == null ? "" : partId),
                "PartName: " + (partName == null ? "" : partName),
                "RequestedQty: " + requestedQty,
                "AvailableQty: " + availableQty,
                "ShortageQty: " + shortageQty,
                "EngineerId: " + (engineer == null ? "" : engineer.getEmpId()),
                "EngineerName: " + (engineer == null ? "" : engineer.getName()),
                "CreatedAt: " + createdAt,
            "MachineId: " + machineId,
            "MachineName: " + machineName
        );
    }

    private ShortageRecommendation toShortageRecommendation(RaiseRequest request) {
        if (request == null || request.getDescription() == null) {
            return null;
        }

        String description = request.getDescription();
        if (!description.startsWith(REORDER_RECOMMENDATION_PREFIX)) {
            return null;
        }

        Map<String, String> fields = parseKeyValueLines(description);
        Long partId = parseLong(fields.get("PartId"));
        String partName = fields.getOrDefault("PartName", "").trim();
        int requestedQty = parseInt(fields.get("RequestedQty"));
        int availableQty = parseInt(fields.get("AvailableQty"));
        Long engineerId = parseLong(fields.get("EngineerId"));
        String engineerName = fields.getOrDefault("EngineerName", "").trim();
        String createdAt = fields.getOrDefault("CreatedAt", "").trim();
        String machineId = fields.getOrDefault("MachineId", request.getMachineId());
        String machineName = fields.getOrDefault("MachineName", "").trim();

        if (isBlank(partName)) {
            return null;
        }

        long sequence = request.getRequestId() != null ? request.getRequestId() : reorderSequence.incrementAndGet();
        return new ShortageRecommendation(
                sequence,
                partId,
                machineId,
                machineName,
                partName,
                requestedQty,
                availableQty,
                engineerId,
                engineerName,
                createdAt,
                "REORDER_RECOMMENDED"
        );
    }

    private Map<String, String> parseKeyValueLines(String description) {
        Map<String, String> fields = new HashMap<>();
        if (description == null) {
            return fields;
        }
        for (String line : description.split("\n")) {
            int colonIndex = line.indexOf(':');
            if (colonIndex <= 0) {
                continue;
            }
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();
            if (!key.isEmpty()) {
                fields.put(key, value);
            }
        }
        return fields;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static class ShortageRecommendation {
        private final long sequence;
        private final Long partId;
        private final String machineId;
        private final String machineName;
        private final String partName;
        private final int requestedQty;
        private final int availableQty;
        private final Long engineerId;
        private final String engineerName;
        private final String requestTimestamp;
        private final String status;

        public ShortageRecommendation(
                long sequence,
            Long partId,
                String machineId,
                String machineName,
                String partName,
                int requestedQty,
                int availableQty,
                Long engineerId,
                String engineerName,
                String requestTimestamp,
                String status) {
            this.sequence = sequence;
            this.partId = partId;
            this.machineId = machineId;
            this.machineName = machineName;
            this.partName = partName;
            this.requestedQty = requestedQty;
            this.availableQty = availableQty;
            this.engineerId = engineerId;
            this.engineerName = engineerName;
            this.requestTimestamp = requestTimestamp;
            this.status = status;
        }

        public long getSequence() { return sequence; }
        @JsonIgnore
        public Long getPartId() { return partId; }
        public String getMachineId() { return machineId; }
        public String getMachineName() { return machineName; }
        public String getPartName() { return partName; }
        public int getRequestedQty() { return requestedQty; }
        public int getAvailableQty() { return availableQty; }
        public Long getEngineerId() { return engineerId; }
        public String getEngineerName() { return engineerName; }
        public String getRequestTimestamp() { return requestTimestamp; }
        public String getStatus() { return status; }
    }

    private MaintenanceHistory upsertMaintenanceHistory(
            MaintenanceSchedule schedule,
            Long empId,
            MaintenanceStatus status,
            String remarks,
            boolean completed) {

        MaintenanceHistory history = historyRepo.findByScheduleId(schedule.getScheduleId())
                .orElseGet(MaintenanceHistory::new);

        history.setScheduleId(schedule.getScheduleId());
        history.setAlertId(schedule.getAlertId());
        history.setMachineId(schedule.getMachineId());
        history.setEmpId(empId);
        history.setStatus(status);

        if (history.getMaintenanceDate() == null) {
            history.setMaintenanceDate(LocalDate.now());
        }

        if (history.getMaintenanceTime() == null) {
            history.setMaintenanceTime(LocalTime.now().withNano(0));
        }

        if (remarks != null && !remarks.trim().isEmpty()) {
            history.setRemarks(remarks);
        } else if (history.getRemarks() == null || history.getRemarks().trim().isEmpty()) {
            history.setRemarks("No remarks provided by Engineer.");
        }

        if (completed) {
            history.setResolvedDate(LocalDate.now());
            history.setResolvedTime(LocalTime.now());
        } else {
            history.setResolvedDate(null);
            history.setResolvedTime(null);
        }

        Integer actualPartsUsed = partUsageRepo.getTotalPartsUsedForSchedule(schedule.getScheduleId());
        history.setQtyAssigned(actualPartsUsed != null ? actualPartsUsed : 0);

        return historyRepo.save(history);
    }

    private void linkPartUsageToHistory(Long scheduleId, Long historyId) {
        List<PartUsage> usages = partUsageRepo.findByScheduleId(scheduleId);
        for (PartUsage usage : usages) {
            usage.sethistoryId(historyId);
            partUsageRepo.save(usage);
        }
    }
}
