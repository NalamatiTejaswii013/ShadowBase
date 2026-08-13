package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShadowDatabaseService {

    private PostgreSQLContainer postgresContainer;

    // Create Shadow Database
    public String createShadowDatabase() {

        if (postgresContainer != null && postgresContainer.isRunning()) {
            return "Shadow PostgreSQL container is already running.";
        }

        postgresContainer = new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("shadowbase")
                .withUsername("postgres")
                .withPassword("postgres");

        postgresContainer.start();

        String jdbcUrl = postgresContainer.getJdbcUrl();
        String username = postgresContainer.getUsername();
        String password = postgresContainer.getPassword();

        System.out.println("====================================");
        System.out.println("Shadow PostgreSQL Started");
        System.out.println("JDBC URL: " + jdbcUrl);
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        System.out.println("====================================");

        createTable(jdbcUrl, username, password);

        return "Shadow PostgreSQL container started successfully.\n"
                + "JDBC URL: " + jdbcUrl;
    }

    // Create employees table
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
                                jdbcUrl, username, password);

                Statement statement =
                        connection.createStatement()
        ) {

            statement.executeUpdate(sql);

            System.out.println(
                    "Employees table created successfully.");

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to create employees table: "
                            + e.getMessage(), e);
        }
    }

    // Check database status
    public String getStatus() {

        if (postgresContainer == null) {
            return "STOPPED - Shadow database has not been created.";
        }

        if (postgresContainer.isRunning()) {
            return "RUNNING - Shadow database is running.";
        }

        return "STOPPED - Shadow database is stopped.";
    }

    // Stop database
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

    // Insert employee
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
                            + e.getMessage(), e);
        }
    }

    // Get all employees
    public List<String> getEmployees() {

        checkDatabaseRunning();

        List<String> employees = new ArrayList<>();

        String sql =
                "SELECT id, name, salary FROM employees ORDER BY id";

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
                        "ID: " + resultSet.getInt("id")
                        + ", Name: " + resultSet.getString("name")
                        + ", Salary: " + resultSet.getDouble("salary");

                employees.add(employee);
            }

            return employees;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to get employees: "
                            + e.getMessage(), e);
        }
    }

    // Create database connection
    private Connection getConnection()
            throws Exception {

        return DriverManager.getConnection(
                postgresContainer.getJdbcUrl(),
                postgresContainer.getUsername(),
                postgresContainer.getPassword());
    }

    // Check database before executing SQL
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