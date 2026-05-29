package com.tcs.Machcare.repository;

import com.tcs.Machcare.entity.MachineAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface MachineAlertRepository extends JpaRepository<MachineAlert, Long> {
	
	// Count total alerts
    long count();
    
    // Count alerts that haven't been assigned to an engineer yet
    long countByEmpIdIsNull();
    List<MachineAlert> findByEmpIdIsNull();

    boolean existsByLinkedAnalysisId(Long linkedAnalysisId);

    Optional<MachineAlert> findByLinkedAnalysisId(Long linkedAnalysisId);
}
