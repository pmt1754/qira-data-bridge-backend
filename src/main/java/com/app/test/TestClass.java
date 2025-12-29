package com.app.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClass {
    private static final Logger logger = LoggerFactory.getLogger(TestClass.class);
    
    public void goodMethod() {
        // Proper logging instead of System.out.println
        logger.info("Debug output");

        try {
            // some code
        } catch (Exception e) {
            logger.error("Error occurred", e);
        }
    }
}
