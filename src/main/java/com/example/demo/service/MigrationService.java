package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MigrationService {

    private final SourceDatabaseService sourceDatabaseService;
    private final ShadowDatabaseService shadowDatabaseService;

    private String migrationStatus = "NOT_STARTED";

    private int totalRows = 0;
    private int migratedRows = 0;

    public MigrationService(
            SourceDatabaseService sourceDatabaseService,
            ShadowDatabaseService shadowDatabaseService) {

        this.sourceDatabaseService =
                sourceDatabaseService;

        this.shadowDatabaseService =
                shadowDatabaseService;
    }

    public synchronized Map<String, Object> startMigration() {

        migrationStatus = "STARTING";
        migratedRows = 0;
        totalRows = 0;

        try {

            Map<String, String> sourceInfo =
                    sourceDatabaseService
                            .getDatabaseConnectionInfo();

            Map<String, String> shadowInfo =
                    shadowDatabaseService
                            .getDatabaseConnectionInfo();

            migrationStatus = "READING_SOURCE";

            String sourceSql =
                    "SELECT id, name, salary "
                            + "FROM employees ORDER BY id";

            try (
                    Connection sourceConnection =
                            DriverManager.getConnection(
                                    sourceInfo.get("jdbcUrl"),
                                    sourceInfo.get("username"),
                                    sourceInfo.get("password"));

                    Statement sourceStatement =
                            sourceConnection.createStatement();

                    ResultSet resultSet =
                            sourceStatement.executeQuery(
                                    sourceSql)
            ) {

                String shadowSql = """
                        INSERT INTO employees
                        (id, name, salary)
                        VALUES (?, ?, ?)
                        ON CONFLICT (id)
                        DO UPDATE SET
                            name = EXCLUDED.name,
                            salary = EXCLUDED.salary
                        """;

                try (
                        Connection shadowConnection =
                                DriverManager.getConnection(
                                        shadowInfo.get("jdbcUrl"),
                                        shadowInfo.get("username"),
                                        shadowInfo.get("password"));

                        PreparedStatement shadowStatement =
                                shadowConnection.prepareStatement(
                                        shadowSql)
                ) {

                    migrationStatus =
                            "MIGRATING_DATA";

                    // Clear existing Shadow data
                    // so Shadow becomes an exact copy
                    // of Source after migration.
                    try (
                            Statement deleteStatement =
                                    shadowConnection.createStatement()
                    ) {

                        deleteStatement.executeUpdate(
                                "DELETE FROM employees");
                    }

                    while (resultSet.next()) {

                        totalRows++;

                        int id =
                                resultSet.getInt("id");

                        String name =
                                resultSet.getString("name");

                        java.math.BigDecimal salary =
                                resultSet.getBigDecimal(
                                        "salary");

                        shadowStatement.setInt(
                                1,
                                id);

                        shadowStatement.setString(
                                2,
                                name);

                        shadowStatement.setBigDecimal(
                                3,
                                salary);

                        shadowStatement.executeUpdate();

                        migratedRows++;
                    }
                }
            }

            migrationStatus = "COMPLETED";

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "success",
                    true);

            response.put(
                    "status",
                    migrationStatus);

            response.put(
                    "totalRows",
                    totalRows);

            response.put(
                    "migratedRows",
                    migratedRows);

            response.put(
                    "message",
                    "Migration completed successfully.");

            return response;

        } catch (Exception e) {

            migrationStatus = "FAILED";

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "success",
                    false);

            response.put(
                    "status",
                    migrationStatus);

            response.put(
                    "totalRows",
                    totalRows);

            response.put(
                    "migratedRows",
                    migratedRows);

            response.put(
                    "message",
                    "Migration failed: "
                            + e.getMessage());

            return response;
        }
    }

    public Map<String, Object> getMigrationStatus() {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "status",
                migrationStatus);

        response.put(
                "totalRows",
                totalRows);

        response.put(
                "migratedRows",
                migratedRows);

        int progress = 0;

        if (totalRows > 0) {

            progress =
                    (migratedRows * 100)
                            / totalRows;
        }

        response.put(
                "progress",
                progress);

        return response;
    }

    public Map<String, Object> compareDatabases() {

        Map<String, String> sourceInfo =
                sourceDatabaseService
                        .getDatabaseConnectionInfo();

        Map<String, String> shadowInfo =
                shadowDatabaseService
                        .getDatabaseConnectionInfo();

        Map<Integer, Map<String, Object>> sourceEmployees =
                getEmployees(sourceInfo);

        Map<Integer, Map<String, Object>> shadowEmployees =
                getEmployees(shadowInfo);

        int sourceCount =
                sourceEmployees.size();

        int shadowCount =
                shadowEmployees.size();

        int missingRows = 0;
        int extraRows = 0;
        int dataMismatches = 0;

        // Check for rows that exist in Source
        // but are missing in Shadow.
        for (Integer id : sourceEmployees.keySet()) {

            if (!shadowEmployees.containsKey(id)) {

                missingRows++;
            }
        }

        // Check for rows that exist in Shadow
        // but are not present in Source.
        for (Integer id : shadowEmployees.keySet()) {

            if (!sourceEmployees.containsKey(id)) {

                extraRows++;
            }
        }

        // Check data differences for matching IDs.
        for (Integer id : sourceEmployees.keySet()) {

            if (!shadowEmployees.containsKey(id)) {
                continue;
            }

            Map<String, Object> sourceEmployee =
                    sourceEmployees.get(id);

            Map<String, Object> shadowEmployee =
                    shadowEmployees.get(id);

            String sourceName =
                    (String) sourceEmployee.get("name");

            String shadowName =
                    (String) shadowEmployee.get("name");

            java.math.BigDecimal sourceSalary =
                    (java.math.BigDecimal)
                            sourceEmployee.get("salary");

            java.math.BigDecimal shadowSalary =
                    (java.math.BigDecimal)
                            shadowEmployee.get("salary");

            boolean nameMismatch =
                    !java.util.Objects.equals(
                            sourceName,
                            shadowName);

            boolean salaryMismatch =
                    !java.util.Objects.equals(
                            sourceSalary,
                            shadowSalary);

            if (nameMismatch || salaryMismatch) {

                dataMismatches++;
            }
        }

        boolean matching =
                missingRows == 0
                        && extraRows == 0
                        && dataMismatches == 0;

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "sourceRowCount",
                sourceCount);

        response.put(
                "shadowRowCount",
                shadowCount);

        response.put(
                "matching",
                matching);

        response.put(
                "status",
                matching
                        ? "SYNCED"
                        : "MISMATCH");

        response.put(
                "missingRows",
                missingRows);

        response.put(
                "extraRows",
                extraRows);

        response.put(
                "dataMismatches",
                dataMismatches);

        return response;
    }

    private Map<Integer, Map<String, Object>> getEmployees(
            Map<String, String> databaseInfo) {

        Map<Integer, Map<String, Object>> employees =
                new LinkedHashMap<>();

        String sql =
                "SELECT id, name, salary "
                        + "FROM employees ORDER BY id";

        try (
                Connection connection =
                        DriverManager.getConnection(
                                databaseInfo.get("jdbcUrl"),
                                databaseInfo.get("username"),
                                databaseInfo.get("password"));

                Statement statement =
                        connection.createStatement();

                ResultSet resultSet =
                        statement.executeQuery(sql)
        ) {

            while (resultSet.next()) {

                int id =
                        resultSet.getInt("id");

                Map<String, Object> employee =
                        new LinkedHashMap<>();

                employee.put(
                        "name",
                        resultSet.getString("name"));

                employee.put(
                        "salary",
                        resultSet.getBigDecimal("salary"));

                employees.put(
                        id,
                        employee);
            }

            return employees;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read employee data: "
                            + e.getMessage(),
                    e);
        }
    }
}