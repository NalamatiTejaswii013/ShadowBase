package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ErrorLogService {

    // ==========================================
    // ERROR LOG STORAGE
    // ==========================================

    private final List<Map<String, Object>> errorLogs =
            new ArrayList<>();

    private static final int MAX_ERROR_LOGS = 50;

    // ==========================================
    // RECORD ERROR
    // ==========================================

    public synchronized void recordError(
            String operation,
            String message) {

        Map<String, Object> error =
                new LinkedHashMap<>();

        error.put(
                "operation",
                operation);

        error.put(
                "message",
                message);

        error.put(
                "timestamp",
                System.currentTimeMillis());

        errorLogs.add(0, error);

        // Keep only latest 50 errors
        if (errorLogs.size() > MAX_ERROR_LOGS) {

            errorLogs.remove(
                    errorLogs.size() - 1);
        }
    }

    // ==========================================
    // GET ERROR LOGS
    // ==========================================

    public synchronized List<Map<String, Object>>
    getErrorLogs() {

        return new ArrayList<>(
                errorLogs);
    }

    // ==========================================
    // CLEAR ERROR LOGS
    // ==========================================

    public synchronized void clearErrorLogs() {

        errorLogs.clear();
    }
}