package com.app.service;

import com.app.model.IssueRecord;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class TicketMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketMapper.class);
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_INSTANT,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };
    
    /**
     * Map JSON ticket data to IssueRecord entity
     * Handles both flat and nested JIRA-style JSON structures
     * @param ticketNode JSON node containing ticket data
     * @return Mapped IssueRecord
     */
    public IssueRecord map(JsonNode ticketNode) {
        IssueRecord record = new IssueRecord();
        
        try {
            // Store raw JSON for troubleshooting
            record.setRawJson(ticketNode.toString());
            
            // Check if this is JIRA-style format with "key" and "fields"
            JsonNode fieldsNode = ticketNode;
            if (ticketNode.has("fields")) {
                fieldsNode = ticketNode.get("fields");
                // JIRA key is the qiraId
                record.setQiraId(ticketNode.has("key") ? ticketNode.get("key").asText() : null);
            } else {
                // Flat format - extract qiraId directly
                record.setQiraId(getTextValue(ticketNode, "Qira id", "qiraId", "id", "key"));
            }
            
            // Map project (can be object with "key" or "name", or direct string)
            if (fieldsNode.has("project") && fieldsNode.get("project").isObject()) {
                JsonNode projectNode = fieldsNode.get("project");
                String projectName = projectNode.has("name") ? projectNode.get("name").asText() : 
                                    projectNode.has("key") ? projectNode.get("key").asText() : null;
                record.setProject(projectName);
            } else {
                record.setProject(getTextValue(fieldsNode, "Project", "project"));
            }
            
            // Map priority (can be object with "name" or direct string)
            if (fieldsNode.has("priority") && fieldsNode.get("priority").isObject()) {
                record.setPriority(fieldsNode.get("priority").get("name").asText());
            } else {
                record.setPriority(getTextValue(fieldsNode, "Priority", "priority"));
            }
            
            // Map issue type (can be object with "name" or direct string)
            if (fieldsNode.has("issuetype") && fieldsNode.get("issuetype").isObject()) {
                record.setIssueType(fieldsNode.get("issuetype").get("name").asText());
            } else {
                record.setIssueType(getTextValue(fieldsNode, "Issue Type", "issueType", "issuetype", "type"));
            }
            
            // Map summary and description
            record.setSummary(getTextValue(fieldsNode, "Summary", "summary"));
            
            // Description can be in multiple fields
            String description = getTextValue(fieldsNode, "Description", "description");
            if (description == null) {
                description = getTextValue(fieldsNode, "customfield_20797"); // Alternate description
            }
            record.setDescription(description);
            
            // Map reporter (can be object with displayName/emailAddress or direct string)
            if (fieldsNode.has("reporter") && fieldsNode.get("reporter").isObject()) {
                JsonNode reporterNode = fieldsNode.get("reporter");
                String reporterName = reporterNode.has("displayName") ? reporterNode.get("displayName").asText() :
                                     reporterNode.has("emailAddress") ? reporterNode.get("emailAddress").asText() : null;
                record.setReporter(reporterName);
            } else {
                record.setReporter(getTextValue(fieldsNode, "Reporter", "reporter"));
            }
            
            // Map assignee (can be object with displayName/emailAddress or direct string)
            if (fieldsNode.has("assignee") && fieldsNode.get("assignee").isObject()) {
                JsonNode assigneeNode = fieldsNode.get("assignee");
                String assigneeName = assigneeNode.has("displayName") ? assigneeNode.get("displayName").asText() :
                                     assigneeNode.has("emailAddress") ? assigneeNode.get("emailAddress").asText() : null;
                record.setAssignee(assigneeName);
            } else {
                record.setAssignee(getTextValue(fieldsNode, "Assignee", "assignee"));
            }
            
            // Map status (can be object with "name" or direct string)
            if (fieldsNode.has("status") && fieldsNode.get("status").isObject()) {
                record.setStatus(fieldsNode.get("status").get("name").asText());
            } else {
                record.setStatus(getTextValue(fieldsNode, "Status", "status"));
            }
            
            record.setAssignedTeam(getTextValue(fieldsNode, "Assigned Team", "assignedTeam", "team"));
            
            // Date fields
            record.setDueDate(getDateValue(fieldsNode, "Due Date", "dueDate", "duedate"));
            record.setCreatedAt(getDateValue(fieldsNode, "Created", "createdAt", "created"));
            record.setResolvedAt(getDateValue(fieldsNode, "Resolved", "resolvedAt", "resolved", "resolutiondate"));
            record.setFirstResponseAt(getDateValue(fieldsNode, "Date of First Response", "firstResponseAt", "dateOfFirstResponse"));
            record.setUpdatedAt(getDateValue(fieldsNode, "Updated", "updatedAt", "updated"));
            
            // Other fields
            record.setRelatedJiraTicket(getTextValue(fieldsNode, "Related Jira Ticket", "relatedJiraTicket"));
            record.setLinkedIssues(getTextValue(fieldsNode, "Linked Issues", "linkedIssues"));
            
            // Support category - can be in customfield_22883 or direct field
            String supportCategory = null;
            if (fieldsNode.has("customfield_22883") && fieldsNode.get("customfield_22883").isObject()) {
                supportCategory = fieldsNode.get("customfield_22883").get("value").asText();
            } else {
                supportCategory = getTextValue(fieldsNode, "Support Category", "supportCategory", "customfield_22883");
            }
            record.setSupportCategory(supportCategory);
            
            record.setSupportActionDate(getDateValue(fieldsNode, "Support Action Date", "supportActionDate"));
            
            // Support actioned by - can be array of objects or direct string
            String supportActionedBy = null;
            if (fieldsNode.has("customfield_22886") && fieldsNode.get("customfield_22886").isArray()) {
                List<String> users = new ArrayList<>();
                fieldsNode.get("customfield_22886").forEach(userNode -> {
                    if (userNode.has("displayName")) {
                        users.add(userNode.get("displayName").asText());
                    }
                });
                supportActionedBy = users.isEmpty() ? null : String.join(", ", users);
            } else {
                supportActionedBy = getTextValue(fieldsNode, "Support Actioned By", "supportActionedBy", "customfield_22886");
            }
            record.setSupportActionedBy(supportActionedBy);
            
            // Support priority - can be in customfield_22884 or direct field
            String supportPriority = null;
            if (fieldsNode.has("customfield_22884") && fieldsNode.get("customfield_22884").isObject()) {
                supportPriority = fieldsNode.get("customfield_22884").get("value").asText();
            } else {
                supportPriority = getTextValue(fieldsNode, "Support Priority", "supportPriority", "customfield_22884");
            }
            record.setSupportPriority(supportPriority);
            
            // Support remark - can be in customfield_22885
            record.setSupportRemark(getTextValue(fieldsNode, "Support Remark", "supportRemark", "customfield_22885"));
            
            record.setComment(getTextValue(fieldsNode, "Comment", "comment", "comments"));
            record.setIsbnOrderNumber(getTextValue(fieldsNode, "ISBN/OrderNumber", "isbnOrderNumber", "isbn"));
            record.setBookId(getTextValue(fieldsNode, "BookID", "bookId"));
            record.setResolution(getTextValue(fieldsNode, "Resolution", "resolution"));
            record.setCausedByBooks(getTextValue(fieldsNode, "Caused by (Books)", "causedByBooks"));
            
            // Handle multiple DOI values - join with newline
            record.setDoiMultiLine(getMultiLineValue(fieldsNode, "DOI (multiple entries possible - text area)", "doiMultiLine", "doi"));
            
            record.setErratumDoi(getTextValue(fieldsNode, "Erratum DOI", "erratumDoi"));
            record.setErrorLocationBooks(getTextValue(fieldsNode, "Error Location (Books)", "errorLocationBooks"));
            record.setErrorTypeBooks(getTextValue(fieldsNode, "Error Type (Books)", "errorTypeBooks"));
            record.setProductionSystemBooks(getTextValue(fieldsNode, "Production System (Books)", "productionSystemBooks"));
            record.setRequestActionBooks(getTextValue(fieldsNode, "Request Action (Books)", "requestActionBooks"));
            record.setPublicationStatusBooks(getTextValue(fieldsNode, "Publication Status (Books)", "publicationStatusBooks"));
            record.setQiraTicketsCategory(getTextValue(fieldsNode, "Qira tickets Category", "qiraTicketsCategory", "category"));
            
            // Validate required field
            if (record.getQiraId() == null || record.getQiraId().isBlank()) {
                logger.warn("Ticket missing qiraId, skipping. Raw: {}", ticketNode.toString().substring(0, Math.min(200, ticketNode.toString().length())));
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error mapping ticket, will log for investigation: {}", e.getMessage());
            logger.debug("Failed ticket data: {}", ticketNode);
            // Return record with raw JSON so we can investigate later
            if (record.getQiraId() != null) {
                return record;
            }
            return null;
        }
        
        return record;
    }
    
    /**
     * Get text value from JSON node, trying multiple field name variations
     */
    private String getTextValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            // Try exact match first
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                String value = node.get(fieldName).asText();
                return value.isBlank() ? null : value;
            }
            
            // Try case-insensitive match
            node.fields().forEachRemaining(entry -> {
                if (entry.getKey().equalsIgnoreCase(fieldName) && !entry.getValue().isNull()) {
                    // Will be picked up on next iteration
                }
            });
        }
        return null;
    }
    
    /**
     * Get date value, trying multiple formats
     */
    private OffsetDateTime getDateValue(JsonNode node, String... fieldNames) {
        String dateStr = getTextValue(node, fieldNames);
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return OffsetDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        
        // Try parsing as ISO instant and converting
        try {
            return OffsetDateTime.parse(dateStr + "Z");
        } catch (DateTimeParseException e) {
            logger.warn("Could not parse date '{}' for fields {}", dateStr, String.join(", ", fieldNames));
            return null;
        }
    }
    
    /**
     * Get multi-line value (handles arrays or newline-separated text)
     */
    private String getMultiLineValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                JsonNode fieldNode = node.get(fieldName);
                
                // Handle array
                if (fieldNode.isArray()) {
                    List<String> values = new ArrayList<>();
                    fieldNode.forEach(item -> {
                        if (!item.isNull()) {
                            values.add(item.asText());
                        }
                    });
                    return values.isEmpty() ? null : String.join("\n", values);
                }
                
                // Handle regular text
                if (!fieldNode.isNull()) {
                    String value = fieldNode.asText();
                    return value.isBlank() ? null : value;
                }
            }
        }
        return null;
    }
}
