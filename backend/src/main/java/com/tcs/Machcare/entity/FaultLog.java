
package com.tcs.Machcare.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.persistence.*;

@Entity
@Table(name = "fault_log", schema = "dev")
public class FaultLog {

    @Id
    @Column(name = "fault_id")
    private String faultId;

    @Column(name = "machine_id")
    private String machineId;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "dev.severity_enum")
    private Severity severity;

    @Column(name = "fault_date")
    private LocalDate faultDate;

    @Column(name = "fault_time")
    private LocalTime faultTime;

    // Made nullable as requested
    @Column(name = "reported_by", nullable = true)
    private Long reportedBy;

    // NEW COLUMN
    @Column(name = "reported_by_name")
    private String reportedByName;

    @Column(name = "priority_score")
    private BigDecimal priorityScore;

    @Column(name = "priority_level")
    private String priorityLevel;

    @Column(name = "production_impact_score")
    private BigDecimal productionImpactScore;

    @Column(name = "analysis_status")
    private String analysisStatus;

    public FaultLog() {
    }

    public String getFaultId() { return faultId; }
    public void setFaultId(String faultId) { this.faultId = faultId; }

    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public LocalDate getFaultDate() { return faultDate; }
    public void setFaultDate(LocalDate faultDate) { this.faultDate = faultDate; }

    public LocalTime getFaultTime() { return faultTime; }
    public void setFaultTime(LocalTime faultTime) { this.faultTime = faultTime; }

    public Long getReportedBy() { return reportedBy; }
    public void setReportedBy(Long reportedBy) { this.reportedBy = reportedBy; }

    public String getReportedByName() { return reportedByName; }
    public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }

    public BigDecimal getPriorityScore() { return priorityScore; }
    public void setPriorityScore(BigDecimal priorityScore) { this.priorityScore = priorityScore; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }

    public BigDecimal getProductionImpactScore() { return productionImpactScore; }
    public void setProductionImpactScore(BigDecimal productionImpactScore) { this.productionImpactScore = productionImpactScore; }

    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
}
