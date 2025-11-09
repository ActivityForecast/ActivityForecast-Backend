package com.activityforecastbackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/simple")
    public ResponseEntity<Map<String, Object>> simpleHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "ActivityForecast Backend");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> detailedStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 기본 상태 정보
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().toString());
        status.put("timezone", ZoneId.systemDefault().toString());
        status.put("service", "ActivityForecast Backend");
        status.put("version", "1.0.0");
        
        // 시스템 정보
        Map<String, Object> system = new HashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        system.put("processors", Runtime.getRuntime().availableProcessors());
        
        // 메모리 정보
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("totalMemory", runtime.totalMemory());
        memory.put("freeMemory", runtime.freeMemory());
        memory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        memory.put("maxMemory", runtime.maxMemory());
        
        system.put("memory", memory);
        status.put("system", system);
        
        // 애플리케이션 정보
        Map<String, Object> application = new HashMap<>();
        application.put("profile", System.getProperty("spring.profiles.active", "default"));
        application.put("port", System.getProperty("server.port", "8080"));
        
        status.put("application", application);
        
        return ResponseEntity.ok(status);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> defaultHealth() {
        return simpleHealth();
    }
}