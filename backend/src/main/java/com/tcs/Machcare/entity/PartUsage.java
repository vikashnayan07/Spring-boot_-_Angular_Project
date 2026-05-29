package com.tcs.Machcare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "part_usage", schema = "dev")
public class PartUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long usageId;

    @Column(name = "part_id")
    private Long partId;

    
   
    
    @Column(name = "emp_id")
    private Long empId;

    @Column(name = "qty_assigned", nullable = false)
    private Integer qtyAssigned;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    @Column(name="schedule_id")
	private Long scheduleId;

    @Column(name="history_id")
	private Long historyId;

    @PrePersist
    public void prePersist() {
        this.lastUpdated = LocalDateTime.now();
    }

    public PartUsage() {}

    // Getters and Setters
    public Long gethistoryId() { return historyId; }
    public void sethistoryId(Long historyId) { this.historyId = historyId; }
    
    public Long getscheduleId() { return scheduleId; }
    public void setscheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    
    public Long getUsageId() { return usageId; }
    public void setUsageId(Long usageId) { this.usageId = usageId; }

    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }

    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }

    public Integer getQtyAssigned() { return qtyAssigned; }
    public void setQtyAssigned(Integer qtyAssigned) { this.qtyAssigned = qtyAssigned; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
}