package com.app.client;

import com.app.dto.QiraPageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class QiraClient {
    
    private static final Logger logger = LoggerFactory.getLogger(QiraClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${qira.base-url}")
    private String baseUrl;
    
    @Value("${qira.api-key:}")
    private String apiKey;
    
    @Value("${qira.username:}")
    private String username;
    
    @Value("${qira.password:}")
    private String password;
    
    @Value("${qira.jql:project=QIRA}")
    private String jqlQuery;
    
    @Value("${ingestion.page-size:100}")
    private int defaultPageSize;
    
    public QiraClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Fetch a single page of tickets from JIRA/QIRA API using JQL search
     * JIRA REST API v2 uses startAt instead of page number
     * @param startAt Starting index (0-based)
     * @param maxResults Number of results per page
     * @return Page response with tickets
     */
    @Retryable(
        retryFor = {HttpClientErrorException.TooManyRequests.class, Exception.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 30000)
    )
    public QiraPageResponse fetchTickets(int startAt, int maxResults) {
        logger.info("Fetching tickets: startAt={}, maxResults={}", startAt, maxResults);
        
        // Build JIRA search URL with JQL query
        // Example: https://jira.springernature.com/rest/api/2/search?jql=project=QIRA&startAt=0&maxResults=100
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/rest/api/2/search")
            .queryParam("jql", jqlQuery)
            .queryParam("startAt", startAt)
            .queryParam("maxResults", maxResults)
            .queryParam("fields", "*all") // Include all fields including custom fields
            .build()
            .toUriString();
        
        logger.debug("Request URL: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        
        // Support both API key and basic auth
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + apiKey);
        } else if (username != null && !username.isEmpty()) {
            String auth = username + ":" + password;
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
        }
        
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            // Add delay for rate limiting
            Thread.sleep(200); // 200ms between requests
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                logger.warn("Rate limit hit, will retry with backoff");
                throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limited");
            }
            
            // Parse JIRA response
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            QiraPageResponse pageResponse = new QiraPageResponse();
            
            // JIRA API v2 response structure:
            // {
            //   "issues": [...],
            //   "startAt": 0,
            //   "maxResults": 100,
            //   "total": 500
            // }
            
            // Parse issues array
            JsonNode itemsArray = rootNode.get("issues");
            if (itemsArray != null && itemsArray.isArray()) {
                List<JsonNode> items = new ArrayList<>();
                itemsArray.forEach(items::add);
                pageResponse.setItems(items.stream()
                    .map(node -> {
                        com.app.dto.QiraTicket ticket = new com.app.dto.QiraTicket();
                        ticket.setRawData(node);
                        return ticket;
                    })
                    .toList());
                logger.info("Parsed {} tickets from response", items.size());
            } else {
                logger.warn("No 'issues' array found in response");
            }
            
            // Parse pagination metadata from JIRA response
            if (rootNode.has("total")) {
                pageResponse.setTotal(rootNode.get("total").asInt());
            }
            if (rootNode.has("startAt")) {
                int currentStartAt = rootNode.get("startAt").asInt();
                pageResponse.setPage(currentStartAt / maxResults); // Convert startAt to page number
            }
            if (rootNode.has("maxResults")) {
                pageResponse.setPageSize(rootNode.get("maxResults").asInt());
            }
            
            // Calculate if there are more results
            int total = pageResponse.getTotal() != null ? pageResponse.getTotal() : 0;
            int currentStartAt = rootNode.has("startAt") ? rootNode.get("startAt").asInt() : startAt;
            int returnedResults = pageResponse.getItems() != null ? pageResponse.getItems().size() : 0;
            boolean hasNext = (currentStartAt + returnedResults) < total;
            pageResponse.setHasNext(hasNext);
            
            logger.info("Fetched page: startAt={}, maxResults={}, total={}, hasNext={}", 
                currentStartAt, maxResults, total, hasNext);
            
            return pageResponse;
            
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw e; // Will be retried
        } catch (HttpClientErrorException e) {
            logger.error("HTTP client error fetching tickets: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch tickets: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rate limit sleep interrupted", e);
        } catch (Exception e) {
            logger.error("Error fetching tickets from QIRA", e);
            throw new RuntimeException("Failed to fetch tickets", e);
        }
    }
    
    /**
     * Fetch all tickets with automatic pagination using JIRA's startAt/maxResults
     * @return List of all ticket JSON nodes
     */
    public List<JsonNode> fetchAllTickets() {
        logger.info("Starting to fetch all tickets using JQL: {}", jqlQuery);
        List<JsonNode> allTickets = new ArrayList<>();
        
        int startAt = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            try {
                QiraPageResponse response = fetchTickets(startAt, defaultPageSize);
                
                if (response.getItems() != null && !response.getItems().isEmpty()) {
                    response.getItems().forEach(ticket -> 
                        allTickets.add(ticket.getRawData())
                    );
                    logger.info("Accumulated {} total tickets so far", allTickets.size());
                } else {
                    logger.info("No items at startAt {}, stopping", startAt);
                    break;
                }
                
                hasMore = response.hasMorePages();
                startAt += defaultPageSize; // Move to next page using startAt
                
            } catch (Exception e) {
                logger.error("Error fetching at startAt {}, stopping pagination", startAt, e);
                throw new RuntimeException("Pagination failed at startAt " + startAt, e);
            }
        }
        
        logger.info("Completed fetching all tickets: {} total", allTickets.size());
        return allTickets;
    }
    
    /**
     * Fetch limited number of tickets for testing
     * @param maxRecords Maximum number of records to fetch
     * @return List of ticket JSON nodes (up to maxRecords)
     */
    public List<JsonNode> fetchLimitedTickets(int maxRecords) {
        logger.info("Starting to fetch limited tickets using JQL: {} (max: {})", jqlQuery, maxRecords);
        List<JsonNode> allTickets = new ArrayList<>();
        
        int startAt = 0;
        int remaining = maxRecords;
        
        while (remaining > 0) {
            try {
                int fetchSize = Math.min(remaining, defaultPageSize);
                QiraPageResponse response = fetchTickets(startAt, fetchSize);
                
                if (response.getItems() != null && !response.getItems().isEmpty()) {
                    response.getItems().forEach(ticket -> 
                        allTickets.add(ticket.getRawData())
                    );
                    logger.info("Accumulated {} total tickets so far (target: {})", allTickets.size(), maxRecords);
                    
                    remaining -= response.getItems().size();
                    
                    // Stop if we got fewer items than requested (no more data)
                    if (response.getItems().size() < fetchSize || !response.hasMorePages()) {
                        logger.info("Reached end of available data");
                        break;
                    }
                } else {
                    logger.info("No items at startAt {}, stopping", startAt);
                    break;
                }
                
                startAt += fetchSize;
                
            } catch (Exception e) {
                logger.error("Error fetching at startAt {}, stopping pagination", startAt, e);
                throw new RuntimeException("Pagination failed at startAt " + startAt, e);
            }
        }
        
        logger.info("Completed fetching limited tickets: {} total (requested: {})", allTickets.size(), maxRecords);
        return allTickets;
    }
}
