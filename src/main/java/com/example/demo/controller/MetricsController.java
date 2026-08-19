package com.example.demo.controller;

import com.example.demo.service.MetricsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/shadow/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(
            MetricsService metricsService) {

        this.metricsService =
                metricsService;
    }

    // ==========================================
    // GET METRICS
    // ==========================================

    @GetMapping
    public Map<String, Object> getMetrics() {

        return metricsService.getMetrics();
    }

    // ==========================================
    // RESET METRICS
    // ==========================================

    @PostMapping("/reset")
    public String resetMetrics() {

        metricsService.resetMetrics();

        return "Metrics reset successfully.";
    }
}