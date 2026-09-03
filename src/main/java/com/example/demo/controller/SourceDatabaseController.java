package com.example.demo.controller;

import com.example.demo.service.SourceDatabaseService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/source")
public class SourceDatabaseController {
    private final SourceDatabaseService sourceDatabaseService;
    public SourceDatabaseController(
            SourceDatabaseService sourceDatabaseService) {
        this.sourceDatabaseService =
                sourceDatabaseService;
    }
    // ==========================================
    // CREATE SOURCE DATABASE
    // ==========================================
    @PostMapping("/create")
    public String createSourceDatabase() {
        return sourceDatabaseService
                .createSourceDatabase();
    }

    // ==========================================
    // CHECK SOURCE STATUS
    // ==========================================
    @GetMapping("/status")
    public String getStatus() {

        return sourceDatabaseService
                .getStatus();
    }
    // ==========================================
    // GET SOURCE EMPLOYEES
    // ==========================================
    @GetMapping("/employees")
    public List<Map<String, Object>> getEmployees() {
        return sourceDatabaseService
                .getEmployees();
    }

    // ==========================================
    // STOP SOURCE DATABASE
    // ==========================================
    @PostMapping("/stop")
    public String stopSourceDatabase() {

        return sourceDatabaseService
                .stopSourceDatabase();
    }
}


