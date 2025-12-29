package com.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Application health check endpoints")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns the health status of the application")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", OffsetDateTime.now());
        response.put("message", "QIRA Data Bridge Backend is running");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    @Operation(summary = "Application info", description = "Returns basic information about the application")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "QIRA Data Bridge Backend");
        response.put("version", "1.0.0");
        response.put("description", "Monthly QIRA ticket ingestion and reporting system");
        response.put("features", new String[]{
            "QIRA API Integration",
            "Automated Monthly Ingestion",
            "Excel Report Generation",
            "Email Notifications",
            "Bulk Upsert Processing"
        });
        return ResponseEntity.ok(response);
    }
}
