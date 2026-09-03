package com.example.demo.controller;

import com.example.demo.service.ErrorLogService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shadow/errors")
public class ErrorLogController {

    private final ErrorLogService errorLogService;

    public ErrorLogController(
            ErrorLogService errorLogService) {

        this.errorLogService =
                errorLogService;
    }

    // ==========================================
    // GET ERROR LOGS
    // ==========================================

    @GetMapping
    public List<Map<String, Object>> getErrorLogs() {

        return errorLogService.getErrorLogs();
    }

    // ==========================================
    // CLEAR ERROR LOGS
    // ==========================================

    @DeleteMapping
    public String clearErrorLogs() {

        errorLogService.clearErrorLogs();

        return "Error logs cleared successfully.";
    }
}