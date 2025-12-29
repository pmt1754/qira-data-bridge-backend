package com.app.controller;

import com.app.model.IssueRecord;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/issues")
@Tag(name = "Issues", description = "Issue data export and statistics endpoints")
public class IssueController {
    
    private static final Logger logger = LoggerFactory.getLogger(IssueController.class);
    
    private final IssueService issueService;
    private final ReportService reportService;
    
    public IssueController(IssueService issueService, ReportService reportService) {
        this.issueService = issueService;
        this.reportService = reportService;
    }
    
    /**
     * Export issues to Excel with date range and optional team filter
     * GET /issues/export?from=2024-01-01&to=2024-12-31&team=Engineering
     */
    @GetMapping("/export")
    @Operation(
        summary = "Export issues to Excel",
        description = "Generates and downloads an Excel report containing issue records filtered by date range. " +
                     "Optionally filter by team. The Excel file includes a summary sheet with statistics and a " +
                     "detailed data sheet with all issue fields."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Excel file generated and downloaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date format or parameters"),
        @ApiResponse(responseCode = "500", description = "Error generating Excel file")
    })
    public ResponseEntity<InputStreamResource> exportIssues(
            @Parameter(description = "Start date (inclusive) in YYYY-MM-DD format", example = "2024-01-01", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            
            @Parameter(description = "End date (inclusive) in YYYY-MM-DD format", example = "2024-12-31", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            
            @Parameter(description = "Optional team name filter", example = "Engineering")
            @RequestParam(required = false) String team) {
        
        logger.info("Export request: from={}, to={}, team={}", from, to, team);
        
        try {
            // Convert LocalDate to OffsetDateTime
            OffsetDateTime fromDateTime = from.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime toDateTime = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);
            
            // Query data
            List<IssueRecord> records;
            if (team != null && !team.isBlank()) {
                records = issueService.findByDateRangeAndTeam(fromDateTime, toDateTime, team);
            } else {
                records = issueService.findByDateRange(fromDateTime, toDateTime);
            }
            
            logger.info("Found {} records for export", records.size());
            
            // Generate temporary Excel file
            String fileName = String.format("QIRA_Export_%s_to_%s.xlsx",
                from.format(DateTimeFormatter.ISO_DATE),
                to.format(DateTimeFormatter.ISO_DATE));
            
            Path tempFile = Files.createTempFile("qira_export_", ".xlsx");
            reportService.generateExcel(records, tempFile);
            
            // Stream file to response
            FileInputStream fileInputStream = new FileInputStream(tempFile.toFile());
            InputStreamResource resource = new InputStreamResource(fileInputStream);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");
            
            // Schedule temp file deletion after response
            // In production, use a proper cleanup mechanism
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Wait 5 seconds
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    logger.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }).start();
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Error generating export", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get issue count by status
     */
    @GetMapping("/stats/by-status")
    public ResponseEntity<List<Object[]>> getStatsByStatus() {
        // This would query the repository for counts
        // Returning simple response for now
        return ResponseEntity.ok(List.of());
    }
}
