package com.texttosql.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@RestController
@Slf4j
public class HealthController {

    private final Instant startTime = Instant.now();

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "API Gateway");
        health.put("timestamp", Instant.now().toString());
        health.put("uptime", java.time.Duration.between(startTime, Instant.now()).getSeconds() + " seconds");

        return ResponseEntity.ok(health);
    }
}