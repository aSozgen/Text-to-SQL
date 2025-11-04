package com.texttosql.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/service")
    public ResponseEntity<Map<String, Object>> serviceFallback() {
        log.warn("Circuit breaker triggered - backend service unavailable");

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Service Unavailable");
        response.put("message", "The backend service is temporarily unavailable. Please try again later.");
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        log.warn("Circuit breaker triggered - auth service unavailable");

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Authentication Service Unavailable");
        response.put("message", "The authentication service is temporarily unavailable. Please try again later.");
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}