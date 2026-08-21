package com.example.demo.controller;

import com.example.demo.service.MigrationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    private final MigrationService migrationService;

    public MigrationController(
            MigrationService migrationService) {

        this.migrationService =
                migrationService;
    }
    // ==========================================
    // START MIGRATION
    // ==========================================
    @PostMapping("/start")
    public Map<String, Object> startMigration() {
    	
        return migrationService.startMigration();
    }

    // ==========================================
    // GET MIGRATION STATUS
    // ==========================================
    @GetMapping("/status")
    public Map<String, Object> getMigrationStatus() {

        return migrationService
                .getMigrationStatus();
    }

    // ==========================================
    // COMPARE DATABASES
    // ==========================================
    @GetMapping("/compare")
    public Map<String, Object> compareDatabases() {

        return migrationService
                .compareDatabases();
    }
}



