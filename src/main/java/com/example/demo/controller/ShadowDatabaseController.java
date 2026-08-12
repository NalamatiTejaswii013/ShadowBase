package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ShadowDatabaseService;

@RestController
@RequestMapping("/api/shadow")
public class ShadowDatabaseController {

    private final ShadowDatabaseService shadowDatabaseService;

    public ShadowDatabaseController(ShadowDatabaseService shadowDatabaseService) {
        this.shadowDatabaseService = shadowDatabaseService;
    }

    @PostMapping("/create")
    public String createShadowDatabase() {

        return shadowDatabaseService.createShadowDatabase();
    }
}