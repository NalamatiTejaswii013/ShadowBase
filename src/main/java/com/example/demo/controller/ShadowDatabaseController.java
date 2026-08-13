package com.example.demo.controller;

import com.example.demo.service.ShadowDatabaseService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shadow")
public class ShadowDatabaseController {

    private final ShadowDatabaseService shadowDatabaseService;

    public ShadowDatabaseController(
            ShadowDatabaseService shadowDatabaseService) {

        this.shadowDatabaseService = shadowDatabaseService;
    }

    // Create Shadow Database
    @PostMapping("/create")
    public String createShadowDatabase() {

        return shadowDatabaseService.createShadowDatabase();
    }

    // Check status
    @GetMapping("/status")
    public String getStatus() {

        return shadowDatabaseService.getStatus();
    }

    // Stop Shadow Database
    @PostMapping("/stop")
    public String stopShadowDatabase() {

        return shadowDatabaseService.stopShadowDatabase();
    }

    // Add Employee
    @PostMapping("/employees")
    public String addEmployee(
            @RequestBody Map<String, Object> employee) {

        String name =
                employee.get("name").toString();

        double salary =
                Double.parseDouble(
                        employee.get("salary").toString());

        return shadowDatabaseService.addEmployee(
                name, salary);
    }

    // Get Employees
    @GetMapping("/employees")
    public List<String> getEmployees() {

        return shadowDatabaseService.getEmployees();
    }
}