package com.example.demo.service;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    // ==========================================
    // AUTOMATICALLY CREATE SHADOW DATABASE
    // ==========================================

    @PostConstruct
    public void initializeShadowDatabase() {

        System.out.println();
        System.out.println("====================================");
        System.out.println("INITIALIZING SHADOW DATABASE");
        System.out.println("====================================");

        try {

            createShadowDatabase();

            System.out.println(
                    "Shadow database initialized successfully.");

        } catch (Exception e) {

            System.err.println(
                    "Failed to initialize shadow database.");

            e.printStackTrace();
        }
    }

    // ==========================================
    // CREATE SHADOW DATABASE
    // ==========================================

    public synchronized String createShadowDatabase() {

        if (postgresContainer != null
                && postgresContainer.isRunning()) {

            return "Shadow PostgreSQL container is already running.";
        }

        try {

            // ==========================================
            // CREATE PERSISTENT DATA DIRECTORY
            // ==========================================

            Path dataDirectory =
                    Paths.get(
                            System.getProperty("user.dir"),
                            "shadowbase-data"
                    ).toAbsolutePath();

            Files.createDirectories(dataDirectory);

            System.out.println(
                    "Shadow database data directory:");

            System.out.println(
                    dataDirectory);

            // ==========================================
            // CREATE POSTGRES CONTAINER
            // ==========================================

            postgresContainer =
                    new PostgreSQLContainer(
                            "postgres:16-alpine")
                            .withDatabaseName("shadowbase")
                            .withUsername("postgres")
                            .withPassword("postgres")
                            .withFileSystemBind(
                                    dataDirectory.toString(),
                                    "/var/lib/postgresql/data",
                                    BindMode.READ_WRITE)
                            .waitingFor(
                                    Wait.forListeningPort());

            // ==========================================
            // START POSTGRESQL
            // ==========================================

            postgresContainer.start();

            System.out.println(
                    "PostgreSQL container started.");

            // ==========================================
            // WAIT FOR POSTGRESQL
            // ==========================================

            waitForPostgres();

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
                    "====================================");

            // ==========================================
            // CREATE EMPLOYEES TABLE
            // ==========================================

            createTable(
                    jdbcUrl,
                    username,
                    password);

            return
                    "Shadow PostgreSQL container started successfully.";

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to create Shadow Database: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==========================================
    // WAIT FOR POSTGRESQL
    // ==========================================

    private void waitForPostgres() {

        int maxAttempts = 30;

        for (int attempt = 1;
             attempt <= maxAttempts;
             attempt++) {

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    postgresContainer.getJdbcUrl(),
                                    postgresContainer.getUsername(),
                                    postgresContainer.getPassword())
            ) {

                System.out.println(
                        "PostgreSQL is ready to accept connections.");

                return;

            } catch (Exception e) {

                System.out.println(
                        "Waiting for PostgreSQL... attempt "
                                + attempt
                                + "/"
                                + maxAttempts);

                try {

                    Thread.sleep(1000);

                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    throw new RuntimeException(
                            "Interrupted while waiting for PostgreSQL.",
                            interruptedException);
                }
            }
        }

        throw new RuntimeException(
                "PostgreSQL did not become ready within 30 seconds.");
    }

    // ==========================================
    // CREATE EMPLOYEES TABLE
    // ==========================================

    private void createTable(
            String jdbcUrl,
            String username,
            String password) {

        String sql = """
                CREATE TABLE IF NOT EXISTS employees (
                    id INTEGER PRIMARY KEY,
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

    // ==========================================
    // CHECK STATUS
    // ==========================================

    public String getStatus() {

        if (postgresContainer == null) {

            return
                    "STOPPED - Shadow database has not been created.";
        }

        if (postgresContainer.isRunning()) {

            return
                    "RUNNING - Shadow database is running.";
        }

        return
                "STOPPED - Shadow database is stopped.";
    }

    // ==========================================
    // STOP DATABASE
    // ==========================================

    public String stopShadowDatabase() {

        if (postgresContainer == null) {

            return
                    "Shadow database has not been created.";
        }

        if (!postgresContainer.isRunning()) {

            return
                    "Shadow database is already stopped.";
        }

        postgresContainer.stop();

        return
                "Shadow PostgreSQL container stopped successfully.";
    }

    // ==========================================
    // ADD EMPLOYEE
    // ==========================================

    public String addEmployee(
            String name,
            double salary) {

        checkDatabaseRunning();

        String sql =
                "INSERT INTO employees (name, salary) VALUES (?, ?)";

        try (
                Connection connection =
                        getShadowConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, name);
            statement.setDouble(2, salary);

            statement.executeUpdate();

            return
                    "Employee added successfully.";

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to add employee: "
                            + e.getMessage(),
                    e);
        }
    }

    // ==========================================
    // GET EMPLOYEES
    // ==========================================

    public List<String> getEmployees() {

        checkDatabaseRunning();

        List<String> employees =
                new ArrayList<>();

        String sql =
                "SELECT id, name, salary "
                        + "FROM employees ORDER BY id";

        try (
                Connection connection =
                        getShadowConnection();

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

    // ==========================================
    // EXECUTE SELECT SQL
    // ==========================================

    public Map<String, Object> executeSql(
            String sql) {

        checkDatabaseRunning();

        if (sql == null
                || sql.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "SQL query cannot be empty.");
        }

        String cleanSql =
                sql.trim();

        if (!cleanSql
                .toLowerCase()
                .startsWith("select")) {

            throw new IllegalArgumentException(
                    "Only SELECT queries are supported currently.");
        }

        try (
                Connection connection =
                        getShadowConnection();

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

    // ==========================================
    // GET COLUMN NAMES
    // ==========================================

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

    // ==========================================
    // GET SHADOW CONNECTION
    // ==========================================

    public Connection getShadowConnection()
            throws Exception {

        checkDatabaseRunning();

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
                    "Shadow database has not been created.");
        }

        if (!postgresContainer.isRunning()) {

            throw new RuntimeException(
                    "Shadow database is not running.");
        }
    }
}