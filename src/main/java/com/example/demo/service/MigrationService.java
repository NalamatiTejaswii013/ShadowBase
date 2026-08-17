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

    // ==========================================
    // START MIGRATION
    // ==========================================
    public synchronized Map<String, Object> startMigration() {

        migrationStatus = "STARTING";
        migratedRows = 0;
        totalRows = 0;

        try {

            // Check both databases
            Map<String, String> sourceInfo =
                    sourceDatabaseService
                            .getDatabaseConnectionInfo();

            Map<String, String> shadowInfo =
                    shadowDatabaseService
                            .getDatabaseConnectionInfo();

            migrationStatus = "READING_SOURCE";

            // ----------------------------------
            // READ SOURCE EMPLOYEES
            // ----------------------------------

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

                // ----------------------------------
                // PREPARE SHADOW INSERT
                // ----------------------------------

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

    // ==========================================
    // MIGRATION STATUS
    // ==========================================
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

    // ==========================================
    // COMPARE SOURCE AND SHADOW
    // ==========================================
    public Map<String, Object> compareDatabases() {

        Map<String, String> sourceInfo =
                sourceDatabaseService
                        .getDatabaseConnectionInfo();

        Map<String, String> shadowInfo =
                shadowDatabaseService
                        .getDatabaseConnectionInfo();

        int sourceCount =
                getEmployeeCount(sourceInfo);

        int shadowCount =
                getEmployeeCount(shadowInfo);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "sourceRowCount",
                sourceCount);

        response.put(
                "shadowRowCount",
                shadowCount);

        response.put(
                "match",
                sourceCount == shadowCount);

        if (sourceCount == shadowCount) {

            response.put(
                    "message",
                    "Source and Shadow row counts match.");

        } else {

            response.put(
                    "message",
                    "Source and Shadow row counts do not match.");
        }

        return response;
    }

    // ==========================================
    // GET EMPLOYEE COUNT
    // ==========================================
    private int getEmployeeCount(
            Map<String, String> databaseInfo) {

        String sql =
                "SELECT COUNT(*) FROM employees";

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

            if (resultSet.next()) {

                return resultSet.getInt(1);
            }

            return 0;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to compare databases: "
                            + e.getMessage(),
                    e);
        }
    }
}
