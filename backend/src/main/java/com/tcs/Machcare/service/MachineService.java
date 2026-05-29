package com.tcs.Machcare.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tcs.Machcare.dto.MachineDTO;
import com.tcs.Machcare.entity.Machine;
import com.tcs.Machcare.exception.ResourceNotFoundException;
import com.tcs.Machcare.repository.MachineRepository;

@Service
public class MachineService {

    private final MachineRepository repository;
    private final AssetLifecycleService assetLifecycleService;

    public MachineService(
            MachineRepository repository,
            AssetLifecycleService assetLifecycleService) {

        this.repository = repository;
        this.assetLifecycleService = assetLifecycleService;
    }

    // CREATE MACHINE
    public Machine createMachine(
            MachineDTO dto) {

        validateMachine(dto);

        Machine machine =
                new Machine();

        machine.setMachineId(
                "M" + System.currentTimeMillis()
        );

        applyMachineFields(machine, dto);

        return assetLifecycleService.refreshMachineLifecycle(repository.save(machine));
    }

    public Machine updateMachine(String id, MachineDTO dto) {
        validateMachine(dto);

        Machine machine = getMachineById(id);
        applyMachineFields(machine, dto);

        return assetLifecycleService.refreshMachineLifecycle(repository.save(machine));
    }

    // GET ALL
    public List<Machine> getAllMachines() {
        return repository.findAll()
                .stream()
                .map(assetLifecycleService::applyMachineStatuses)
                .toList();
    }

    // GET BY ID
    public Machine getMachineById(
            String id) {
        Machine machine = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Machine not found with ID: " + id
                        ));
        return assetLifecycleService.applyMachineStatuses(machine);
    }

    private void validateMachine(MachineDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Machine details are required.");
        }
        if (isBlank(dto.getMachineName())) {
            throw new IllegalArgumentException("Machine name is required.");
        }
        if (isBlank(dto.getMachineType())) {
            throw new IllegalArgumentException("Machine type is required.");
        }
        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("Machine status is required.");
        }
        if (isBlank(dto.getProductionCriticality())) {
            throw new IllegalArgumentException("productionCriticality is required.");
        }
        if (dto.getCurrentOperationalLoad() == null) {
            throw new IllegalArgumentException("currentOperationalLoad is required.");
        }
        if (dto.getProductionBottleneck() == null) {
            throw new IllegalArgumentException("productionBottleneck is required.");
        }
        if (dto.getExpectedMtbf() == null || dto.getExpectedMtbf() <= 0) {
            throw new IllegalArgumentException("expectedMtbf must be greater than 0 hours.");
        }
        if (dto.getExpectedMttr() == null || dto.getExpectedMttr() <= 0) {
            throw new IllegalArgumentException("expectedMttr must be greater than 0 minutes.");
        }
        if (dto.getPurchaseDate() != null && dto.getWarrantyExpiryDate() != null
                && dto.getWarrantyExpiryDate().isBefore(dto.getPurchaseDate())) {
            throw new IllegalArgumentException("warrantyExpiryDate must be on or after purchaseDate.");
        }
        if (dto.getLastServiceDate() != null && dto.getNextServiceDueDate() != null
                && dto.getNextServiceDueDate().isBefore(dto.getLastServiceDate())) {
            throw new IllegalArgumentException("nextServiceDueDate must be on or after lastServiceDate.");
        }
    }

    private String normalizeCriticality(String value) {
        if (isBlank(value)) {
            return "Medium";
        }

        for (String allowed : List.of("Low", "Medium", "High", "Critical")) {
            if (allowed.equalsIgnoreCase(value.trim())) {
                return allowed;
            }
        }

        throw new IllegalArgumentException(
                "productionCriticality must be one of Low, Medium, High, Critical."
        );
    }

    private Integer normalizeOperationalLoad(Integer value) {
        if (value == null) {
            return 50;
        }
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(
                    "currentOperationalLoad must be between 0 and 100."
            );
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void applyMachineFields(Machine machine, MachineDTO dto) {
        machine.setMachineName(dto.getMachineName());
        machine.setMachineType(dto.getMachineType());
        machine.setStatus(dto.getStatus());
        machine.setFaultType(dto.getFaultType());
        machine.setDescription(dto.getDescription());
        machine.setProductionCriticality(normalizeCriticality(dto.getProductionCriticality()));
        machine.setCurrentOperationalLoad(normalizeOperationalLoad(dto.getCurrentOperationalLoad()));
        machine.setProductionBottleneck(Boolean.TRUE.equals(dto.getProductionBottleneck()));
        machine.setExpectedMtbf(dto.getExpectedMtbf());
        machine.setExpectedMttr(dto.getExpectedMttr());
        machine.setPurchaseDate(dto.getPurchaseDate());
        machine.setWarrantyExpiryDate(dto.getWarrantyExpiryDate());
        machine.setLastServiceDate(dto.getLastServiceDate());
        machine.setNextServiceDueDate(dto.getNextServiceDueDate());
        machine.setLifecycleStatus(dto.getLifecycleStatus());
        machine.setHealthStatus(dto.getHealthStatus());
    }
}
