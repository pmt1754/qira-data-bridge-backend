package com.app.test.protocols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import com.app.service.IssueService;
import com.app.model.IssueRecord;
import com.app.dto.IssueRecordDto;
import java.util.List;
import javax.validation.Valid;

/**
 * Test class to verify pre-commit hook protocol checks
 * This is the FIXED version with proper patterns
 */
@RestController
@RequestMapping("/test")
public class ProtocolTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProtocolTestController.class);
    
    // FIXED: Constructor injection instead of field injection
    private final IssueService issueService;
    
    public ProtocolTestController(IssueService issueService) {
        this.issueService = issueService;
    }
    
    // FIXED: Uses DTOs, validation, service layer, proper logging
    @PostMapping("/create")
    public IssueRecord createIssue(@Valid @RequestBody IssueRecordDto issueDto) {
        logger.info("Creating issue with ID: {}", issueDto.getQiraId());
        
        try {
            return issueService.createIssue(issueDto);
        } catch (Exception e) {
            logger.error("Failed to create issue", e);
            throw e;
        }
    }
    
    // FIXED: Proper exception handling with logging
    @GetMapping("/safe")
    public void safeMethod() {
        try {
            int result = 10 / 0;
        } catch (ArithmeticException e) {
            logger.error("Arithmetic error occurred", e);
            throw new IllegalStateException("Cannot divide by zero", e);
        }
    }
    
    // FIXED: Null checks, proper logging
    @GetMapping("/all")
    public List<IssueRecord> getAllIssues() {
        logger.debug("Fetching all issues");
        List<IssueRecord> issues = issueService.findAll();
        
        if (issues == null || issues.isEmpty()) {
            logger.warn("No issues found");
        }
        
        return issues;
    }
    
    // FIXED: Uses parameterized queries through service layer
    @GetMapping("/search")
    public List<IssueRecord> searchIssues(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            logger.warn("Empty search query provided");
            return List.of();
        }
        
        logger.info("Searching issues with query: {}", query);
        return issueService.searchByTitle(query);
    }
    
    // FIXED: Uses proper data fetching patterns (join fetch or batch loading)
    @GetMapping("/efficient")
    public void efficientMethod() {
        logger.debug("Fetching issues with related data");
        List<IssueRecord> issuesWithRelatedData = issueService.findAllWithRelatedData();
        
        issuesWithRelatedData.forEach(issue -> 
            logger.debug("Issue: {}", issue.getQiraId())
        );
    }
    
    // FIXED: Input validation, authorization check, proper error handling
    @PostMapping("/secure")
    public String secureLogic(@Valid @RequestBody String input) {
        if (input == null || input.isEmpty()) {
            logger.warn("Invalid input received");
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        
        String processed = input.trim().toLowerCase();
        logger.info("Processing input");
        
        // Authorization check should be done via Spring Security
        // This is just a demonstration
        return processed;
    }
}
