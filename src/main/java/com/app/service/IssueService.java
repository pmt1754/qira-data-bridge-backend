package com.app.service;

import com.app.dto.UpsertStats;
import com.app.model.IssueRecord;
import com.app.repository.IssueRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IssueService {
    
    private static final Logger logger = LoggerFactory.getLogger(IssueService.class);
    private static final int BATCH_SIZE = 100;
    
    private final IssueRecordRepository repository;
    
    public IssueService(IssueRecordRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Upsert a single issue record
     * @param record The issue record to upsert
     * @return Stats for this operation
     */
    @Transactional
    public UpsertStats upsert(IssueRecord record) {
        UpsertStats stats = new UpsertStats();
        
        if (record == null || record.getQiraId() == null) {
            stats.incrementSkipped();
            return stats;
        }
        
        try {
            Optional<IssueRecord> existing = repository.findByQiraId(record.getQiraId());
            
            if (existing.isPresent()) {
                // Update existing record
                IssueRecord existingRecord = existing.get();
                updateRecord(existingRecord, record);
                repository.save(existingRecord);
                stats.incrementUpdated();
                logger.debug("Updated record for qiraId: {}", record.getQiraId());
            } else {
                // Insert new record
                repository.save(record);
                stats.incrementInserted();
                logger.debug("Inserted new record for qiraId: {}", record.getQiraId());
            }
        } catch (Exception e) {
            logger.error("Failed to upsert record for qiraId: {}", record.getQiraId(), e);
            stats.incrementFailed();
        }
        
        return stats;
    }
    
    /**
     * Efficiently bulk upsert issue records
     * Finds existing records first, then batches insert/update operations
     * @param records List of records to upsert
     * @return Aggregate stats
     */
    @Transactional
    public UpsertStats upsertBulk(List<IssueRecord> records) {
        logger.info("Starting bulk upsert for {} records", records.size());
        UpsertStats stats = new UpsertStats();
        
        if (records == null || records.isEmpty()) {
            return stats;
        }
        
        // Filter out null or invalid records
        List<IssueRecord> validRecords = records.stream()
            .filter(r -> r != null && r.getQiraId() != null && !r.getQiraId().isBlank())
            .collect(Collectors.toList());
        
        stats.addInserted(records.size() - validRecords.size()); // Count skipped
        
        if (validRecords.isEmpty()) {
            logger.warn("No valid records to upsert");
            return stats;
        }
        
        // Extract all qiraIds
        Set<String> qiraIds = validRecords.stream()
            .map(IssueRecord::getQiraId)
            .collect(Collectors.toSet());
        
        // Find existing qiraIds in bulk
        Set<String> existingQiraIds = repository.findExistingQiraIds(qiraIds);
        logger.info("Found {} existing records out of {}", existingQiraIds.size(), qiraIds.size());
        
        // Separate into inserts and updates
        List<IssueRecord> toInsert = new ArrayList<>();
        List<IssueRecord> toUpdate = new ArrayList<>();
        Map<String, IssueRecord> newRecordMap = validRecords.stream()
            .collect(Collectors.toMap(IssueRecord::getQiraId, r -> r, (r1, r2) -> r1));
        
        for (IssueRecord record : validRecords) {
            if (existingQiraIds.contains(record.getQiraId())) {
                toUpdate.add(record);
            } else {
                toInsert.add(record);
            }
        }
        
        // Insert new records individually to handle duplicates gracefully
        if (!toInsert.isEmpty()) {
            logger.info("Inserting {} new records individually", toInsert.size());
            for (IssueRecord record : toInsert) {
                try {
                    // Check if it already exists (race condition protection)
                    if (repository.findByQiraId(record.getQiraId()).isPresent()) {
                        logger.debug("Record {} already exists, skipping insert", record.getQiraId());
                        stats.incrementSkipped();
                    } else {
                        repository.save(record);
                        stats.incrementInserted();
                    }
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Duplicate key - another thread inserted it
                    logger.debug("Duplicate key for qiraId: {}, skipping", record.getQiraId());
                    stats.incrementSkipped();
                } catch (Exception ex) {
                    logger.error("Failed to insert qiraId: {}", record.getQiraId(), ex);
                    stats.incrementFailed();
                }
            }
        }
        
        // Batch update existing records
        if (!toUpdate.isEmpty()) {
            logger.info("Updating {} existing records", toUpdate.size());
            for (IssueRecord newData : toUpdate) {
                try {
                    Optional<IssueRecord> existingOpt = repository.findByQiraId(newData.getQiraId());
                    if (existingOpt.isPresent()) {
                        IssueRecord existing = existingOpt.get();
                        updateRecord(existing, newData);
                        repository.save(existing);
                        stats.incrementUpdated();
                    } else {
                        // Race condition - insert instead
                        repository.save(newData);
                        stats.incrementInserted();
                    }
                } catch (Exception e) {
                    logger.error("Failed to update qiraId: {}", newData.getQiraId(), e);
                    stats.incrementFailed();
                }
            }
        }
        
        logger.info("Bulk upsert completed: {}", stats);
        return stats;
    }
    
    /**
     * Update existing record with new data
     */
    private void updateRecord(IssueRecord existing, IssueRecord newData) {
        // Update all fields (preserve ID)
        existing.setProject(newData.getProject());
        existing.setPriority(newData.getPriority());
        existing.setIssueType(newData.getIssueType());
        existing.setSummary(newData.getSummary());
        existing.setDescription(newData.getDescription());
        existing.setReporter(newData.getReporter());
        existing.setAssignedTeam(newData.getAssignedTeam());
        existing.setAssignee(newData.getAssignee());
        existing.setStatus(newData.getStatus());
        existing.setDueDate(newData.getDueDate());
        existing.setCreatedAt(newData.getCreatedAt());
        existing.setResolvedAt(newData.getResolvedAt());
        existing.setFirstResponseAt(newData.getFirstResponseAt());
        existing.setUpdatedAt(newData.getUpdatedAt());
        existing.setRelatedJiraTicket(newData.getRelatedJiraTicket());
        existing.setLinkedIssues(newData.getLinkedIssues());
        existing.setSupportCategory(newData.getSupportCategory());
        existing.setSupportActionDate(newData.getSupportActionDate());
        existing.setSupportActionedBy(newData.getSupportActionedBy());
        existing.setSupportPriority(newData.getSupportPriority());
        existing.setSupportRemark(newData.getSupportRemark());
        existing.setComment(newData.getComment());
        existing.setIsbnOrderNumber(newData.getIsbnOrderNumber());
        existing.setBookId(newData.getBookId());
        existing.setResolution(newData.getResolution());
        existing.setCausedByBooks(newData.getCausedByBooks());
        existing.setDoiMultiLine(newData.getDoiMultiLine());
        existing.setErratumDoi(newData.getErratumDoi());
        existing.setErrorLocationBooks(newData.getErrorLocationBooks());
        existing.setErrorTypeBooks(newData.getErrorTypeBooks());
        existing.setProductionSystemBooks(newData.getProductionSystemBooks());
        existing.setRequestActionBooks(newData.getRequestActionBooks());
        existing.setPublicationStatusBooks(newData.getPublicationStatusBooks());
        existing.setQiraTicketsCategory(newData.getQiraTicketsCategory());
        existing.setRawJson(newData.getRawJson());
        existing.setIngestedAt(OffsetDateTime.now());
    }
    
    /**
     * Query records by date range
     */
    public List<IssueRecord> findByDateRange(OffsetDateTime from, OffsetDateTime to) {
        return repository.findByDateRange(from, to);
    }
    
    /**
     * Query records by date range and optional team filter
     */
    public List<IssueRecord> findByDateRangeAndTeam(OffsetDateTime from, OffsetDateTime to, String team) {
        return repository.findByDateRangeAndTeam(from, to, team);
    }
}
