package com.tcs.Machcare.repository;

import com.tcs.Machcare.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByRecipientEmpIdOrRecipientRoleIdOrderByCreatedAtDesc(Long empId, Integer roleId);
    long countByRecipientEmpIdAndReadFalse(Long empId);
    long countByRecipientRoleIdAndReadFalse(Integer roleId);
    Optional<Notification> findByRecipientEmpIdAndReferenceTypeAndReferenceIdAndCategory(
        Long recipientEmpId,
        String referenceType,
        String referenceId,
        String category
    );
    Optional<Notification> findByRecipientRoleIdAndReferenceTypeAndReferenceIdAndCategory(
        Integer recipientRoleId,
        String referenceType,
        String referenceId,
        String category
    );
}
