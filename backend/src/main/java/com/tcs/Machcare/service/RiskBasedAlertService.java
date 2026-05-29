package com.tcs.Machcare.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tcs.Machcare.entity.FaultAnalysis;
import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.entity.MachineAlert;
import com.tcs.Machcare.entity.Priority;
import com.tcs.Machcare.entity.Severity;
import com.tcs.Machcare.exception.ConflictException;
import com.tcs.Machcare.repository.MachineAlertRepository;

@Service
public class RiskBasedAlertService {

    private final MachineAlertRepository alertRepository;

    public RiskBasedAlertService(MachineAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public MachineAlert evaluateAndGenerate(FaultAnalysis analysis, FaultLog faultLog) {
        if (analysis == null || faultLog == null || analysis.getAnalysisId() == null) {
            throw new IllegalArgumentException("Fault analysis and fault log are required for alert evaluation.");
        }

        if (alertRepository.existsByLinkedAnalysisId(analysis.getAnalysisId())) {
            throw new ConflictException("Alert already exists for analysis ID: " + analysis.getAnalysisId());
        }

        List<String> reasons = matchedReasons(analysis, faultLog);
        if (reasons.size() < 2) {
            return null;
        }

        MachineAlert alert = new MachineAlert();
        alert.setMachineId(faultLog.getMachineId());
        alert.setAnalysisId(analysis.getAnalysisId());
        alert.setIssueName("System generated risk alert for fault " + faultLog.getFaultId());
        alert.setSeverity(reasons.size() >= 3 ? Severity.Critical : Severity.High);
        alert.setPriority(reasons.size() >= 3 ? Priority._1 : Priority._2);
        alert.setEmpId(null);
        alert.setAlertPriority(reasons.size() >= 3 ? "Critical Alert" : "High Alert");
        alert.setAlertReason(String.join("; ", reasons));
        alert.setGeneratedBySystem(true);
        alert.setLinkedFaultId(faultLog.getFaultId());
        alert.setLinkedAnalysisId(analysis.getAnalysisId());

        return alertRepository.save(alert);
    }

    private List<String> matchedReasons(FaultAnalysis analysis, FaultLog faultLog) {
        List<String> reasons = new ArrayList<>();

        double healthScore = analysis.getHealthScore() == null
                ? 100
                : analysis.getHealthScore().doubleValue();
        double productionImpact = analysis.getProductionImpactScore() == null
                ? 0
                : analysis.getProductionImpactScore().doubleValue();

        if (healthScore < 40) {
            reasons.add("Machine health score is below 40");
        }
        if (faultLog.getSeverity() == Severity.Critical) {
            reasons.add("Fault severity is Critical");
        }
        if ("Increasing".equalsIgnoreCase(analysis.getFailureTrend())) {
            reasons.add("Failure trend is Increasing");
        }
        if (productionImpact > 70) {
            reasons.add("Production impact is above 70");
        }

        return reasons;
    }
}
