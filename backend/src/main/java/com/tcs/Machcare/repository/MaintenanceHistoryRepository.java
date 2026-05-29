package com.tcs.Machcare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcs.Machcare.entity.MaintenanceHistory;

public interface MaintenanceHistoryRepository 
        extends JpaRepository<MaintenanceHistory, Long> {

    // ✅ SIMPLE + SAFE
    List<MaintenanceHistory> findByMachineId(String machineId);
	List<MaintenanceHistory> findByEmpId(Long empId);
    Optional<MaintenanceHistory> findByScheduleId(Long scheduleId);
}
