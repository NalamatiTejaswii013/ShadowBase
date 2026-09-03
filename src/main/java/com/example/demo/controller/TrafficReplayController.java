package com.example.demo.controller;

import com.example.demo.service.TrafficReplayService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shadow/replay")
public class TrafficReplayController {

    private final TrafficReplayService trafficReplayService;

    public TrafficReplayController(
            TrafficReplayService trafficReplayService) {

        this.trafficReplayService =
                trafficReplayService;
    }

    @GetMapping
    public List<Map<String, Object>> getReplayHistory() {

        return trafficReplayService
                .getReplayHistory();
    }

    @DeleteMapping
    public String clearReplayHistory() {

        trafficReplayService
                .clearReplayHistory();

        return "Traffic replay history cleared successfully.";
    }
}