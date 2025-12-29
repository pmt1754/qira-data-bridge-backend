package com.app.test.protocols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security Protocol Test
 * Tests security-related checks in pre-commit hook
 */
public class SecurityTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityTest.class);
    
    // Test 1: Hard-coded credentials (security risk)
    private static final String API_KEY = "sk-1234567890abcdef";
    private static final String PASSWORD = "admin123";
    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/db?password=secret";
    
    // Test 2: SQL injection vulnerability
    public void unsafeQuery(String userInput) {
        String query = "SELECT * FROM users WHERE username = '" + userInput + "'";
        logger.info("Executing: {}", query);
    }
    
    // Test 3: Path traversal vulnerability
    public void unsafeFileAccess(String fileName) {
        String filePath = "/var/data/" + fileName;
        logger.info("Accessing file: {}", filePath);
    }
    
    // Test 4: Weak random number generation
    public int generateToken() {
        return (int) (Math.random() * 1000000);
    }
}
