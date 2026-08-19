package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private final AtomicLong queriesReplayed =
            new AtomicLong(0);

    private final AtomicLong errors =
            new AtomicLong(0);

    private final AtomicLong totalEvents =
            new AtomicLong(0);

    // ==========================================
    // RECORD SUCCESSFUL CDC EVENT
    // ==========================================

    public void recordReplay() {

        totalEvents.incrementAndGet();
        queriesReplayed.incrementAndGet();
    }

    // ==========================================
    // RECORD CDC ERROR
    // ==========================================

    public void recordError() {

        totalEvents.incrementAndGet();
        errors.incrementAndGet();
    }

    // ==========================================
    // GET METRICS
    // ==========================================

    public Map<String, Object> getMetrics() {

        long replayed =
                queriesReplayed.get();

        long errorCount =
                errors.get();

        long total =
                totalEvents.get();

        double errorRate = 0.0;

        if (total > 0) {

            errorRate =
                    ((double) errorCount / total) * 100.0;
        }

        Map<String, Object> metrics =
                new LinkedHashMap<>();

        metrics.put(
                "queriesReplayed",
                replayed);

        metrics.put(
                "errors",
                errorCount);

        metrics.put(
                "totalEvents",
                total);

        metrics.put(
                "errorRate",
                Math.round(errorRate * 100.0) / 100.0);

        return metrics;
    }

    // ==========================================
    // RESET METRICS
    // ==========================================

    public void resetMetrics() {

        queriesReplayed.set(0);
        errors.set(0);
        totalEvents.set(0);
    }
}