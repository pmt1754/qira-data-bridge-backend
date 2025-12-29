package com.app.test.protocols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;

/**
 * Performance Protocol Test
 * Tests performance-related checks in pre-commit hook
 */
public class PerformanceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTest.class);
    
    // Test 1: N+1 query pattern
    public void n1QueryProblem(List<Integer> ids) {
        List<String> results = new ArrayList<>();
        for (Integer id : ids) {
            // Simulating database call in loop - N+1 problem
            String data = fetchDataById(id);
            results.add(data);
        }
    }
    
    // Test 2: String concatenation in loop
    public String inefficientStringBuilding(List<String> items) {
        String result = "";
        for (String item : items) {
            result += item + ",";
        }
        return result;
    }
    
    // Test 3: Inefficient collection usage
    public boolean contains(List<String> list, String value) {
        for (String item : list) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    // Test 4: Unnecessary object creation in loop
    public void wasteMemory(int count) {
        for (int i = 0; i < count; i++) {
            String temp = new String("iteration: " + i);
            logger.debug("Iteration: {}", temp);
        }
    }
    
    private String fetchDataById(Integer id) {
        return "data-" + id;
    }
}
