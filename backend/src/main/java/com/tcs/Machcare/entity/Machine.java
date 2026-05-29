package com.tcs.Machcare.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;

@Entity
@Table(name = "machine", schema = "dev")
public class Machine {

    @Id
    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "machine_name")
    private String machineName;

    @Column(name = "machine_type")
    private String machineType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
        name = "status",
        columnDefinition = "dev.machine_status_enum"
    )
    private MachineStatus status;

    @Column(name = "fault_type")
    private String faultType;

    @Column(name = "description")
    private String description;

    @Column(name = "production_criticality")
    private String productionCriticality = "Medium";

    @Column(name = "current_operational_load")
    private Integer currentOperationalLoad = 50;

    @Column(name = "is_production_bottleneck")
    private Boolean productionBottleneck = false;

    @Column(name = "expected_mtbf")
    private Integer expectedMtbf = 240;

    @Column(name = "expected_mttr")
    private Integer expectedMttr = 120;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "next_service_due_date")
    private LocalDate nextServiceDueDate;

    @Column(name = "lifecycle_status")
    private String lifecycleStatus;

    @Column(name = "health_status")
    private String healthStatus;

    @Column(name = "warranty_status")
    private String warrantyStatus;

    public Machine() {}

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

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

    public String getWarrantyStatus() {
        return warrantyStatus;
    }

    public void setWarrantyStatus(String warrantyStatus) {
        this.warrantyStatus = warrantyStatus;
    }
}
