package com.tcs.Machcare.service;

import com.tcs.Machcare.entity.AuditLog;
import com.tcs.Machcare.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(
            Long empId,
            String empName,
            String email,
            Integer roleId,
            String action,
            String status,
            String message) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEmpId(empId);
            auditLog.setEmpName(empName);
            auditLog.setEmail(email);
            auditLog.setRoleId(roleId);
            auditLog.setAction(action);
            auditLog.setStatus(status);
            auditLog.setMessage(message);
            auditLog.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception ignored) {
            // Best-effort observability must never break the business workflow.
        }
    }

    public List<AuditLog> getFilteredLogs(
            String date,
            Integer roleId,
            String status,
            String action) {

        LocalDate filterDate = parseDate(date).orElse(null);
        String normalizedStatus = normalize(status);
        String normalizedAction = normalize(action);

        return auditLogRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")
                                .and(Sort.by(Sort.Direction.DESC, "logId")))
                .stream()
                .filter(log -> filterDate == null || sameDate(log.getCreatedAt(), filterDate))
                .filter(log -> roleId == null || Objects.equals(log.getRoleId(), roleId))
                .filter(log -> normalizedStatus == null || normalize(log.getStatus()).equals(normalizedStatus))
                .filter(log -> normalizedAction == null || normalize(log.getAction()).equals(normalizedAction))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getSummary() {
        List<AuditLog> logs = auditLogRepository.findAll();
        LocalDate today = LocalDate.now();

        long actionsToday = logs.stream().filter(log -> sameDate(log.getCreatedAt(), today)).count();
        long failedActions = logs.stream()
                .filter(log -> sameDate(log.getCreatedAt(), today))
                .filter(log -> "FAILED".equalsIgnoreCase(Optional.ofNullable(log.getStatus()).orElse("")))
                .count();
        long successActions = logs.stream()
                .filter(log -> sameDate(log.getCreatedAt(), today))
                .filter(log -> "SUCCESS".equalsIgnoreCase(Optional.ofNullable(log.getStatus()).orElse("")))
                .count();
        long activeUsers = logs.stream()
                .filter(log -> sameDate(log.getCreatedAt(), today))
                .map(AuditLog::getEmpId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        double successRate = actionsToday == 0 ? 0 : (successActions * 100.0) / actionsToday;

        Map<String, Object> summary = new HashMap<>();
        summary.put("actionsToday", actionsToday);
        summary.put("successRate", Math.round(successRate * 10.0) / 10.0);
        summary.put("failedActions", failedActions);
        summary.put("activeUsers", activeUsers);
        return summary;
    }

    private Optional<LocalDate> parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.trim()));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    private boolean sameDate(LocalDateTime timestamp, LocalDate date) {
        return timestamp != null && timestamp.toLocalDate().equals(date);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}