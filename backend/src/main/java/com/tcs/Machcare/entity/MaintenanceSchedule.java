package com.tcs.Machcare.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_schedule", schema = "dev")
public class MaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "machine_id")
    private String machineId; 

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "emp_id")
    private Long empId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MaintenanceStatus status = MaintenanceStatus.Pending;

    @Transient
    private String machineHealthStatus;

    @Transient
    private String machineLifecycleStatus;

    @Transient
    private String machineWarrantyStatus;

    public MaintenanceSchedule() {}

    // Getters and Setters
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }
    public LocalDate getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }
    public MaintenanceStatus getStatus() { return status; }
    public void setStatus(MaintenanceStatus status) { this.status = status; }

    public String getMachineHealthStatus() { return machineHealthStatus; }
    public void setMachineHealthStatus(String machineHealthStatus) { this.machineHealthStatus = machineHealthStatus; }

    public String getMachineLifecycleStatus() { return machineLifecycleStatus; }
    public void setMachineLifecycleStatus(String machineLifecycleStatus) { this.machineLifecycleStatus = machineLifecycleStatus; }

    public String getMachineWarrantyStatus() { return machineWarrantyStatus; }
    public void setMachineWarrantyStatus(String machineWarrantyStatus) { this.machineWarrantyStatus = machineWarrantyStatus; }
}