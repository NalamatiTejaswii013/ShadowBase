package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public KafkaCdcConsumer(
            ShadowDatabaseService shadowDatabaseService) {

        this.shadowDatabaseService =
                shadowDatabaseService;
    }

    // ==========================================
    // KAFKA CDC LISTENER
    // ==========================================

    @KafkaListener(
            topics = "shadowbase.public.employees",
            groupId = "shadowbase-consumer"
    )
    public void consume(String message) {

        System.out.println();
        System.out.println("====================================");
        System.out.println("CDC EVENT RECEIVED");
        System.out.println("====================================");
        System.out.println(message);

        try {

            JsonNode root =
                    objectMapper.readTree(message);

            JsonNode payload =
                    root.path("payload");

            String operation =
                    payload.path("op").asText();

            System.out.println(
                    "CDC Operation: " + operation);

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
                    System.out.println(
                            "Unknown CDC operation: "
                                    + operation);
            }

        } catch (Exception e) {

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
            JsonNode payload) throws Exception {

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

        System.out.println(
                "SNAPSHOT replayed successfully.");
    }

    // ==========================================
    // INSERT
    // ==========================================

    private void handleInsert(
            JsonNode payload) throws Exception {

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

        System.out.println(
                "INSERT replayed successfully.");
    }

    // ==========================================
    // UPDATE
    // ==========================================

    private void handleUpdate(
            JsonNode payload) throws Exception {

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

                statement.setString(1, name);
                statement.setBigDecimal(2, salary);
                statement.setInt(3, id);

                statement.executeUpdate();
            }
        }

        System.out.println(
                "UPDATE replayed successfully.");
    }

    // ==========================================
    // DELETE
    // ==========================================

    private void handleDelete(
            JsonNode payload) throws Exception {

        JsonNode before =
                payload.path("before");

        if (before.isMissingNode()
                || before.isNull()) {

            return;
        }

        int id =
                before.path("id").asInt();

        Connection connection =
                shadowDatabaseService
                        .getShadowConnection();

        try (connection) {

            String sql =
                    "DELETE FROM employees WHERE id = ?";

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, id);

                statement.executeUpdate();
            }
        }

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

                statement.setInt(1, id);
                statement.setString(2, name);
                statement.setBigDecimal(3, salary);

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

        // Normal JSON number
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