package com.devbuild.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker
 * Provides graceful error responses when downstream services are unavailable
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("User service circuit breaker triggered");
        return buildFallbackResponse("User Service",
                "The user service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/defense-service")
    public ResponseEntity<Map<String, Object>> defenseServiceFallback() {
        log.warn("Defense service circuit breaker triggered");
        return buildFallbackResponse("Defense Service",
                "The defense service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/inscription-service")
    public ResponseEntity<Map<String, Object>> inscriptionServiceFallback() {
        log.warn("Inscription service circuit breaker triggered");
        return buildFallbackResponse("Inscription Service",
                "The inscription service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/notification-service")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        log.warn("Notification service circuit breaker triggered");
        return buildFallbackResponse("Notification Service",
                "The notification service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/batch-service")
    public ResponseEntity<Map<String, Object>> batchServiceFallback() {
        log.warn("Batch service circuit breaker triggered");
        return buildFallbackResponse("Batch Service",
                "The batch service is temporarily unavailable. Please try again later.");
    }

    /**
     * Build standardized fallback response
     */
    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String serviceName, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Service Unavailable");
        response.put("service", serviceName);
        response.put("message", message);
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
