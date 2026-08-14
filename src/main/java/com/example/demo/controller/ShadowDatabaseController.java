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

    // ==============================
    // CREATE SHADOW DATABASE
    // ==============================
    @PostMapping("/create")
    public String createShadowDatabase() {

        return shadowDatabaseService.createShadowDatabase();
    }

    // ==============================
    // CHECK STATUS
    // ==============================
    @GetMapping("/status")
    public String getStatus() {

        return shadowDatabaseService.getStatus();
    }

    // ==============================
    // STOP DATABASE
    // ==============================
    @PostMapping("/stop")
    public String stopShadowDatabase() {

        return shadowDatabaseService.stopShadowDatabase();
    }

    // ==============================
    // ADD EMPLOYEE
    // ==============================
    @PostMapping("/employees")
    public String addEmployee(
            @RequestBody Map<String, Object> employee) {

        String name =
                employee.get("name").toString();

        double salary =
                Double.parseDouble(
                        employee.get("salary").toString());

        return shadowDatabaseService.addEmployee(
                name,
                salary);
    }

    // ==============================
    // GET EMPLOYEES
    // ==============================
    @GetMapping("/employees")
    public List<String> getEmployees() {

        return shadowDatabaseService.getEmployees();
    }

    // ==============================
    // EXECUTE SQL
    // ==============================
    @PostMapping("/sql")
    public Map<String, Object> executeSql(
            @RequestBody Map<String, String> request) {

        String sql = request.get("sql");

        return shadowDatabaseService.executeSql(sql);
    }
}