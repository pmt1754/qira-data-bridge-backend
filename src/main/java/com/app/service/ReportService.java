package com.app.service;

import com.app.model.IssueRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Generate Excel report from issue records
     * @param records List of issue records
     * @param outputPath Path where Excel file should be saved
     * @return Path to generated file
     */
    public Path generateExcel(List<IssueRecord> records, Path outputPath) throws IOException {
        logger.info("Generating Excel report for {} records to {}", records.size(), outputPath);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create summary sheet
            createSummarySheet(workbook, records);
            
            // Create data sheet
            createDataSheet(workbook, records);
            
            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(outputPath.toFile())) {
                workbook.write(fileOut);
            }
            
            logger.info("Excel report generated successfully: {}", outputPath);
            return outputPath;
            
        } catch (IOException e) {
            logger.error("Failed to generate Excel report", e);
            throw e;
        }
    }
    
    /**
     * Create summary sheet with statistics
     */
    private void createSummarySheet(Workbook workbook, List<IssueRecord> records) {
        Sheet sheet = workbook.createSheet("Summary");
        
        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = workbook.createCellStyle();
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("QIRA Tickets Report Summary");
        titleCell.setCellStyle(headerStyle);
        
        rowNum++; // Blank row
        
        // Total count
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue("Total Tickets:");
        totalRow.createCell(1).setCellValue(records.size());
        
        // Generation date
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Generated:");
        dateRow.createCell(1).setCellValue(OffsetDateTime.now().format(DATE_FORMATTER));
        
        rowNum++; // Blank row
        
        // Count by status
        Row statusHeaderRow = sheet.createRow(rowNum++);
        Cell statusHeader = statusHeaderRow.createCell(0);
        statusHeader.setCellValue("Status Breakdown");
        statusHeader.setCellStyle(headerStyle);
        
        Map<String, Long> statusCounts = records.stream()
            .filter(r -> r.getStatus() != null)
            .collect(Collectors.groupingBy(IssueRecord::getStatus, Collectors.counting()));
        
        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }
        
        rowNum++; // Blank row
        
        // Top issue types
        Row typeHeaderRow = sheet.createRow(rowNum++);
        Cell typeHeader = typeHeaderRow.createCell(0);
        typeHeader.setCellValue("Top Issue Types");
        typeHeader.setCellStyle(headerStyle);
        
        final int typeStartRow = rowNum;
        Map<String, Long> typeCounts = records.stream()
            .filter(r -> r.getIssueType() != null)
            .collect(Collectors.groupingBy(IssueRecord::getIssueType, Collectors.counting()));
        
        AtomicInteger typeRowNum = new AtomicInteger(typeStartRow);
        typeCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> {
                Row row = sheet.createRow(typeRowNum.getAndIncrement());
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            });
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    /**
     * Create data sheet with all records
     */
    private void createDataSheet(Workbook workbook, List<IssueRecord> records) {
        Sheet sheet = workbook.createSheet("Tickets");
        
        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        
        int rowNum = 0;
        
        // Header row
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {
            "QIRA ID", "Project", "Priority", "Issue Type", "Summary", "Description",
            "Reporter", "Assigned Team", "Assignee", "Status", "Due Date", "Created",
            "Resolved", "First Response", "Updated", "Related Jira", "Linked Issues",
            "Support Category", "Support Action Date", "Support Actioned By", 
            "Support Priority", "Support Remark", "Comment", "ISBN/Order Number",
            "Book ID", "Resolution", "Caused By Books", "DOI", "Erratum DOI",
            "Error Location", "Error Type", "Production System", "Request Action",
            "Publication Status", "Category"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        for (IssueRecord record : records) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            setCellValue(row, colNum++, record.getQiraId());
            setCellValue(row, colNum++, record.getProject());
            setCellValue(row, colNum++, record.getPriority());
            setCellValue(row, colNum++, record.getIssueType());
            setCellValue(row, colNum++, record.getSummary());
            setCellValue(row, colNum++, record.getDescription());
            setCellValue(row, colNum++, record.getReporter());
            setCellValue(row, colNum++, record.getAssignedTeam());
            setCellValue(row, colNum++, record.getAssignee());
            setCellValue(row, colNum++, record.getStatus());
            setDateCellValue(row, colNum++, record.getDueDate(), dateStyle);
            setDateCellValue(row, colNum++, record.getCreatedAt(), dateStyle);
            setDateCellValue(row, colNum++, record.getResolvedAt(), dateStyle);
            setDateCellValue(row, colNum++, record.getFirstResponseAt(), dateStyle);
            setDateCellValue(row, colNum++, record.getUpdatedAt(), dateStyle);
            setCellValue(row, colNum++, record.getRelatedJiraTicket());
            setCellValue(row, colNum++, record.getLinkedIssues());
            setCellValue(row, colNum++, record.getSupportCategory());
            setDateCellValue(row, colNum++, record.getSupportActionDate(), dateStyle);
            setCellValue(row, colNum++, record.getSupportActionedBy());
            setCellValue(row, colNum++, record.getSupportPriority());
            setCellValue(row, colNum++, record.getSupportRemark());
            setCellValue(row, colNum++, record.getComment());
            setCellValue(row, colNum++, record.getIsbnOrderNumber());
            setCellValue(row, colNum++, record.getBookId());
            setCellValue(row, colNum++, record.getResolution());
            setCellValue(row, colNum++, record.getCausedByBooks());
            setCellValue(row, colNum++, record.getDoiMultiLine());
            setCellValue(row, colNum++, record.getErratumDoi());
            setCellValue(row, colNum++, record.getErrorLocationBooks());
            setCellValue(row, colNum++, record.getErrorTypeBooks());
            setCellValue(row, colNum++, record.getProductionSystemBooks());
            setCellValue(row, colNum++, record.getRequestActionBooks());
            setCellValue(row, colNum++, record.getPublicationStatusBooks());
            setCellValue(row, colNum++, record.getQiraTicketsCategory());
        }
        
        // Auto-size important columns
        for (int i = 0; i < Math.min(10, headers.length); i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Create header cell style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    /**
     * Set cell value handling nulls
     */
    private void setCellValue(Row row, int colNum, String value) {
        Cell cell = row.createCell(colNum);
        if (value != null) {
            cell.setCellValue(value);
        }
    }
    
    /**
     * Set date cell value
     */
    private void setDateCellValue(Row row, int colNum, OffsetDateTime date, CellStyle dateStyle) {
        Cell cell = row.createCell(colNum);
        if (date != null) {
            cell.setCellValue(date.format(DATE_FORMATTER));
            cell.setCellStyle(dateStyle);
        }
    }
}
