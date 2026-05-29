package com.tcs.Machcare.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.tcs.Machcare.entity.FaultLog;
import com.tcs.Machcare.entity.Machine;
import com.tcs.Machcare.entity.MaintenanceHistory;
import com.tcs.Machcare.entity.Severity;
import com.tcs.Machcare.repository.FaultLogRepository;
import com.tcs.Machcare.repository.MachineRepository;
import com.tcs.Machcare.repository.MaintenanceHistoryRepository;

@Service
public class MachineMetricsService {

    private final FaultLogRepository faultLogRepository;
    private final MachineRepository machineRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;

    public MachineMetricsService(
            FaultLogRepository faultLogRepository,
            MachineRepository machineRepository,
            MaintenanceHistoryRepository maintenanceHistoryRepository) {
        this.faultLogRepository = faultLogRepository;
        this.machineRepository = machineRepository;
        this.maintenanceHistoryRepository = maintenanceHistoryRepository;
    }

    public double severityScore(Severity severity) {
        if (severity == null) {
            return 0;
        }

        return switch (severity) {
            case Low -> 25;
            case Medium -> 50;
            case High -> 75;
            case Critical -> 100;
        };
    }

    public long faultsInLastDays(String machineId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        return faultLogRepository.countByMachineIdAndFaultDateBetween(machineId, start, end);
    }

    public long faultsBetween(String machineId, LocalDate start, LocalDate end) {
        return faultLogRepository.countByMachineIdAndFaultDateBetween(machineId, start, end);
    }

    public double frequencyScore(String machineId) {
        long count = faultsInLastDays(machineId, 30);

        if (count == 0) {
            return 0;
        }
        if (count <= 2) {
            return 25;
        }
        if (count <= 5) {
            return 50;
        }
        if (count <= 9) {
            return 75;
        }
        return 100;
    }

    public double mtbfHours(String machineId) {
        List<FaultLog> faults = faultLogRepository.findByMachineIdOrderByFaultDateAscFaultTimeAsc(machineId)
                .stream()
                .filter(fault -> fault.getFaultDate() != null && fault.getFaultTime() != null)
                .toList();

        if (faults.size() < 2) {
            return expectedMtbfHours(machineId);
        }

        long totalHours = 0;
        int gaps = 0;
        for (int i = 1; i < faults.size(); i++) {
            LocalDateTime previous = LocalDateTime.of(faults.get(i - 1).getFaultDate(), faults.get(i - 1).getFaultTime());
            LocalDateTime current = LocalDateTime.of(faults.get(i).getFaultDate(), faults.get(i).getFaultTime());
            long hours = Math.max(0, Duration.between(previous, current).toHours());
            totalHours += hours;
            gaps++;
        }

        return gaps == 0 ? expectedMtbfHours(machineId) : (double) totalHours / gaps;
    }

    public double mtbfScore(String machineId) {
        double hours = mtbfHours(machineId);

        if (hours > 240) {
            return 0;
        }
        if (hours >= 120) {
            return 25;
        }
        if (hours >= 48) {
            return 50;
        }
        if (hours >= 12) {
            return 75;
        }
        return 100;
    }

    public double mttrMinutes(String machineId) {
        List<MaintenanceHistory> histories = maintenanceHistoryRepository.findByMachineId(machineId)
                .stream()
                .filter(history -> history.getMaintenanceDate() != null && history.getResolvedDate() != null)
            .filter(history -> history.getResolvedTime() != null)
            .sorted(Comparator.comparing(
                MaintenanceHistory::getResolvedDate,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MaintenanceHistory::getResolvedTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed())
                .toList();

        if (histories.isEmpty()) {
            return -1;
        }

        List<Double> repairMinutes = histories.stream()
            .map(this::repairMinutes)
                .filter(minutes -> minutes >= 0)
                .toList();

        if (repairMinutes.isEmpty()) {
            return -1;
        }

        return repairMinutes.stream().mapToDouble(Double::doubleValue).average().orElse(-1);
    }

    public double mttrScore(String machineId) {
        double minutes = mttrMinutes(machineId);
        if (minutes < 0) {
            minutes = expectedMttrMinutes(machineId);
        }
        if (minutes < 60) {
            return 25;
        }
        if (minutes <= 180) {
            return 50;
        }
        if (minutes <= 360) {
            return 75;
        }
        return 100;
    }

    public double productionImpactScore(String machineId) {
        Machine machine = machineRepository.findById(machineId).orElse(null);
        double staticCriticality = criticalityScore(machine == null ? null : machine.getProductionCriticality());
        int load = machine == null || machine.getCurrentOperationalLoad() == null
                ? 50
                : Math.max(0, Math.min(100, machine.getCurrentOperationalLoad()));
        double bottleneckBonus = machine != null && Boolean.TRUE.equals(machine.getProductionBottleneck()) ? 20 : 0;

        return Math.min(100, (staticCriticality * 0.50) + (load * 0.30) + bottleneckBonus);
    }

    public double expectedMtbfHours(String machineId) {
        Machine machine = machineRepository.findById(machineId).orElse(null);
        if (machine == null || machine.getExpectedMtbf() == null || machine.getExpectedMtbf() <= 0) {
            return 240;
        }
        return machine.getExpectedMtbf();
    }

    public double expectedMttrMinutes(String machineId) {
        Machine machine = machineRepository.findById(machineId).orElse(null);
        if (machine == null || machine.getExpectedMttr() == null || machine.getExpectedMttr() <= 0) {
            return 120;
        }
        return machine.getExpectedMttr();
    }

    public String failureTrend(String machineId) {
        LocalDate today = LocalDate.now();
        long last7 = faultsBetween(machineId, today.minusDays(6), today);
        long previous7 = faultsBetween(machineId, today.minusDays(13), today.minusDays(7));

        if (last7 - previous7 > 1) {
            return "Increasing";
        }
        if (previous7 - last7 > 1) {
            return "Declining";
        }
        return "Stable";
    }

    public String healthStatus(double healthScore) {
        if (healthScore >= 80) {
            return "Healthy";
        }
        if (healthScore >= 60) {
            return "Stable";
        }
        if (healthScore >= 40) {
            return "Warning";
        }
        if (healthScore >= 20) {
            return "Critical";
        }
        return "Failure Risk";
    }

    public BigDecimal score(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private double criticalityScore(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 50;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "low" -> 25;
            case "medium" -> 50;
            case "high" -> 75;
            case "critical" -> 100;
            default -> 50;
        };
    }

    private double repairMinutes(MaintenanceHistory history) {
        LocalTime maintenanceTime = history.getMaintenanceTime() == null
                ? LocalTime.MIDNIGHT
                : history.getMaintenanceTime();
        LocalDateTime start = LocalDateTime.of(history.getMaintenanceDate(), maintenanceTime);
        LocalDateTime resolved = LocalDateTime.of(history.getResolvedDate(), history.getResolvedTime());
        long seconds = Duration.between(start, resolved).getSeconds();
        return Math.max(0, seconds / 60.0);
    }
}
