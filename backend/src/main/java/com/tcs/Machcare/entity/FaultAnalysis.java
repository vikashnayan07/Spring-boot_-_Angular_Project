package com.tcs.Machcare.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "fault_analysis", schema = "dev")
public class FaultAnalysis {

    @Id
    @Column(name = "analysis_id")
    private Long analysisId;

    @Column(name = "fault_id")
    private String faultId;

    @Column(name = "total_failures")
    private Integer totalFailures;

    @Column(name = "mtbf_hours")
    private BigDecimal mtbfHours;

    @Column(name = "failure_frequency")
    private BigDecimal failureFrequency;

    @Column(name = "health_score")
    private BigDecimal healthScore;

    @Column(name = "health_status")
    private String healthStatus;

    @Column(name = "failure_trend")
    private String failureTrend;

    @Column(name = "production_impact_score")
    private BigDecimal productionImpactScore;

    // ✅ CHANGED → STRING CATEGORY
    @Column(name = "risk_score")
    private String riskScore;

    @Column(name = "priority")
    private String priority;

    // ✅ NOW STORED IN MINUTES
    @Column(name = "mttr")
    private BigDecimal mttr;

    @Column(name = "description")
    private String description;

    public FaultAnalysis() {}

    public Long getAnalysisId() { return analysisId; }
    public void setAnalysisId(Long analysisId) { this.analysisId = analysisId; }

    public String getFaultId() { return faultId; }
    public void setFaultId(String faultId) { this.faultId = faultId; }

    public Integer getTotalFailures() { return totalFailures; }
    public void setTotalFailures(Integer totalFailures) { this.totalFailures = totalFailures; }

    public BigDecimal getMtbfHours() { return mtbfHours; }
    public void setMtbfHours(BigDecimal mtbfHours) { this.mtbfHours = mtbfHours; }

    public BigDecimal getFailureFrequency() { return failureFrequency; }
    public void setFailureFrequency(BigDecimal failureFrequency) { this.failureFrequency = failureFrequency; }

    public BigDecimal getHealthScore() { return healthScore; }
    public void setHealthScore(BigDecimal healthScore) { this.healthScore = healthScore; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public String getFailureTrend() { return failureTrend; }
    public void setFailureTrend(String failureTrend) { this.failureTrend = failureTrend; }

    public BigDecimal getProductionImpactScore() { return productionImpactScore; }
    public void setProductionImpactScore(BigDecimal productionImpactScore) { this.productionImpactScore = productionImpactScore; }

    public String getRiskScore() { return riskScore; }
    public void setRiskScore(String riskScore) { this.riskScore = riskScore; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public BigDecimal getMttr() { return mttr; }
    public void setMttr(BigDecimal mttr) { this.mttr = mttr; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
