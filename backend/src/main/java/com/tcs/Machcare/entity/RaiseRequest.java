package com.tcs.Machcare.entity;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "raise_request", schema = "dev")
public class RaiseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "emp_id")
    private Long empId;

    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "maintenance_date")
    private LocalDate maintenanceDate;

    @Column(name = "maintenance_time")
    private LocalTime maintenanceTime;

    @Column(name = "description")
    private String description;

    public RaiseRequest() {}

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }

    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }
    public void setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public LocalTime getMaintenanceTime() {
        return maintenanceTime;
    }
    public void setMaintenanceTime(LocalTime maintenanceTime) {
        this.maintenanceTime = maintenanceTime;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }
}
