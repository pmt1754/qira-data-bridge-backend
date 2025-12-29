package com.app.controller;

import com.app.dto.JobStatus;
import com.app.model.IssueRecord;
import com.app.service.IngestionOrchestrator;
import com.app.service.IssueService;
import com.app.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Administrative endpoints for manual ingestion triggers and job monitoring")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final IngestionOrchestrator ingestionOrchestrator;
    private final IssueService issueService;
    private final ReportService reportService;
    
    public AdminController(
            IngestionOrchestrator ingestionOrchestrator,
            IssueService issueService,
            ReportService reportService) {
        this.ingestionOrchestrator = ingestionOrchestrator;
        this.issueService = issueService;
        this.reportService = reportService;
    }
    
    /**
     * Trigger manual ingestion immediately
     * POST /admin/fetch-now
     */
    @PostMapping("/fetch-now")
    @Operation(
        summary = "Trigger manual ingestion",
        description = "Manually triggers the QIRA ticket ingestion process immediately, bypassing the scheduled cron job. " +
                     "Optionally specify maxRecords to limit the number of tickets fetched for testing. " +
                     "Returns 409 CONFLICT if ingestion is already running."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Ingestion job started successfully"),
        @ApiResponse(responseCode = "409", description = "Ingestion job already running")
    })
    public ResponseEntity<Map<String, Object>> fetchNow(
            @RequestParam(required = false) @Parameter(description = "Maximum number of records to fetch (null for all)") Integer maxRecords) {
        logger.info("Manual ingestion triggered via admin endpoint (maxRecords: {})", maxRecords);
        
        if (ingestionOrchestrator.isRunning()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ALREADY_RUNNING");
            response.put("message", "Ingestion job is already in progress");
            response.put("currentJob", ingestionOrchestrator.getLastJobStatus());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        
        // Trigger async (in a real implementation, you'd use @Async or a thread pool)
        Integer finalMaxRecords = maxRecords;
        new Thread(() -> {
            try {
                ingestionOrchestrator.fetchAndReport(finalMaxRecords);
            } catch (Exception e) {
                logger.error("Error in async ingestion", e);
            }
        }).start();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "STARTED");
        response.put("message", "Ingestion job has been triggered");
        response.put("maxRecords", maxRecords != null ? maxRecords : "ALL");
        response.put("startedAt", OffsetDateTime.now());
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Get last job run status
     * GET /admin/last-run
     */
    @GetMapping("/last-run")
    @Operation(
        summary = "Get last job execution status",
        description = "Returns detailed statistics and status of the most recent ingestion job including start time, " +
                     "duration, records fetched, inserted, updated, and any errors encountered."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Last job status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "No previous job execution found")
    })
    public ResponseEntity<JobStatus> getLastRun() {
        JobStatus lastJob = ingestionOrchestrator.getLastJobStatus();
        
        if (lastJob == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(lastJob);
    }
    
    /**
     * Get current job status
     * GET /admin/status
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get current ingestion status",
        description = "Returns whether an ingestion job is currently running and the status of the last completed job."
    )
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", ingestionOrchestrator.isRunning());
        status.put("lastJob", ingestionOrchestrator.getLastJobStatus());
        return ResponseEntity.ok(status);
    }
}
