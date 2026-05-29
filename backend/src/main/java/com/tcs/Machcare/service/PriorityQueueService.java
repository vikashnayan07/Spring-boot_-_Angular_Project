package com.tcs.Machcare.service;

import org.springframework.stereotype.Service;

import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.repository.FaultLogRepository;

@Service
public class PriorityQueueService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ANALYZING = "ANALYZING";
    public static final String STATUS_ANALYZED = "ANALYZED";
    public static final String STATUS_ALERT_GENERATED = "ALERT_GENERATED";

    private final MachineMetricsService metricsService;
    private final FaultLogRepository faultLogRepository;

    public PriorityQueueService(
            MachineMetricsService metricsService,
            FaultLogRepository faultLogRepository) {
        this.metricsService = metricsService;
        this.faultLogRepository = faultLogRepository;
    }

    public FaultLog applyPriority(FaultLog faultLog) {
        double severity = metricsService.severityScore(faultLog.getSeverity());
        double frequency = metricsService.frequencyScore(faultLog.getMachineId());
        double mtbf = metricsService.mtbfScore(faultLog.getMachineId());
        double mttr = metricsService.mttrScore(faultLog.getMachineId());
        double productionImpact = metricsService.productionImpactScore(faultLog.getMachineId());

        double priorityScore =
                (severity * 0.25)
                        + (frequency * 0.20)
                        + (mtbf * 0.15)
                        + (mttr * 0.10)
                        + (productionImpact * 0.30);

        faultLog.setPriorityScore(metricsService.score(priorityScore));
        faultLog.setPriorityLevel(priorityLevel(priorityScore));
        faultLog.setProductionImpactScore(metricsService.score(productionImpact));
        faultLog.setAnalysisStatus(STATUS_PENDING);

        return faultLogRepository.save(faultLog);
    }

    private String priorityLevel(double score) {
        if (score >= 75) {
            return "P1";
        }
        if (score >= 50) {
            return "P2";
        }
        return "P3";
    }
}
