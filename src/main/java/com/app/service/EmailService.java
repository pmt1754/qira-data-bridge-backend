package com.app.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@qira.local}")
    private String fromAddress;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Send email with attachments
     * @param toList List of recipient email addresses
     * @param subject Email subject
     * @param body Email body (HTML supported)
     * @param attachments File paths to attach
     */
    public void sendWithAttachments(List<String> toList, String subject, String body, Path... attachments) {
        if (toList == null || toList.isEmpty()) {
            logger.warn("No recipients specified, skipping email send");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress);
            helper.setTo(toList.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML
            
            // Add attachments
            if (attachments != null) {
                for (Path attachment : attachments) {
                    if (attachment != null && attachment.toFile().exists()) {
                        FileSystemResource file = new FileSystemResource(attachment.toFile());
                        helper.addAttachment(attachment.getFileName().toString(), file);
                        logger.debug("Attached file: {}", attachment.getFileName());
                    } else {
                        logger.warn("Attachment not found or null: {}", attachment);
                    }
                }
            }
            
            mailSender.send(message);
            logger.info("Email sent successfully to {} recipients: {}", toList.size(), subject);
            
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}: {}", toList, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Send simple email without attachments
     */
    public void send(List<String> toList, String subject, String body) {
        sendWithAttachments(toList, subject, body);
    }
    
    /**
     * Build HTML email body for monthly report
     */
    public String buildMonthlyReportBody(int totalFetched, int inserted, int updated, int failed, String reportMonth) {
        return String.format("""
            <html>
            <body>
                <h2>QIRA Monthly Ingestion Report - %s</h2>
                <p>The monthly ticket ingestion has completed successfully.</p>
                
                <h3>Summary:</h3>
                <table border="1" cellpadding="8" cellspacing="0">
                    <tr>
                        <td><b>Total Tickets Fetched:</b></td>
                        <td>%d</td>
                    </tr>
                    <tr>
                        <td><b>New Records Inserted:</b></td>
                        <td>%d</td>
                    </tr>
                    <tr>
                        <td><b>Records Updated:</b></td>
                        <td>%d</td>
                    </tr>
                    <tr>
                        <td><b>Failed:</b></td>
                        <td>%d</td>
                    </tr>
                </table>
                
                <p>Please find the detailed Excel report attached.</p>
                
                <p><i>This is an automated message from QIRA Data Bridge.</i></p>
            </body>
            </html>
            """, reportMonth, totalFetched, inserted, updated, failed);
    }
    
    /**
     * Build HTML email body for failure notification
     */
    public String buildFailureNotificationBody(String errorMessage, int partialFetched, String details) {
        return String.format("""
            <html>
            <body>
                <h2 style="color: red;">QIRA Monthly Ingestion Failed</h2>
                <p>The monthly ticket ingestion encountered an error and did not complete successfully.</p>
                
                <h3>Error Details:</h3>
                <p style="color: red;"><b>%s</b></p>
                
                <h3>Partial Stats:</h3>
                <p>Tickets fetched before failure: <b>%d</b></p>
                
                <h3>Additional Information:</h3>
                <pre>%s</pre>
                
                <p>Please investigate the issue and consider running a manual fetch.</p>
                
                <p><i>This is an automated message from QIRA Data Bridge.</i></p>
            </body>
            </html>
            """, errorMessage, partialFetched, details != null ? details : "No additional details available");
    }
}
