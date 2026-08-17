package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShadowDatabaseService {

    private PostgreSQLContainer postgresContainer;

    // ==============================
    // CREATE SHADOW DATABASE
    // ==============================
    public String createShadowDatabase() {

        if (postgresContainer != null
                && postgresContainer.isRunning()) {

            return "Shadow PostgreSQL container is already running.";
        }

        postgresContainer = new PostgreSQLContainer(
                "postgres:16-alpine")
                .withDatabaseName("shadowbase")
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
                "Shadow PostgreSQL Started");

        System.out.println(
                "JDBC URL: " + jdbcUrl);

        System.out.println(
                "Username: " + username);

        System.out.println(
                "Password: " + password);

        System.out.println(
                "====================================");

        createTable(
                jdbcUrl,
                username,
                password);

        return "Shadow PostgreSQL container started successfully.\n"
                + "JDBC URL: " + jdbcUrl;
    }

    // ==============================
    // CREATE EMPLOYEES TABLE
    // ==============================
    private void createTable(
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
                    "Employees table created successfully.");

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to create employees table: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==============================
    // CHECK STATUS
    // ==============================
    public String getStatus() {

        if (postgresContainer == null) {

            return "STOPPED - Shadow database has not been created.";
        }

        if (postgresContainer.isRunning()) {

            return "RUNNING - Shadow database is running.";
        }

        return "STOPPED - Shadow database is stopped.";
    }

    // ==============================
    // STOP DATABASE
    // ==============================
    public String stopShadowDatabase() {

        if (postgresContainer == null) {

            return "Shadow database has not been created.";
        }

        if (!postgresContainer.isRunning()) {

            return "Shadow database is already stopped.";
        }

        postgresContainer.stop();

        return "Shadow PostgreSQL container stopped successfully.";
    }

    // ==============================
    // ADD EMPLOYEE
    // ==============================
    public String addEmployee(
            String name,
            double salary) {

        checkDatabaseRunning();

        String sql =
                "INSERT INTO employees (name, salary) VALUES (?, ?)";

        try (
                Connection connection =
                        getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, name);
            statement.setDouble(2, salary);

            statement.executeUpdate();

            return "Employee added successfully.";

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to add employee: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==============================
    // GET EMPLOYEES
    // ==============================
    public List<String> getEmployees() {

        checkDatabaseRunning();

        List<String> employees =
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

                String employee =
                        "ID: "
                        + resultSet.getInt("id")
                        + ", Name: "
                        + resultSet.getString("name")
                        + ", Salary: "
                        + resultSet.getDouble("salary");

                employees.add(employee);
            }

            return employees;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to get employees: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==============================
    // EXECUTE SELECT SQL
    // ==============================
    public Map<String, Object> executeSql(
            String sql) {

        checkDatabaseRunning();

        if (sql == null
                || sql.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "SQL query cannot be empty.");
        }

        String cleanSql = sql.trim();

        if (!cleanSql
                .toLowerCase()
                .startsWith("select")) {

            throw new IllegalArgumentException(
                    "Only SELECT queries are supported currently.");
        }

        try (
                Connection connection =
                        getConnection();

                Statement statement =
                        connection.createStatement();

                ResultSet resultSet =
                        statement.executeQuery(cleanSql)
        ) {

            ResultSetMetaData metaData =
                    resultSet.getMetaData();

            List<Map<String, Object>> rows =
                    new ArrayList<>();

            while (resultSet.next()) {

                Map<String, Object> row =
                        new LinkedHashMap<>();

                for (int i = 1;
                     i <= metaData.getColumnCount();
                     i++) {

                    String columnName =
                            metaData.getColumnLabel(i);

                    Object value =
                            resultSet.getObject(i);

                    row.put(
                            columnName,
                            value);
                }

                rows.add(row);
            }

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "success",
                    true);

            response.put(
                    "rowCount",
                    rows.size());

            response.put(
                    "columns",
                    getColumnNames(metaData));

            response.put(
                    "rows",
                    rows);

            return response;

        } catch (Exception e) {

            throw new RuntimeException(
                    "SQL execution failed: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==============================
    // GET COLUMN NAMES
    // ==============================
    private List<String> getColumnNames(
            ResultSetMetaData metaData)
            throws Exception {

        List<String> columns =
                new ArrayList<>();

        for (int i = 1;
             i <= metaData.getColumnCount();
             i++) {

            columns.add(
                    metaData.getColumnLabel(i));
        }

        return columns;
    }

    // ==============================
    // DATABASE CONNECTION
    // ==============================
    private Connection getConnection()
            throws Exception {

        return DriverManager.getConnection(
                postgresContainer.getJdbcUrl(),
                postgresContainer.getUsername(),
                postgresContainer.getPassword());
    }

    // ==============================
    // CONNECTION INFORMATION
    // ==============================
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

    // ==============================
    // CHECK DATABASE
    // ==============================
    private void checkDatabaseRunning() {

        if (postgresContainer == null) {

            throw new RuntimeException(
                    "Shadow database has not been created.");
        }

        if (!postgresContainer.isRunning()) {

            throw new RuntimeException(
                    "Shadow database is not running.");
        }
    }
}