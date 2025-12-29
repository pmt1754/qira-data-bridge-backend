package com.app.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HookTest {
    private static final Logger logger = LoggerFactory.getLogger(HookTest.class);
    
    public void goodMethod() {
        // This should pass the hook checks
        logger.info("This is proper logging!");
        
        try {
            int result = 10 / 0;
        } catch (Exception e) {
            logger.error("Error occurred during calculation", e);
        }
    }
}
