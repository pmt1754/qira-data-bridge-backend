package com.app.service;

import com.app.dto.UpsertStats;
import com.app.model.IssueRecord;
import com.app.repository.IssueRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {
    
    @Mock
    private IssueRecordRepository repository;
    
    @InjectMocks
    private IssueService issueService;
    
    @BeforeEach
    void setUp() {
    }
    
    @Test
    void testUpsertNewRecord() {
        IssueRecord record = new IssueRecord();
        record.setQiraId("QIRA-1");
        record.setSummary("Test");
        
        when(repository.findByQiraId("QIRA-1")).thenReturn(Optional.empty());
        when(repository.save(any(IssueRecord.class))).thenReturn(record);
        
        UpsertStats stats = issueService.upsert(record);
        
        assertEquals(1, stats.getInserted());
        assertEquals(0, stats.getUpdated());
        verify(repository).save(record);
    }
    
    @Test
    void testUpsertExistingRecord() {
        IssueRecord existing = new IssueRecord();
        existing.setId(100L);
        existing.setQiraId("QIRA-1");
        existing.setSummary("Old summary");
        
        IssueRecord newData = new IssueRecord();
        newData.setQiraId("QIRA-1");
        newData.setSummary("Updated summary");
        
        when(repository.findByQiraId("QIRA-1")).thenReturn(Optional.of(existing));
        when(repository.save(any(IssueRecord.class))).thenReturn(existing);
        
        UpsertStats stats = issueService.upsert(newData);
        
        assertEquals(0, stats.getInserted());
        assertEquals(1, stats.getUpdated());
        verify(repository).save(any(IssueRecord.class));
    }
    
    @Test
    void testBulkUpsert() {
        List<IssueRecord> records = new ArrayList<>();
        
        IssueRecord record1 = new IssueRecord();
        record1.setQiraId("QIRA-1");
        records.add(record1);
        
        IssueRecord record2 = new IssueRecord();
        record2.setQiraId("QIRA-2");
        records.add(record2);
        
        when(repository.findExistingQiraIds(anySet())).thenReturn(Set.of("QIRA-1"));
        when(repository.findByQiraId("QIRA-1")).thenReturn(Optional.of(record1));
        when(repository.saveAll(anyList())).thenReturn(List.of(record2));
        when(repository.save(any(IssueRecord.class))).thenReturn(record1);
        
        UpsertStats stats = issueService.upsertBulk(records);
        
        assertTrue(stats.getInserted() > 0 || stats.getUpdated() > 0);
        assertEquals(2, stats.getTotal() - stats.getSkipped() - stats.getFailed());
    }
    
    @Test
    void testUpsertNullRecord() {
        UpsertStats stats = issueService.upsert(null);
        
        assertEquals(1, stats.getSkipped());
        assertEquals(0, stats.getInserted());
        verify(repository, never()).save(any());
    }
}
