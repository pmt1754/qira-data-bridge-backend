package com.app.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "issue_records", 
       uniqueConstraints = @UniqueConstraint(columnNames = "qira_id"))
public class IssueRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "qira_id", nullable = false, unique = true)
    private String qiraId;
    
    private String project;
    private String priority;
    
    @Column(name = "issue_type")
    private String issueType;
    
    @Column(length = 1000)
    private String summary;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String reporter;
    
    @Column(name = "assigned_team")
    private String assignedTeam;
    
    private String assignee;
    private String status;
    
    @Column(name = "due_date")
    private OffsetDateTime dueDate;
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
    
    @Column(name = "first_response_at")
    private OffsetDateTime firstResponseAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @Column(name = "related_jira_ticket")
    private String relatedJiraTicket;
    
    @Column(name = "linked_issues", columnDefinition = "TEXT")
    private String linkedIssues;
    
    @Column(name = "support_category")
    private String supportCategory;
    
    @Column(name = "support_action_date")
    private OffsetDateTime supportActionDate;
    
    @Column(name = "support_actioned_by")
    private String supportActionedBy;
    
    @Column(name = "support_priority")
    private String supportPriority;
    
    @Column(name = "support_remark", columnDefinition = "TEXT")
    private String supportRemark;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "isbn_order_number")
    private String isbnOrderNumber;
    
    @Column(name = "book_id")
    private String bookId;
    
    private String resolution;
    
    @Column(name = "caused_by_books", columnDefinition = "TEXT")
    private String causedByBooks;
    
    @Column(name = "doi_multi_line", columnDefinition = "TEXT")
    private String doiMultiLine;
    
    @Column(name = "erratum_doi")
    private String erratumDoi;
    
    @Column(name = "error_location_books", columnDefinition = "TEXT")
    private String errorLocationBooks;
    
    @Column(name = "error_type_books")
    private String errorTypeBooks;
    
    @Column(name = "production_system_books")
    private String productionSystemBooks;
    
    @Column(name = "request_action_books", columnDefinition = "TEXT")
    private String requestActionBooks;
    
    @Column(name = "publication_status_books")
    private String publicationStatusBooks;
    
    @Column(name = "qira_tickets_category")
    private String qiraTicketsCategory;
    
    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;
    
    @Column(name = "ingested_at")
    private OffsetDateTime ingestedAt;
    
    // Constructors
    public IssueRecord() {
        this.ingestedAt = OffsetDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getQiraId() {
        return qiraId;
    }
    
    public void setQiraId(String qiraId) {
        this.qiraId = qiraId;
    }
    
    public String getProject() {
        return project;
    }
    
    public void setProject(String project) {
        this.project = project;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getIssueType() {
        return issueType;
    }
    
    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getReporter() {
        return reporter;
    }
    
    public void setReporter(String reporter) {
        this.reporter = reporter;
    }
    
    public String getAssignedTeam() {
        return assignedTeam;
    }
    
    public void setAssignedTeam(String assignedTeam) {
        this.assignedTeam = assignedTeam;
    }
    
    public String getAssignee() {
        return assignee;
    }
    
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public OffsetDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }
    
    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    public OffsetDateTime getFirstResponseAt() {
        return firstResponseAt;
    }
    
    public void setFirstResponseAt(OffsetDateTime firstResponseAt) {
        this.firstResponseAt = firstResponseAt;
    }
    
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getRelatedJiraTicket() {
        return relatedJiraTicket;
    }
    
    public void setRelatedJiraTicket(String relatedJiraTicket) {
        this.relatedJiraTicket = relatedJiraTicket;
    }
    
    public String getLinkedIssues() {
        return linkedIssues;
    }
    
    public void setLinkedIssues(String linkedIssues) {
        this.linkedIssues = linkedIssues;
    }
    
    public String getSupportCategory() {
        return supportCategory;
    }
    
    public void setSupportCategory(String supportCategory) {
        this.supportCategory = supportCategory;
    }
    
    public OffsetDateTime getSupportActionDate() {
        return supportActionDate;
    }
    
    public void setSupportActionDate(OffsetDateTime supportActionDate) {
        this.supportActionDate = supportActionDate;
    }
    
    public String getSupportActionedBy() {
        return supportActionedBy;
    }
    
    public void setSupportActionedBy(String supportActionedBy) {
        this.supportActionedBy = supportActionedBy;
    }
    
    public String getSupportPriority() {
        return supportPriority;
    }
    
    public void setSupportPriority(String supportPriority) {
        this.supportPriority = supportPriority;
    }
    
    public String getSupportRemark() {
        return supportRemark;
    }
    
    public void setSupportRemark(String supportRemark) {
        this.supportRemark = supportRemark;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public String getIsbnOrderNumber() {
        return isbnOrderNumber;
    }
    
    public void setIsbnOrderNumber(String isbnOrderNumber) {
        this.isbnOrderNumber = isbnOrderNumber;
    }
    
    public String getBookId() {
        return bookId;
    }
    
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    
    public String getResolution() {
        return resolution;
    }
    
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
    
    public String getCausedByBooks() {
        return causedByBooks;
    }
    
    public void setCausedByBooks(String causedByBooks) {
        this.causedByBooks = causedByBooks;
    }
    
    public String getDoiMultiLine() {
        return doiMultiLine;
    }
    
    public void setDoiMultiLine(String doiMultiLine) {
        this.doiMultiLine = doiMultiLine;
    }
    
    public String getErratumDoi() {
        return erratumDoi;
    }
    
    public void setErratumDoi(String erratumDoi) {
        this.erratumDoi = erratumDoi;
    }
    
    public String getErrorLocationBooks() {
        return errorLocationBooks;
    }
    
    public void setErrorLocationBooks(String errorLocationBooks) {
        this.errorLocationBooks = errorLocationBooks;
    }
    
    public String getErrorTypeBooks() {
        return errorTypeBooks;
    }
    
    public void setErrorTypeBooks(String errorTypeBooks) {
        this.errorTypeBooks = errorTypeBooks;
    }
    
    public String getProductionSystemBooks() {
        return productionSystemBooks;
    }
    
    public void setProductionSystemBooks(String productionSystemBooks) {
        this.productionSystemBooks = productionSystemBooks;
    }
    
    public String getRequestActionBooks() {
        return requestActionBooks;
    }
    
    public void setRequestActionBooks(String requestActionBooks) {
        this.requestActionBooks = requestActionBooks;
    }
    
    public String getPublicationStatusBooks() {
        return publicationStatusBooks;
    }
    
    public void setPublicationStatusBooks(String publicationStatusBooks) {
        this.publicationStatusBooks = publicationStatusBooks;
    }
    
    public String getQiraTicketsCategory() {
        return qiraTicketsCategory;
    }
    
    public void setQiraTicketsCategory(String qiraTicketsCategory) {
        this.qiraTicketsCategory = qiraTicketsCategory;
    }
    
    public String getRawJson() {
        return rawJson;
    }
    
    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }
    
    public OffsetDateTime getIngestedAt() {
        return ingestedAt;
    }
    
    public void setIngestedAt(OffsetDateTime ingestedAt) {
        this.ingestedAt = ingestedAt;
    }
}
