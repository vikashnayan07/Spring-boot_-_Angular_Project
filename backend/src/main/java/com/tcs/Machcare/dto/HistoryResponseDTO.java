package com.tcs.Machcare.dto;

import com.tcs.Machcare.entity.MaintenanceHistory;
import com.tcs.Machcare.entity.PartUsage;

import java.util.List;

public class HistoryResponseDTO {

    private MaintenanceHistory history;

    private List<PartUsage> partsUsed;

    public MaintenanceHistory getHistory() {
        return history;
    }

    public void setHistory(MaintenanceHistory history) {
        this.history = history;
    }

    public List<PartUsage> getPartsUsed() {
        return partsUsed;
    }

    public void setPartsUsed(List<PartUsage> partsUsed) {
        this.partsUsed = partsUsed;
    }
}