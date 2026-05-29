package com.tcs.Machcare.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcs.Machcare.entity.FaultAnalysis;
import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.entity.MachineAlert;
import com.tcs.Machcare.entity.Priority;
import com.tcs.Machcare.repository.FaultAnalysisRepository;
import com.tcs.Machcare.repository.FaultLogRepository;
import com.tcs.Machcare.repository.MachineAlertRepository;

@Service
@Transactional
public class MachineAlertService {

    private final MachineAlertRepository alertRepo;
    private final FaultAnalysisRepository analysisRepo;
    private final FaultLogRepository faultRepo;

    public MachineAlertService(
            MachineAlertRepository alertRepo,
            FaultAnalysisRepository analysisRepo,
            FaultLogRepository faultRepo) {

        this.alertRepo = alertRepo;
        this.analysisRepo = analysisRepo;
        this.faultRepo = faultRepo;
    }

    public MachineAlert generateAlert(Long analysisId) {

        // ✅ 1. FETCH ANALYSIS
        FaultAnalysis analysis = analysisRepo.findById(analysisId)
                .orElseThrow(() -> new RuntimeException("Analysis not found"));

        // ✅ 2. FETCH FAULT
        FaultLog fault = faultRepo.findById(analysis.getFaultId())
                .orElseThrow(() -> new RuntimeException("Fault not found"));

        MachineAlert alert = new MachineAlert();

        // ✅ SET VALUES
        alert.setAnalysisId(analysisId);
        alert.setMachineId(fault.getMachineId());
        alert.setIssueName(fault.getDescription());

        // ✅ FIXED ENUM (IMPORTANT)
        alert.setSeverity(fault.getSeverity());
        alert.setSeverity(fault.getSeverity());

        switch (analysis.getPriority()) {
            case "1":
                alert.setPriority(Priority._1);
                break;
            case "2":
                alert.setPriority(Priority._2);
                break;
            default:
                alert.setPriority(Priority._3);
        }

        // ✅ STRING priority (safe for '1','2','3')
        // alert.setPriority(analysis.getPriority());

        // ✅ TEMP DEFAULT
        alert.setEmpId(1L);

        MachineAlert saved = alertRepo.save(alert);

        // ✅ DEBUG LOG
        System.out.println("✅ Alert saved successfully: ID = " + saved.getAlertId());

        return saved;
    }
}