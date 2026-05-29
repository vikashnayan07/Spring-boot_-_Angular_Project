package com.tcs.Machcare.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tcs.Machcare.entity.FaultLog;

public interface FaultLogRepository
        extends JpaRepository<FaultLog, String> {
	
	// Count total faults ever logged
    long count();
    
    // Count faults logged by a specific operator
    long countByReportedBy(Long reportedBy);

    long countByFaultDate(LocalDate faultDate);

    long countByMachineIdAndFaultDateBetween(String machineId, LocalDate startDate, LocalDate endDate);

    List<FaultLog> findByMachineIdOrderByFaultDateAscFaultTimeAsc(String machineId);

    @Query("""
            select f from FaultLog f
            where f.analysisStatus = :status
            order by
                case f.priorityLevel
                    when 'P1' then 1
                    when 'P2' then 2
                    else 3
                end,
                f.priorityScore desc,
                f.faultDate desc,
                f.faultTime desc
            """)
    List<FaultLog> findEngineerQueue(@Param("status") String status);

}
