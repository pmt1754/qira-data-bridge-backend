package com.app.dto;

public class UpsertStats {
    private int inserted;
    private int updated;
    private int skipped;
    private int failed;
    
    public UpsertStats() {
        this.inserted = 0;
        this.updated = 0;
        this.skipped = 0;
        this.failed = 0;
    }
    
    public void incrementInserted() {
        this.inserted++;
    }
    
    public void incrementUpdated() {
        this.updated++;
    }
    
    public void incrementSkipped() {
        this.skipped++;
    }
    
    public void incrementFailed() {
        this.failed++;
    }
    
    public void addInserted(int count) {
        this.inserted += count;
    }
    
    public void addUpdated(int count) {
        this.updated += count;
    }
    
    public void merge(UpsertStats other) {
        this.inserted += other.inserted;
        this.updated += other.updated;
        this.skipped += other.skipped;
        this.failed += other.failed;
    }
    
    public int getInserted() {
        return inserted;
    }
    
    public int getUpdated() {
        return updated;
    }
    
    public int getSkipped() {
        return skipped;
    }
    
    public int getFailed() {
        return failed;
    }
    
    public int getTotal() {
        return inserted + updated + skipped + failed;
    }
    
    @Override
    public String toString() {
        return String.format("UpsertStats{inserted=%d, updated=%d, skipped=%d, failed=%d}", 
            inserted, updated, skipped, failed);
    }
}
