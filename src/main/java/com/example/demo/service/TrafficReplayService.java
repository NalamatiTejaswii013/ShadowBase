package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrafficReplayService {

    private final List<Map<String, Object>> replayHistory =
            new ArrayList<>();

    public synchronized void recordReplay(
            String operation,
            String table,
            Object id) {

        Map<String, Object> replay =
                new LinkedHashMap<>();

        replay.put("operation", operation);
        replay.put("table", table);
        replay.put("id", id);
        replay.put("timestamp", System.currentTimeMillis());

        replayHistory.add(0, replay);

        if (replayHistory.size() > 50) {
            replayHistory.remove(replayHistory.size() - 1);
        }
    }

    public synchronized List<Map<String, Object>> getReplayHistory() {
        return new ArrayList<>(replayHistory);
    }

    public synchronized void clearReplayHistory() {
        replayHistory.clear();
    }
}