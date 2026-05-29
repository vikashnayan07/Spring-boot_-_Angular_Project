package com.tcs.Machcare.dto;

import com.tcs.Machcare.entity.MachineStatus;
import java.time.LocalDate;

public class MachineDTO {

    private String machineName;
    private String machineType;
    private MachineStatus status;
    private String faultType;
    private String description;
    private String productionCriticality;
    private Integer currentOperationalLoad;
    private Boolean productionBottleneck;
    private Integer expectedMtbf;
    private Integer expectedMttr;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiryDate;
    private LocalDate lastServiceDate;
    private LocalDate nextServiceDueDate;
    private String lifecycleStatus;
    private String healthStatus;

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public void setStatus(MachineStatus status) {
        this.status = status;
    }

    public String getFaultType() {
        return faultType;
    }

    public void setFaultType(String faultType) {
        this.faultType = faultType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProductionCriticality() {
        return productionCriticality;
    }

    public void setProductionCriticality(String productionCriticality) {
        this.productionCriticality = productionCriticality;
    }

    public Integer getCurrentOperationalLoad() {
        return currentOperationalLoad;
    }

    public void setCurrentOperationalLoad(Integer currentOperationalLoad) {
        this.currentOperationalLoad = currentOperationalLoad;
    }

    public Boolean getProductionBottleneck() {
        return productionBottleneck;
    }

    public void setProductionBottleneck(Boolean productionBottleneck) {
        this.productionBottleneck = productionBottleneck;
    }

    public Integer getExpectedMtbf() {
        return expectedMtbf;
    }

    public void setExpectedMtbf(Integer expectedMtbf) {
        this.expectedMtbf = expectedMtbf;
    }

    public Integer getExpectedMttr() {
        return expectedMttr;
    }

    public void setExpectedMttr(Integer expectedMttr) {
        this.expectedMttr = expectedMttr;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getWarrantyExpiryDate() {
        return warrantyExpiryDate;
    }

    public void setWarrantyExpiryDate(LocalDate warrantyExpiryDate) {
        this.warrantyExpiryDate = warrantyExpiryDate;
    }

    public LocalDate getLastServiceDate() {
        return lastServiceDate;
    }

    public void setLastServiceDate(LocalDate lastServiceDate) {
        this.lastServiceDate = lastServiceDate;
    }

    public LocalDate getNextServiceDueDate() {
        return nextServiceDueDate;
    }

    public void setNextServiceDueDate(LocalDate nextServiceDueDate) {
        this.nextServiceDueDate = nextServiceDueDate;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(String lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }
}
