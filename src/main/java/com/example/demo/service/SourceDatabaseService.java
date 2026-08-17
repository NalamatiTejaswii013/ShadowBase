package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.testcontainers.postgresql.PostgreSQLContainer;

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

    private PostgreSQLContainer postgresContainer;

    // ==========================================
    // CREATE SOURCE DATABASE
    // ==========================================
    public String createSourceDatabase() {

        if (postgresContainer != null
                && postgresContainer.isRunning()) {

            return "Source PostgreSQL container is already running.";
        }

        postgresContainer = new PostgreSQLContainer(
                "postgres:16-alpine")
                .withDatabaseName("sourcedb")
                .withUsername("postgres")
                .withPassword("postgres");

        postgresContainer.start();

        String jdbcUrl =
                postgresContainer.getJdbcUrl();

        String username =
                postgresContainer.getUsername();

        String password =
                postgresContainer.getPassword();

        System.out.println(
                "====================================");

        System.out.println(
                "Source PostgreSQL Started");

        System.out.println(
                "JDBC URL: " + jdbcUrl);

        System.out.println(
                "Username: " + username);

        System.out.println(
                "Password: " + password);

        System.out.println(
                "====================================");

        createEmployeesTable(
                jdbcUrl,
                username,
                password);

        insertSampleEmployees(
                jdbcUrl,
                username,
                password);

        return "Source PostgreSQL container started successfully.\n"
                + "JDBC URL: " + jdbcUrl;
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
                    "Source employees table created.");

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to create source employees table: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==========================================
    // INSERT SAMPLE DATA
    // ==========================================
    private void insertSampleEmployees(
            String jdbcUrl,
            String username,
            String password) {

        String sql = """
                INSERT INTO employees
                (name, salary)
                VALUES (?, ?)
                """;

        try (
                Connection connection =
                        DriverManager.getConnection(
                                jdbcUrl,
                                username,
                                password);

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            insertEmployee(
                    statement,
                    "Tejaswi",
                    30000);

            insertEmployee(
                    statement,
                    "Priyanka",
                    40000);

            insertEmployee(
                    statement,
                    "Rahul",
                    45000);

            System.out.println(
                    "Sample source employees inserted.");

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to insert source employees: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==========================================
    // INSERT ONE EMPLOYEE
    // ==========================================
    private void insertEmployee(
            PreparedStatement statement,
            String name,
            double salary)
            throws Exception {

        statement.setString(
                1,
                name);

        statement.setDouble(
                2,
                salary);

        statement.executeUpdate();
    }

    // ==========================================
    // CHECK STATUS
    // ==========================================
    public String getStatus() {

        if (postgresContainer == null) {

            return "STOPPED - Source database has not been created.";
        }

        if (postgresContainer.isRunning()) {

            return "RUNNING - Source database is running.";
        }

        return "STOPPED - Source database is stopped.";
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

        if (postgresContainer == null) {

            return "Source database has not been created.";
        }

        if (!postgresContainer.isRunning()) {

            return "Source database is already stopped.";
        }

        postgresContainer.stop();

        return "Source PostgreSQL container stopped successfully.";
    }

    // ==========================================
    // DATABASE CONNECTION
    // ==========================================
    private Connection getConnection()
            throws Exception {

        return DriverManager.getConnection(
                postgresContainer.getJdbcUrl(),
                postgresContainer.getUsername(),
                postgresContainer.getPassword());
    }

    // ==========================================
    // CONNECTION INFORMATION
    // ==========================================
    public Map<String, String>
    getDatabaseConnectionInfo() {

        checkDatabaseRunning();

        Map<String, String> info =
                new LinkedHashMap<>();

        info.put(
                "jdbcUrl",
                postgresContainer.getJdbcUrl());

        info.put(
                "username",
                postgresContainer.getUsername());

        info.put(
                "password",
                postgresContainer.getPassword());

        return info;
    }

    // ==========================================
    // CHECK DATABASE
    // ==========================================
    private void checkDatabaseRunning() {

        if (postgresContainer == null) {

            throw new RuntimeException(
                    "Source database has not been created.");
        }

        if (!postgresContainer.isRunning()) {

            throw new RuntimeException(
                    "Source database is not running.");
        }
    }
}