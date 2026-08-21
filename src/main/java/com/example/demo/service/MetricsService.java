package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    // RECENT CDC EVENTS
    // ==========================================

    private final List<Map<String, Object>> recentEvents =
            new ArrayList<>();

    private static final int MAX_EVENTS = 20;

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
    // RECORD CDC EVENT DETAILS
    // ==========================================

    public synchronized void recordEvent(
            String operation,
            int id,
            String name,
            String salary) {

        Map<String, Object> event =
                new LinkedHashMap<>();

        event.put(
                "operation",
                operation);

        event.put(
                "id",
                id);

        event.put(
                "name",
                name);

        event.put(
                "salary",
                salary);

        event.put(
                "timestamp",
                System.currentTimeMillis());

        recentEvents.add(0, event);

        // Keep only the latest 20 events
        if (recentEvents.size() > MAX_EVENTS) {

            recentEvents.remove(
                    recentEvents.size() - 1);
        }
    }

    // ==========================================
    // GET RECENT CDC EVENTS
    // ==========================================

    public synchronized List<Map<String, Object>>
    getRecentEvents() {

        return new ArrayList<>(
                recentEvents);
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
                    ((double) errorCount / total)
                            * 100.0;
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
                Math.round(errorRate * 100.0)
                        / 100.0);

        return metrics;
    }

    // ==========================================
    // RESET METRICS
    // ==========================================

    public synchronized void resetMetrics() {

        queriesReplayed.set(0);
        errors.set(0);
        totalEvents.set(0);

        recentEvents.clear();
    }
}