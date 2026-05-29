package com.tcs.Machcare.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tcs.Machcare.entity.FaultAnalysis;
import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.entity.MachineAlert;
import com.tcs.Machcare.exception.ConflictException;
import com.tcs.Machcare.repository.FaultAnalysisRepository;
import com.tcs.Machcare.repository.FaultLogRepository;

@Service
public class FaultAnalysisService {

    private final FaultAnalysisRepository analysisRepo;
    private final FaultLogRepository faultRepo;
    private final MachineMetricsService metricsService;
    private final RiskBasedAlertService alertService;

    public FaultAnalysisService(
            FaultAnalysisRepository analysisRepo,
            FaultLogRepository faultRepo,
            MachineMetricsService metricsService,
            RiskBasedAlertService alertService) {
        this.analysisRepo = analysisRepo;
        this.faultRepo = faultRepo;
        this.metricsService = metricsService;
        this.alertService = alertService;
    }

    public FaultAnalysis generateAnalysis(String faultId) {
        FaultLog fault = faultRepo.findById(faultId)
                .orElseThrow(() -> new IllegalArgumentException("Fault not found with ID: " + faultId));

        validateAnalysisTransition(fault);

        fault.setAnalysisStatus(PriorityQueueService.STATUS_ANALYZING);
        faultRepo.save(fault);

        FaultAnalysis analysis = new FaultAnalysis();
        analysis.setAnalysisId(System.currentTimeMillis());
        analysis.setFaultId(faultId);

        int totalFailures = (int) metricsService.faultsInLastDays(fault.getMachineId(), 30);
        double severityScore = metricsService.severityScore(fault.getSeverity());
        double frequencyScore = metricsService.frequencyScore(fault.getMachineId());
        double mtbfScore = metricsService.mtbfScore(fault.getMachineId());
        double mttrScore = metricsService.mttrScore(fault.getMachineId());
        double productionImpactScore = metricsService.productionImpactScore(fault.getMachineId());
        double mtbfHours = metricsService.mtbfHours(fault.getMachineId());
        double mttrMinutes = metricsService.mttrMinutes(fault.getMachineId());
        if (mttrMinutes < 0) {
            mttrMinutes = metricsService.expectedMttrMinutes(fault.getMachineId());
        }

        double healthScore = Math.max(
                0,
                100 - (
                        (severityScore * 0.30)
                                + (frequencyScore * 0.25)
                                + (mtbfScore * 0.25)
                                + (mttrScore * 0.20)
                )
        );

        analysis.setTotalFailures(totalFailures);
        analysis.setMtbfHours(metricsService.score(mtbfHours));
        analysis.setFailureFrequency(metricsService.score(totalFailures / 30.0));
        analysis.setMttr(metricsService.score(mttrMinutes < 0 ? 0 : mttrMinutes));
        analysis.setHealthScore(metricsService.score(healthScore));
        analysis.setHealthStatus(metricsService.healthStatus(healthScore));
        analysis.setFailureTrend(metricsService.failureTrend(fault.getMachineId()));
        analysis.setProductionImpactScore(metricsService.score(productionImpactScore));
        analysis.setRiskScore(analysis.getHealthStatus());
        analysis.setPriority(toLegacyPriority(fault.getPriorityLevel()));
        analysis.setDescription("Rule-based analysis completed");

        FaultAnalysis savedAnalysis = analysisRepo.save(analysis);
        MachineAlert generatedAlert = alertService.evaluateAndGenerate(savedAnalysis, fault);

        fault.setAnalysisStatus(generatedAlert == null
                ? PriorityQueueService.STATUS_ANALYZED
                : PriorityQueueService.STATUS_ALERT_GENERATED);
        faultRepo.save(fault);

        return savedAnalysis;
    }

    public List<FaultAnalysis> getAllAnalyses() {
        return analysisRepo.findAll();
    }

    public FaultAnalysis getAnalysisById(Long id) {
        return analysisRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found with ID: " + id));
    }

    private String toLegacyPriority(String priorityLevel) {
        if ("P1".equalsIgnoreCase(priorityLevel)) {
            return "1";
        }
        if ("P2".equalsIgnoreCase(priorityLevel)) {
            return "2";
        }
        return "3";
    }

    private void validateAnalysisTransition(FaultLog fault) {
        String status = fault.getAnalysisStatus();
        if (status == null || status.trim().isEmpty() || PriorityQueueService.STATUS_PENDING.equalsIgnoreCase(status)) {
            return;
        }
        if (PriorityQueueService.STATUS_ANALYZING.equalsIgnoreCase(status)) {
            throw new ConflictException("Fault " + fault.getFaultId() + " is already being analyzed.");
        }
        if (PriorityQueueService.STATUS_ANALYZED.equalsIgnoreCase(status)) {
            throw new ConflictException("Fault " + fault.getFaultId() + " has already been analyzed.");
        }
        if (PriorityQueueService.STATUS_ALERT_GENERATED.equalsIgnoreCase(status)) {
            throw new ConflictException("Fault " + fault.getFaultId() + " already generated an alert.");
        }
        throw new ConflictException("Fault " + fault.getFaultId() + " cannot be analyzed from status " + status + ".");
    }
}
