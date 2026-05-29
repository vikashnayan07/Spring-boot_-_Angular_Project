package com.tcs.Machcare.dto;

public class AlertDTO {
    private String machineId;
    private String issueName;
    private String severity;
    private String priority;
    private Long empId;

    public AlertDTO() {}

    public AlertDTO(String machineId, String issueName, String severity,
                    String priority, Long empId) {
        this.machineId = machineId;
        this.issueName = issueName;
        this.severity = severity;
        this.priority = priority;
        this.empId = empId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getIssueName() {
        return issueName;
    }

    public void setIssueName(String issueName) {
        this.issueName = issueName;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Long getEmpId() {
        return empId;
    }

    public void setEmpId(Long empId) {
        this.empId = empId;
    }
}