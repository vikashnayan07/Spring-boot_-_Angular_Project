package com.tcs.Machcare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.tcs.Machcare.entity.FaultAnalysis;

import jakarta.transaction.Transactional;

//public interface FaultAnalysisRepository
//extends JpaRepository<FaultAnalysis, Long> {
//
//}
//	Optional<FaultAnalysis> findByFaultId(String faultId);
//
//	List<FaultAnalysis> findByPriority(String priority);
public interface FaultAnalysisRepository
extends JpaRepository<FaultAnalysis, Long> {

@Modifying
@Transactional
@Query(value = """
INSERT INTO dev.fault_analysis
(analysis_id, fault_id, total_failures, mtbf_hours, 
 failure_frequency, health_score, risk_score, priority, mttr, description)
VALUES (:analysisId, :faultId, :totalFailures, :mtbfHours,
        :failureFrequency, :healthScore, :riskScore,
        CAST(:priority AS dev.priority_enum),
        :mttr, :description)
""", nativeQuery = true)
void insertAnalysis(
    Long analysisId,
    String faultId,
    Integer totalFailures,
    java.math.BigDecimal mtbfHours,
    java.math.BigDecimal failureFrequency,
    java.math.BigDecimal healthScore,
    java.math.BigDecimal riskScore,
    String priority,
    java.math.BigDecimal mttr,
    String description
);
Optional<FaultAnalysis> findByFaultId(String faultId);

List<FaultAnalysis> findByPriority(String priority);

}