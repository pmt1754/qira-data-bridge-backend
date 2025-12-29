package com.app.client;

import com.app.dto.QiraPageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QiraClientTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    private QiraClient qiraClient;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        qiraClient = new QiraClient(restTemplate, objectMapper);
    }
    
    @Test
    void testFetchTicketsSuccessfully() {
        String mockResponse = """
            {
                "items": [
                    {"Qira id": "QIRA-1", "Summary": "Test 1"},
                    {"Qira id": "QIRA-2", "Summary": "Test 2"}
                ],
                "total": 10,
                "page": 0,
                "pageSize": 2,
                "hasNext": true
            }
            """;
        
        when(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        QiraPageResponse response = qiraClient.fetchTickets(0, 2);
        
        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        assertEquals(10, response.getTotal());
        assertTrue(response.hasMorePages());
    }
    
    @Test
    void testFetchTicketsLastPage() {
        String mockResponse = """
            {
                "items": [
                    {"Qira id": "QIRA-9", "Summary": "Test 9"}
                ],
                "total": 10,
                "page": 9,
                "pageSize": 2,
                "hasNext": false
            }
            """;
        
        when(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        QiraPageResponse response = qiraClient.fetchTickets(9, 2);
        
        assertNotNull(response);
        assertFalse(response.hasMorePages());
    }
}
