package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Base64;

@Service
public class KafkaCdcConsumer {

    private final ShadowDatabaseService shadowDatabaseService;

    private final MetricsService metricsService;

    private final ErrorLogService errorLogService;

    private final TrafficReplayService trafficReplayService;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public KafkaCdcConsumer(
            ShadowDatabaseService shadowDatabaseService,
            MetricsService metricsService,
            ErrorLogService errorLogService,
            TrafficReplayService trafficReplayService) {

        this.shadowDatabaseService =
                shadowDatabaseService;

        this.metricsService =
                metricsService;

        this.errorLogService =
                errorLogService;

        this.trafficReplayService =
                trafficReplayService;
    }

    // ==========================================
    // KAFKA CDC LISTENER
    // ==========================================

    @KafkaListener(
            topics = "shadowbase.public.employees",
            groupId = "shadowbase-consumer"
    )
    public void consume(
            ConsumerRecord<String, String> record) {

        System.out.println();
        System.out.println("====================================");
        System.out.println("CDC EVENT RECEIVED");
        System.out.println("====================================");

        String message = record.value();

        // ==========================================
        // HANDLE KAFKA TOMBSTONE
        // ==========================================

        if (message == null
                || message.trim().isEmpty()) {

            System.out.println(
                    "Kafka tombstone message received. Ignoring.");

            System.out.println(
                    "Kafka offset: "
                            + record.offset());

            return;
        }

        System.out.println(message);

        // ==========================================
        // PROCESS CDC EVENT
        // ==========================================

        try {

            JsonNode root =
                    objectMapper.readTree(message);

            JsonNode payload =
                    root.path("payload");

            // ==========================================
            // CHECK PAYLOAD
            // ==========================================

            if (payload.isMissingNode()
                    || payload.isNull()) {

                System.out.println(
                        "CDC payload is empty. Ignoring message.");

                return;
            }

            // ==========================================
            // GET OPERATION
            // ==========================================

            String operation =
                    payload.path("op").asText();

            System.out.println(
                    "CDC Operation: "
                            + operation);

            // ==========================================
            // HANDLE OPERATION
            // ==========================================

            switch (operation) {

                case "r":

                    handleSnapshot(payload);

                    break;

                case "c":

                    handleInsert(payload);

                    break;

                case "u":

                    handleUpdate(payload);

                    break;

                case "d":

                    handleDelete(payload);

                    break;

                default:

                    throw new IllegalArgumentException(
                            "Unknown CDC operation: "
                                    + operation);
            }

        } catch (Exception e) {

            // ==========================================
            // RECORD CDC ERROR
            // ==========================================

            metricsService.recordError();

            errorLogService.recordError(
                    "CDC_PROCESSING",
                    e.getMessage());

            System.err.println(
                    "CDC processing failed: "
                            + e.getMessage());

            e.printStackTrace();
        }
    }

    // ==========================================
    // SNAPSHOT
    // ==========================================

    private void handleSnapshot(
            JsonNode payload)
            throws Exception {

        JsonNode after =
                payload.path("after");

        if (after.isMissingNode()
                || after.isNull()) {

            return;
        }

        int id =
                after.path("id").asInt();

        String name =
                after.path("name").asText();

        BigDecimal salary =
                decodeSalary(
                        after.path("salary"));

        System.out.println(
                "SNAPSHOT -> ID: "
                        + id
                        + ", Name: "
                        + name
                        + ", Salary: "
                        + salary);

        upsertEmployee(
                id,
                name,
                salary);

        // ==========================================
        // RECORD METRICS
        // ==========================================

        metricsService.recordReplay();

        metricsService.recordEvent(
                "SNAPSHOT",
                id,
                name,
                salary.toString());

        // ==========================================
        // RECORD TRAFFIC REPLAY
        // ==========================================

        trafficReplayService.recordReplay(
                "SNAPSHOT",
                "employees",
                id);

        System.out.println(
                "SNAPSHOT replayed successfully.");
    }

    // ==========================================
    // INSERT
    // ==========================================

    private void handleInsert(
            JsonNode payload)
            throws Exception {

        JsonNode after =
                payload.path("after");

        if (after.isMissingNode()
                || after.isNull()) {

            return;
        }

        int id =
                after.path("id").asInt();

        String name =
                after.path("name").asText();

        BigDecimal salary =
                decodeSalary(
                        after.path("salary"));

        System.out.println(
                "INSERT -> ID: "
                        + id
                        + ", Name: "
                        + name
                        + ", Salary: "
                        + salary);

        upsertEmployee(
                id,
                name,
                salary);

        // ==========================================
        // RECORD METRICS
        // ==========================================

        metricsService.recordReplay();

        metricsService.recordEvent(
                "INSERT",
                id,
                name,
                salary.toString());

        // ==========================================
        // RECORD TRAFFIC REPLAY
        // ==========================================

        trafficReplayService.recordReplay(
                "INSERT",
                "employees",
                id);

        System.out.println(
                "INSERT replayed successfully.");
    }

    // ==========================================
    // UPDATE
    // ==========================================

    private void handleUpdate(
            JsonNode payload)
            throws Exception {

        JsonNode after =
                payload.path("after");

        if (after.isMissingNode()
                || after.isNull()) {

            return;
        }

        int id =
                after.path("id").asInt();

        String name =
                after.path("name").asText();

        BigDecimal salary =
                decodeSalary(
                        after.path("salary"));

        Connection connection =
                shadowDatabaseService
                        .getShadowConnection();

        try (connection) {

            String sql =
                    """
                    UPDATE employees
                    SET name = ?,
                        salary = ?
                    WHERE id = ?
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(
                        1,
                        name);

                statement.setBigDecimal(
                        2,
                        salary);

                statement.setInt(
                        3,
                        id);

                statement.executeUpdate();
            }
        }

        // ==========================================
        // RECORD METRICS
        // ==========================================

        metricsService.recordReplay();

        metricsService.recordEvent(
                "UPDATE",
                id,
                name,
                salary.toString());

        // ==========================================
        // RECORD TRAFFIC REPLAY
        // ==========================================

        trafficReplayService.recordReplay(
                "UPDATE",
                "employees",
                id);

        System.out.println(
                "UPDATE replayed successfully.");
    }

    // ==========================================
    // DELETE
    // ==========================================

    private void handleDelete(
            JsonNode payload)
            throws Exception {

        JsonNode before =
                payload.path("before");

        if (before.isMissingNode()
                || before.isNull()) {

            return;
        }

        int id =
                before.path("id").asInt();

        String name =
                before.path("name").asText();

        BigDecimal salary =
                decodeSalary(
                        before.path("salary"));

        Connection connection =
                shadowDatabaseService
                        .getShadowConnection();

        try (connection) {

            String sql =
                    "DELETE FROM employees WHERE id = ?";

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(
                        1,
                        id);

                statement.executeUpdate();
            }
        }

        // ==========================================
        // RECORD METRICS
        // ==========================================

        metricsService.recordReplay();

        metricsService.recordEvent(
                "DELETE",
                id,
                name,
                salary.toString());

        // ==========================================
        // RECORD TRAFFIC REPLAY
        // ==========================================

        trafficReplayService.recordReplay(
                "DELETE",
                "employees",
                id);

        System.out.println(
                "DELETE replayed successfully.");
    }

    // ==========================================
    // INSERT OR UPDATE
    // ==========================================

    private void upsertEmployee(
            int id,
            String name,
            BigDecimal salary)
            throws Exception {

        Connection connection =
                shadowDatabaseService
                        .getShadowConnection();

        try (connection) {

            String sql =
                    """
                    INSERT INTO employees
                    (id, name, salary)
                    VALUES (?, ?, ?)
                    ON CONFLICT (id)
                    DO UPDATE SET
                        name = EXCLUDED.name,
                        salary = EXCLUDED.salary
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(
                        1,
                        id);

                statement.setString(
                        2,
                        name);

                statement.setBigDecimal(
                        3,
                        salary);

                statement.executeUpdate();
            }
        }
    }

    // ==========================================
    // DECODE POSTGRESQL NUMERIC
    // ==========================================

    private BigDecimal decodeSalary(
            JsonNode salaryNode) {

        if (salaryNode == null
                || salaryNode.isNull()
                || salaryNode.isMissingNode()) {

            return BigDecimal.ZERO;
        }

        if (salaryNode.isNumber()) {

            return salaryNode.decimalValue();
        }

        String encoded =
                salaryNode.asText();

        try {

            byte[] bytes =
                    Base64.getDecoder()
                            .decode(encoded);

            BigInteger unscaled =
                    new BigInteger(bytes);

            return new BigDecimal(
                    unscaled,
                    2);

        } catch (Exception e) {

            System.err.println(
                    "Unable to decode salary: "
                            + encoded);

            return BigDecimal.ZERO;
        }
    }
}