//package com.tcs.Machcare.entity;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//
//import jakarta.persistence.*;
//
//@Entity
//@Table(name = "maintenance_history", schema = "dev")
//public class MaintenanceHistory {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "history_id")
//    private Long historyId;
//
//    // ✅ Links to machine
//    @Column(name = "machine_id")
//    private String machineId;
//
//    // ✅ Maintenance start date
//    @Column(name = "maintenance_date")
//    private LocalDate maintenanceDate;
//
//    // ✅ NEW (IMPORTANT for MTTR)
//    @Column(name = "maintenance_time")
//    private LocalTime maintenanceTime;
//
//    // ✅ Maintain status (MANDATORY FIELD)
//    @Enumerated(EnumType.STRING)
//    @Column(name = "status")
//    private MaintenanceStatus status;
//
//     //✅ Optional remarks
//    @Column(name = "remarks")
//    private String remarks;
//
//    // ✅ Resolution date
//    @Column(name = "resolved_date")
//    private LocalDate resolvedDate;
//
//    // ✅ NEW (IMPORTANT for MTTR)
//    @Column(name = "resolved_time")
//    private LocalTime resolvedTime;
//
//    // =========================
//    // ✅ CONSTRUCTORS
//    // =========================
//
//    public MaintenanceHistory() {}
//
//    // =========================
//    // ✅ GETTERS & SETTERS
//    // =========================
//
//    public Long getHistoryId() {
//        return historyId;
//    }
//
//    public String getMachineId() {
//        return machineId;
//    }
//
//    public void setMachineId(String machineId) {
//        this.machineId = machineId;
//    }
//
//    public LocalDate getMaintenanceDate() {
//        return maintenanceDate;
//    }
//
//    public void setMaintenanceDate(LocalDate maintenanceDate) {
//        this.maintenanceDate = maintenanceDate;
//    }
//
//    public LocalTime getMaintenanceTime() {
//        return maintenanceTime;
//    }
//
//    public void setMaintenanceTime(LocalTime maintenanceTime) {
//        this.maintenanceTime = maintenanceTime;
//    }
//
//    public MaintenanceStatus getStatus() {
//        return status;
//    }
//
//    public void setStatus(MaintenanceStatus status) {
//        this.status = status;
//    }
//
//    public String getRemarks() {
//        return remarks;
//    }
//
//    public void setRemarks(String remarks) {
//        this.remarks = remarks;
//    }
//
//    public LocalDate getResolvedDate() {
//        return resolvedDate;
//    }
//
//    public void setResolvedDate(LocalDate resolvedDate) {
//        this.resolvedDate = resolvedDate;
//    }
//
//    public LocalTime getResolvedTime() {
//        return resolvedTime;
//    }
//
//    public void setResolvedTime(LocalTime resolvedTime) {
//        this.resolvedTime = resolvedTime;
//    }
//}
package com.tcs.Machcare.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
@Entity
@Table(name = "maintenance_history", schema = "dev")
public class MaintenanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "emp_id")
    private Long empId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MaintenanceStatus status;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "maintenance_date")
    private LocalDate maintenanceDate;

    @Column(name = "maintenance_time")
    private LocalTime maintenanceTime;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Column(name = "resolved_time")
    private LocalTime resolvedTime;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "part_id")
    private Long partId;
    
    @Column(name = "qty_assigned", nullable = false)
    private Integer qtyAssigned;

    public MaintenanceHistory() {
    }

    @PrePersist
    @PreUpdate
    void touchLastUpdated() {
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalTime getResolvedTime() {
        return resolvedTime;
    }

    public void setResolvedTime(LocalTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

   

    public Long getEmpId() {
        return empId;
    }

    public void setEmpId(Long empId) {
        this.empId = empId;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }

    public Integer getQtyAssigned() { return qtyAssigned; }
    public void setQtyAssigned(Integer qtyAssigned) { this.qtyAssigned = qtyAssigned; }


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

    public LocalDate getResolvedDate() {
        return resolvedDate;
    }

    public void setResolvedDate(LocalDate resolvedDate) {
        this.resolvedDate = resolvedDate;
    }
}