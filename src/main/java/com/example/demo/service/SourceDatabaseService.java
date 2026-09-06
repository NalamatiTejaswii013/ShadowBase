package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SourceDatabaseService {

    // ==========================================
    // EXISTING DEBEZIUM SOURCE DATABASE
    // ==========================================

    private final String sourceJdbcUrl =
            "jdbc:postgresql://localhost:5433/productiondb";

    private final String sourceUsername =
            "postgres";

    private final String sourcePassword =
            "postgres";

    // ==========================================
    // CREATE / CHECK SOURCE DATABASE
    // ==========================================

    public String createSourceDatabase() {

        try (Connection connection =
                     DriverManager.getConnection(
                             sourceJdbcUrl,
                             sourceUsername,
                             sourcePassword)) {

            createEmployeesTable(
                    sourceJdbcUrl,
                    sourceUsername,
                    sourcePassword);

            return "Source PostgreSQL database is running successfully.\n"
                    + "JDBC URL: " + sourceJdbcUrl;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to connect to source PostgreSQL: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==========================================
    // CREATE EMPLOYEES TABLE
    // ==========================================

    private void createEmployeesTable(
            String jdbcUrl,
            String username,
            String password) {

        String sql = """
                CREATE TABLE IF NOT EXISTS employees (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100),
                    salary NUMERIC(10,2)
                )
                """;

        try (
                Connection connection =
                        DriverManager.getConnection(
                                jdbcUrl,
                                username,
                                password);

                Statement statement =
                        connection.createStatement()
        ) {

            statement.executeUpdate(sql);

            System.out.println(
                    "Source employees table verified.");

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to create source employees table: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==========================================
    // GET SOURCE DATABASE STATUS
    // ==========================================

    public String getStatus() {

        try (Connection connection =
                     DriverManager.getConnection(
                             sourceJdbcUrl,
                             sourceUsername,
                             sourcePassword)) {

            return "RUNNING - Source database is running.";

        } catch (Exception e) {

            return "STOPPED - Source database is not reachable.";
        }
    }

    // ==========================================
    // GET SOURCE EMPLOYEES
    // ==========================================

    public List<Map<String, Object>> getEmployees() {

        checkDatabaseRunning();

        List<Map<String, Object>> employees =
                new ArrayList<>();

        String sql =
                "SELECT id, name, salary "
                        + "FROM employees ORDER BY id";

        try (
                Connection connection =
                        getConnection();

                Statement statement =
                        connection.createStatement();

                ResultSet resultSet =
                        statement.executeQuery(sql)
        ) {

            while (resultSet.next()) {

                Map<String, Object> employee =
                        new LinkedHashMap<>();

                employee.put(
                        "id",
                        resultSet.getInt("id"));

                employee.put(
                        "name",
                        resultSet.getString("name"));

                employee.put(
                        "salary",
                        resultSet.getBigDecimal("salary"));

                employees.add(employee);
            }

            return employees;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to get source employees: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==========================================
    // STOP SOURCE DATABASE
    // ==========================================

    public String stopSourceDatabase() {

        return "Source database is managed by Docker "
                + "and cannot be stopped from the application.";
    }

    // ==========================================
    // DATABASE CONNECTION
    // ==========================================

    private Connection getConnection()
            throws Exception {

        return DriverManager.getConnection(
                sourceJdbcUrl,
                sourceUsername,
                sourcePassword);
    }

    // ==========================================
    // CONNECTION INFORMATION
    // ==========================================

    public Map<String, String> getDatabaseConnectionInfo() {

        checkDatabaseRunning();

        Map<String, String> info =
                new LinkedHashMap<>();

        info.put(
                "jdbcUrl",
                sourceJdbcUrl);

        info.put(
                "username",
                sourceUsername);

        info.put(
                "password",
                sourcePassword);

        return info;
    }

    // ==========================================
    // CHECK DATABASE
    // ==========================================

    private void checkDatabaseRunning() {

        try (Connection connection =
                     DriverManager.getConnection(
                             sourceJdbcUrl,
                             sourceUsername,
                             sourcePassword)) {

            // Source database is reachable.

        } catch (Exception e) {

            throw new RuntimeException(
                    "Source database is not running "
                            + "or not reachable: "
                            + e.getMessage(),
                    e);
        }
    }
}