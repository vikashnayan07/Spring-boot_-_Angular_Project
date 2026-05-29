package com.tcs.Machcare.repository;

import com.tcs.Machcare.entity.PartUsage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PartUsageRepository extends JpaRepository<PartUsage, Long> {
    
    // Your existing methods (Kept perfectly intact!)
    List<PartUsage> findByScheduleId(Long scheduleId);
    List<PartUsage> findByHistoryId(Long historyId);

    // ✅ THE NEW FIX: Calculates the exact total parts used for the history log
    @Query("SELECT COALESCE(SUM(p.qtyAssigned), 0) FROM PartUsage p WHERE p.scheduleId = :scheduleId")
    Integer getTotalPartsUsedForSchedule(@Param("scheduleId") Long scheduleId);
}