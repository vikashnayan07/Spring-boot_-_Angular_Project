package com.tcs.Machcare.service;

import com.tcs.Machcare.entity.Notification;
import com.tcs.Machcare.entity.Employee;
import com.tcs.Machcare.repository.EmployeeRepository;
import com.tcs.Machcare.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final RealtimeEventService realtimeEventService;
    private final EmployeeRepository employeeRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            RealtimeEventService realtimeEventService,
            EmployeeRepository employeeRepository) {
        this.notificationRepository = notificationRepository;
        this.realtimeEventService = realtimeEventService;
        this.employeeRepository = employeeRepository;
    }

    public List<Notification> listFor(Long empId, Integer roleId) {
        return notificationRepository.findTop50ByRecipientEmpIdOrRecipientRoleIdOrderByCreatedAtDesc(empId, roleId);
    }

    public long unreadCount(Long empId, Integer roleId) {
        return notificationRepository.countByRecipientEmpIdAndReadFalse(empId)
            + notificationRepository.countByRecipientRoleIdAndReadFalse(roleId);
    }

    @Transactional
    public Notification notifyUser(Long empId, String title, String message, String category, String severity, String referenceType, String referenceId) {
        Notification notification = notificationRepository
            .findByRecipientEmpIdAndReferenceTypeAndReferenceIdAndCategory(empId, referenceType, referenceId, category)
            .orElseGet(Notification::new);
        notification.setRecipientEmpId(empId);
        fill(notification, title, message, category, severity, referenceType, referenceId);
        Notification saved = notificationRepository.save(notification);
        realtimeEventService.emitToUser(empId, "notification", saved);
        realtimeEventService.emitToUser(empId, "notifications_updated", unreadCount(empId, 0));
        return saved;
    }

    @Transactional
    public Notification notifyRole(Integer roleId, String title, String message, String category, String severity, String referenceType, String referenceId) {
        Notification firstSaved = null;
        for (Employee employee : employeeRepository.findByRoleIdAndIsActiveTrue(roleId)) {
            Notification saved = notifyUser(
                    employee.getEmpId(),
                    title,
                    message,
                    category,
                    severity,
                    referenceType,
                    referenceId);
            if (firstSaved == null) {
                firstSaved = saved;
            }
        }
        realtimeEventService.emitToRole(roleId, "notifications_updated", Map.of("roleId", roleId));
        return firstSaved;
    }

    @Transactional
    public void markRead(Long notificationId, Long empId, Integer roleId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found."));
        boolean allowed = empId.equals(notification.getRecipientEmpId()) || roleId.equals(notification.getRecipientRoleId());
        if (!allowed) {
            throw new IllegalArgumentException("You cannot update this notification.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        realtimeEventService.emitToUser(empId, "notifications_updated", unreadCount(empId, roleId));
    }

    @Transactional
    public void markAllRead(Long empId, Integer roleId) {
        listFor(empId, roleId).forEach(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
        realtimeEventService.emitToUser(empId, "notifications_updated", 0);
    }

    private void fill(Notification notification, String title, String message, String category, String severity, String referenceType, String referenceId) {
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCategory(category);
        notification.setSeverity(severity);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
    }
}
