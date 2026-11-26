package com.scc.MODEL;

import java.time.LocalDateTime;

public class activityLog {
    private int logId;
    private Integer vehicleId;
    private Integer operatorId;
    private String eventType; // PLATE_RECOGNIZED, FAILED_OCR, GATE_OVERRIDE, ADMIN_LOGIN, CRUD_ACTION
    private String plateNumber;
    private double confidenceScore;
    private String gateId;
    private String details;
    private LocalDateTime entryTimestamp;
    private LocalDateTime exitTimestamp;
    private LocalDateTime createdAt;
    private boolean deleted;

    public activityLog() {}

    public activityLog(int logId, Integer vehicleId, Integer operatorId, String eventType,
                       String plateNumber, double confidenceScore, String gateId, String details,
                       LocalDateTime entryTimestamp, LocalDateTime exitTimestamp,
                       LocalDateTime createdAt, boolean deleted) {
        this.logId = logId;
        this.vehicleId = vehicleId;
        this.operatorId = operatorId;
        this.eventType = eventType;
        this.plateNumber = plateNumber;
        this.confidenceScore = confidenceScore;
        this.gateId = gateId;
        this.details = details;
        this.entryTimestamp = entryTimestamp;
        this.exitTimestamp = exitTimestamp;
        this.createdAt = createdAt;
        this.deleted = deleted;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }

    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getGateId() { return gateId; }
    public void setGateId(String gateId) { this.gateId = gateId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getEntryTimestamp() { return entryTimestamp; }
    public void setEntryTimestamp(LocalDateTime entryTimestamp) { this.entryTimestamp = entryTimestamp; }

    public LocalDateTime getExitTimestamp() { return exitTimestamp; }
    public void setExitTimestamp(LocalDateTime exitTimestamp) { this.exitTimestamp = exitTimestamp; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}