package com.app.service;

import com.app.model.IssueRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketMapperTest {
    
    private TicketMapper ticketMapper;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        ticketMapper = new TicketMapper();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testMapCompleteTicket() throws Exception {
        String json = """
            {
                "key": "QIRA-357781",
                "fields": {
                    "project": { "key": "QIRA", "name": "Qira Project" },
                    "issuetype": { "name": "Bug" },
                    "priority": { "name": "High" },
                    "summary": "Example issue summary",
                    "description": null,
                    "reporter": { "displayName": "Alice", "emailAddress": "alice@example.com" },
                    "assignee": { "displayName": "Bob", "emailAddress": "bob@example.com" },
                    "status": { "name": "Open" },
                    "duedate": "2025-12-10T00:00:00.000+0000",
                    "created": "2025-11-01T12:00:00.000+0000",
                    "resolutiondate": null,
                    "updated": "2025-11-02T09:00:00.000+0000",
                    "customfield_22883": { "value": "MybookSubmission" },
                    "customfield_22884": { "value": "Medium" },
                    "customfield_22885": "Support remark text",
                    "customfield_22886": [ { "displayName": "SupportUser1" }, { "displayName": "SupportUser2" } ],
                    "customfield_20797": "Alternate description text"
                }
            }
            """;
        
        JsonNode node = objectMapper.readTree(json);
        IssueRecord record = ticketMapper.map(node);
        
        assertNotNull(record);
        assertEquals("QIRA-357781", record.getQiraId());
        assertEquals("Qira Project", record.getProject());
        assertEquals("High", record.getPriority());
        assertEquals("Bug", record.getIssueType());
        assertEquals("Example issue summary", record.getSummary());
        assertEquals("Alternate description text", record.getDescription());
        assertEquals("Alice", record.getReporter());
        assertEquals("Bob", record.getAssignee());
        assertEquals("Open", record.getStatus());
        assertEquals("MybookSubmission", record.getSupportCategory());
        assertEquals("Medium", record.getSupportPriority());
        assertEquals("Support remark text", record.getSupportRemark());
        assertEquals("SupportUser1, SupportUser2", record.getSupportActionedBy());
        assertNotNull(record.getCreatedAt());
        assertNotNull(record.getUpdatedAt());
        assertNotNull(record.getRawJson());
    }
    
    @Test
    void testMapTicketWithMissingQiraId() throws Exception {
        String json = """
            {
                "Project": "Platform",
                "Summary": "Issue without qiraId"
            }
            """;
        
        JsonNode node = objectMapper.readTree(json);
        IssueRecord record = ticketMapper.map(node);
        
        assertNull(record, "Should return null when qiraId is missing");
    }
    
    @Test
    void testMapTicketWithNullFields() throws Exception {
        String json = """
            {
                "Qira id": "QIRA-99999",
                "Project": null,
                "Priority": null,
                "Summary": "Issue with nulls"
            }
            """;
        
        JsonNode node = objectMapper.readTree(json);
        IssueRecord record = ticketMapper.map(node);
        
        assertNotNull(record);
        assertEquals("QIRA-99999", record.getQiraId());
        assertNull(record.getProject());
        assertNull(record.getPriority());
        assertEquals("Issue with nulls", record.getSummary());
    }
    
    @Test
    void testMapMultiLineDoi() throws Exception {
        String json = """
            {
                "Qira id": "QIRA-11111",
                "DOI (multiple entries possible - text area)": ["10.1000/test1", "10.1000/test2", "10.1000/test3"]
            }
            """;
        
        JsonNode node = objectMapper.readTree(json);
        IssueRecord record = ticketMapper.map(node);
        
        assertNotNull(record);
        assertEquals("QIRA-11111", record.getQiraId());
        String expectedDoi = "10.1000/test1\n10.1000/test2\n10.1000/test3";
        assertEquals(expectedDoi, record.getDoiMultiLine());
    }
}
