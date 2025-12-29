package com.app.service;

import com.app.client.QiraClient;
import com.app.dto.JobStatus;
import com.app.dto.UpsertStats;
import com.app.model.IssueRecord;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class IngestionOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(IngestionOrchestrator.class);
    
    private final QiraClient qiraClient;
    private final TicketMapper ticketMapper;
    private final IssueService issueService;
    private final ReportService reportService;
    private final EmailService emailService;
    private final MeterRegistry meterRegistry;
    
    @Value("${report.output-dir:./reports}")
    private String reportOutputDir;
    
    @Value("${mail.recipients.management:}")
    private String managementRecipients;
    
    @Value("${mail.recipients.support:}")
    private String supportRecipients;
    
    private JobStatus lastJobStatus;
    private volatile boolean isRunning = false;
    
    public IngestionOrchestrator(
            QiraClient qiraClient,
            TicketMapper ticketMapper,
            IssueService issueService,
            ReportService reportService,
            EmailService emailService,
            MeterRegistry meterRegistry) {
        this.qiraClient = qiraClient;
        this.ticketMapper = ticketMapper;
        this.issueService = issueService;
        this.reportService = reportService;
        this.emailService = emailService;
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Scheduled monthly ingestion job
     * Runs at 2 AM on the 1st of every month by default
     */
    @Scheduled(cron = "${scheduler.monthly.cron:0 0 2 1 * ?}")
    public void scheduledFetchAndReport() {
        logger.info("==========================================================");
        logger.info("SCHEDULED MONTHLY INGESTION JOB TRIGGERED");
        logger.info("Scheduled Time: {}", OffsetDateTime.now());
        logger.info("Cron Expression: ${scheduler.monthly.cron:0 0 2 1 * ?}");
        logger.info("==========================================================");
        fetchAndReport(null); // Fetch all records for scheduled job
    }
    
    /**
     * Main ingestion and reporting flow
     * Can be called manually or by scheduler
     * @param maxRecords Maximum number of records to fetch (null for all records)
     */
    public synchronized JobStatus fetchAndReport(Integer maxRecords) {
        if (isRunning) {
            logger.warn("⚠️  Ingestion already running, skipping this trigger");
            logger.warn("Current job started at: {}", lastJobStatus.getStartedAt());
            return lastJobStatus;
        }
        
        isRunning = true;
        JobStatus jobStatus = new JobStatus();
        jobStatus.setStartedAt(OffsetDateTime.now());
        lastJobStatus = jobStatus;
        
        try {
            logger.info("╔══════════════════════════════════════════════════════════╗");
            logger.info("║    QIRA TICKET INGESTION & REPORTING JOB STARTED        ║");
            logger.info("╚══════════════════════════════════════════════════════════╝");
            logger.info("Job Start Time: {}", jobStatus.getStartedAt());
            logger.info("Max Records Limit: {}", maxRecords != null ? maxRecords : "ALL");
            
            // Step 1: Fetch tickets from QIRA API
            logger.info("📥 STEP 1: Fetching tickets from QIRA API...");
            List<JsonNode> allTickets;
            if (maxRecords != null && maxRecords > 0) {
                allTickets = qiraClient.fetchLimitedTickets(maxRecords);
                logger.info("✅ Successfully fetched {} tickets (limited to {})", allTickets.size(), maxRecords);
            } else {
                allTickets = qiraClient.fetchAllTickets();
                logger.info("✅ Successfully fetched {} tickets from QIRA API", allTickets.size());
            }
            
            jobStatus.setTotalFetched(allTickets.size());
            
            // Update metrics
            meterRegistry.counter("qira.ingestion.fetched").increment(allTickets.size());
            
            // Step 2: Map tickets to entity records
            logger.info("🔄 STEP 2: Mapping {} tickets to database records...", allTickets.size());
            List<IssueRecord> mappedRecords = new ArrayList<>();
            for (JsonNode ticket : allTickets) {
                try {
                    IssueRecord record = ticketMapper.map(ticket);
                    if (record != null) {
                        mappedRecords.add(record);
                    } else {
                        jobStatus.addError("Failed to map ticket (missing qiraId)");
                    }
                } catch (Exception e) {
                    logger.error("Error mapping ticket", e);
                    jobStatus.addError("Mapping error: " + e.getMessage());
                }
            }
            
            logger.info("✅ Successfully mapped {} out of {} tickets", mappedRecords.size(), allTickets.size());
            
            // Step 3: Bulk upsert records
            logger.info("💾 STEP 3: Performing bulk upsert to database...");
            UpsertStats upsertStats = issueService.upsertBulk(mappedRecords);
            jobStatus.setInserted(upsertStats.getInserted());
            jobStatus.setUpdated(upsertStats.getUpdated());
            jobStatus.setFailed(upsertStats.getFailed());
            
            logger.info("✅ Upsert completed - Inserted: {}, Updated: {}, Failed: {}", 
                       upsertStats.getInserted(), upsertStats.getUpdated(), upsertStats.getFailed());
            
            // Update metrics
            meterRegistry.counter("qira.ingestion.inserted").increment(upsertStats.getInserted());
            meterRegistry.counter("qira.ingestion.updated").increment(upsertStats.getUpdated());
            meterRegistry.counter("qira.ingestion.failed").increment(upsertStats.getFailed());
            
            // Step 4: Generate report for last month
            logger.info("📊 STEP 4: Generating monthly report...");
            OffsetDateTime lastMonthStart = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
            OffsetDateTime lastMonthEnd = YearMonth.now().atDay(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).minusNanos(1);
            
            List<IssueRecord> reportRecords = issueService.findByDateRange(lastMonthStart, lastMonthEnd);
            logger.info("Found {} records from last month for report", reportRecords.size());
            
            // Create report directory if needed
            Path reportDir = Paths.get(reportOutputDir);
            if (!Files.exists(reportDir)) {
                Files.createDirectories(reportDir);
                logger.info("Created report directory: {}", reportDir);
            }
            
            String fileName = String.format("QIRA_Report_%s.xlsx", 
                YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM")));
            Path reportPath = reportDir.resolve(fileName);
            
            reportService.generateExcel(reportRecords, reportPath);
            logger.info("✅ Report generated successfully: {}", reportPath);
            
            // Step 5: Send email with report (DISABLED)
            logger.info("📧 STEP 5: Email notification disabled (skipping)...");
            logger.info("Report available at: {}", reportPath);
            // Email service temporarily disabled
            // Uncomment below to re-enable
            /*
            List<String> recipients = parseRecipients(managementRecipients);
            if (!recipients.isEmpty()) {
                logger.info("Email recipients: {}", recipients);
                String subject = String.format("QIRA Monthly Report - %s", 
                    YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                
                String body = emailService.buildMonthlyReportBody(
                    jobStatus.getTotalFetched(),
                    jobStatus.getInserted(),
                    jobStatus.getUpdated(),
                    jobStatus.getFailed(),
                    YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                );
                
                emailService.sendWithAttachments(recipients, subject, body, reportPath);
                logger.info("✅ Report email sent successfully to {} recipients", recipients.size());
            } else {
                logger.warn("⚠️  No email recipients configured for management");
            }
            */
            
            // Mark job as completed
            jobStatus.markCompleted();
            
            // Update metrics
            meterRegistry.timer("qira.ingestion.duration").record(jobStatus.getDurationSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            
            logger.info("╔══════════════════════════════════════════════════════════╗");
            logger.info("║    QIRA INGESTION JOB COMPLETED SUCCESSFULLY ✅         ║");
            logger.info("╚══════════════════════════════════════════════════════════╝");
            logger.info("Job End Time: {}", jobStatus.getFinishedAt());
            logger.info("Total Duration: {} seconds", jobStatus.getDurationSeconds());
            logger.info("Summary: Fetched={}, Inserted={}, Updated={}, Failed={}", 
                       jobStatus.getTotalFetched(), 
                       jobStatus.getInserted(), 
                       jobStatus.getUpdated(), 
                       jobStatus.getFailed());
            
        } catch (Exception e) {
            logger.error("╔══════════════════════════════════════════════════════════╗");
            logger.error("║    QIRA INGESTION JOB FAILED ❌                         ║");
            logger.error("╚══════════════════════════════════════════════════════════╝");
            logger.error("Error: {}", e.getMessage(), e);
            jobStatus.markFailed("Fatal error: " + e.getMessage());
            
            // Send failure notification (DISABLED)
            logger.info("📧 Failure notification email disabled (skipping)...");
            logger.info("Error details logged above");
            // Email service temporarily disabled
            /*
            try {
                List<String> supportList = parseRecipients(supportRecipients);
                if (!supportList.isEmpty()) {
                    logger.info("Failure notification recipients: {}", supportList);
                    String subject = "QIRA Monthly Ingestion FAILED";
                    String body = emailService.buildFailureNotificationBody(
                        e.getMessage(),
                        jobStatus.getTotalFetched(),
                        Arrays.toString(e.getStackTrace()).substring(0, Math.min(500, Arrays.toString(e.getStackTrace()).length()))
                    );
                    emailService.send(supportList, subject, body);
                    logger.info("✅ Failure notification sent successfully");
                } else {
                    logger.warn("⚠️  No support recipients configured for failure notifications");
                }
            } catch (Exception emailEx) {
                logger.error("❌ Failed to send failure notification email", emailEx);
            }
            */
            
            // Update failure metric
            meterRegistry.counter("qira.ingestion.job.failed").increment();
            
        } finally {
            isRunning = false;
            logger.info("==========================================================");
            logger.info("Job execution completed, releasing lock");
            logger.info("==========================================================");
        }
        
        return jobStatus;
    }
    
    /**
     * Get the status of the last ingestion job
     */
    public JobStatus getLastJobStatus() {
        return lastJobStatus;
    }
    
    /**
     * Check if ingestion is currently running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Parse comma-separated email recipients
     */
    private List<String> parseRecipients(String recipientsStr) {
        if (recipientsStr == null || recipientsStr.isBlank()) {
            return List.of();
        }
        return Arrays.stream(recipientsStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
