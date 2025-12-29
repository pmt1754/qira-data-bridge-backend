package com.app.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobStatus {
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private int totalFetched;
    private int inserted;
    private int updated;
    private int failed;
    private List<String> errors;
    private String status; // "RUNNING", "COMPLETED", "FAILED"
    
    public JobStatus() {
        this.errors = new ArrayList<>();
        this.status = "RUNNING";
    }
    
    public void markCompleted() {
        this.finishedAt = OffsetDateTime.now();
        this.status = errors.isEmpty() || errors.size() < totalFetched / 2 ? "COMPLETED" : "COMPLETED_WITH_ERRORS";
    }
    
    public void markFailed(String error) {
        this.finishedAt = OffsetDateTime.now();
        this.status = "FAILED";
        this.errors.add(error);
    }
    
    public void addError(String error) {
        this.errors.add(error);
    }
    
    public long getDurationSeconds() {
        if (startedAt == null) return 0;
        OffsetDateTime end = finishedAt != null ? finishedAt : OffsetDateTime.now();
        return java.time.Duration.between(startedAt, end).getSeconds();
    }
    
    // Getters and Setters
    public OffsetDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }
    
    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
    
    public int getTotalFetched() {
        return totalFetched;
    }
    
    public void setTotalFetched(int totalFetched) {
        this.totalFetched = totalFetched;
    }
    
    public int getInserted() {
        return inserted;
    }
    
    public void setInserted(int inserted) {
        this.inserted = inserted;
    }
    
    public int getUpdated() {
        return updated;
    }
    
    public void setUpdated(int updated) {
        this.updated = updated;
    }
    
    public int getFailed() {
        return failed;
    }
    
    public void setFailed(int failed) {
        this.failed = failed;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
