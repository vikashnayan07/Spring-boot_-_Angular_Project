package com.tcs.Machcare.service;

import com.tcs.Machcare.entity.Machine;
import com.tcs.Machcare.entity.Part;
import com.tcs.Machcare.repository.MachineRepository;
import com.tcs.Machcare.repository.PartRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetLifecycleService {

    private final MachineRepository machineRepository;
    private final PartRepository partRepository;

    public AssetLifecycleService(MachineRepository machineRepository, PartRepository partRepository) {
        this.machineRepository = machineRepository;
        this.partRepository = partRepository;
    }

    @Transactional
    public Machine refreshMachineLifecycle(Machine machine) {
        applyMachineStatuses(machine);
        return machineRepository.save(machine);
    }

    @Transactional
    public Part refreshPartLifecycle(Part part) {
        part.setLifecycleStatus(computePartLifecycleStatus(part));
        part.setConditionStatus(computePartConditionStatus(part));
        return partRepository.save(part);
    }

    @Scheduled(cron = "0 15 1 * * *")
    @Transactional
    public void runDailyLifecycleCheck() {
        for (Machine machine : machineRepository.findAll()) {
            applyMachineStatuses(machine);
            machineRepository.save(machine);
        }

        for (Part part : partRepository.findAll()) {
            part.setLifecycleStatus(computePartLifecycleStatus(part));
            part.setConditionStatus(computePartConditionStatus(part));
            partRepository.save(part);
        }
    }

    public String computeMachineLifecycleStatus(Machine machine) {
        if (machine == null) {
            return "UNKNOWN";
        }

        LocalDate purchaseDate = machine.getPurchaseDate();
        LocalDate warrantyExpiryDate = machine.getWarrantyExpiryDate();
        LocalDate nextServiceDueDate = machine.getNextServiceDueDate();

        if (purchaseDate == null) {
            return "UNREGISTERED_ASSET";
        }
        if (warrantyExpiryDate != null && warrantyExpiryDate.isBefore(LocalDate.now())) {
            return "OUT_OF_WARRANTY";
        }
        if (nextServiceDueDate != null && nextServiceDueDate.isBefore(LocalDate.now())) {
            return "SERVICE_OVERDUE";
        }
        return "ACTIVE";
    }

    public String computeMachineWarrantyStatus(Machine machine) {
        if (machine == null) {
            return "UNKNOWN";
        }

        LocalDate warrantyExpiryDate = machine.getWarrantyExpiryDate();
        if (warrantyExpiryDate == null) {
            return "UNKNOWN";
        }

        LocalDate today = LocalDate.now();
        if (warrantyExpiryDate.isBefore(today)) {
            return "EXPIRED";
        }
        if (!warrantyExpiryDate.isAfter(today.plusDays(30))) {
            return "EXPIRING_SOON";
        }
        return "ACTIVE";
    }

    public String computeMachineHealthStatus(Machine machine) {
        if (machine == null) {
            return "UNKNOWN";
        }

        if (machine.getStatus() != null && "Stopped".equalsIgnoreCase(machine.getStatus().name())) {
            return "OFFLINE";
        }

        if (machine.getNextServiceDueDate() != null && machine.getNextServiceDueDate().isBefore(LocalDate.now())) {
            return "MAINTENANCE_DUE";
        }

        if (machine.getWarrantyExpiryDate() != null && machine.getWarrantyExpiryDate().isBefore(LocalDate.now())) {
            return "AT_RISK";
        }

        int load = machine.getCurrentOperationalLoad() == null ? 0 : machine.getCurrentOperationalLoad();
        if (load >= 85 || Boolean.TRUE.equals(machine.getProductionBottleneck())) {
            return "AT_RISK";
        }

        if (load >= 65) {
            return "MONITOR";
        }

        return "HEALTHY";
    }

    public String computePartLifecycleStatus(Part part) {
        if (part == null) {
            return "UNKNOWN";
        }

        LocalDate expiryDate = part.getExpiryDate();
        LocalDate warrantyExpiryDate = part.getWarrantyExpiryDate();
        Integer shelfLifeDays = part.getShelfLifeDays();

        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            return "EXPIRED";
        }
        if (warrantyExpiryDate != null && warrantyExpiryDate.isBefore(LocalDate.now())) {
            return "OUT_OF_WARRANTY";
        }
        if (shelfLifeDays != null && part.getManufactureDate() != null) {
            long ageDays = ChronoUnit.DAYS.between(part.getManufactureDate(), LocalDate.now());
            if (ageDays >= shelfLifeDays) {
                return "END_OF_LIFE";
            }
            if (ageDays >= Math.max(1, shelfLifeDays - 30)) {
                return "EXPIRING_SOON";
            }
        }
        return "ACTIVE";
    }

    public String computePartConditionStatus(Part part) {
        if (part == null) {
            return "UNKNOWN";
        }

        int currentStock = part.getCurrentStock() == null ? 0 : part.getCurrentStock();
        int minStock = part.getMinStock() == null ? 0 : part.getMinStock();

        if (part.getExpiryDate() != null && part.getExpiryDate().isBefore(LocalDate.now())) {
            return "DISCARDED";
        }
        if (currentStock == 0) {
            return "OUT_OF_STOCK";
        }
        if (currentStock <= minStock) {
            return "REORDER";
        }
        return "AVAILABLE";
    }

    public Machine applyMachineStatuses(Machine machine) {
        if (machine == null) {
            return null;
        }
        machine.setLifecycleStatus(computeMachineLifecycleStatus(machine));
        machine.setHealthStatus(computeMachineHealthStatus(machine));
        machine.setWarrantyStatus(computeMachineWarrantyStatus(machine));
        return machine;
    }
}