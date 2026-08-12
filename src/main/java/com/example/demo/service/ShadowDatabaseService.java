package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.testcontainers.postgresql.PostgreSQLContainer;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Service
public class ShadowDatabaseService {

    private PostgreSQLContainer postgresContainer;

    public String createShadowDatabase() {

        // Create PostgreSQL container
        postgresContainer = new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("shadowbase")
                .withUsername("postgres")
                .withPassword("postgres");

        // Start Docker container
        postgresContainer.start();

        // Get connection details
        String jdbcUrl = postgresContainer.getJdbcUrl();
        String username = postgresContainer.getUsername();
        String password = postgresContainer.getPassword();

        System.out.println("====================================");
        System.out.println("Shadow PostgreSQL Started");
        System.out.println("JDBC URL: " + jdbcUrl);
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        System.out.println("====================================");

        // Create a table inside PostgreSQL
        createTable(jdbcUrl, username, password);

        return "Shadow PostgreSQL container started successfully.\n"
                + "JDBC URL: " + jdbcUrl;
    }

    private void createTable(String jdbcUrl, String username, String password) {

        String sql = """
                CREATE TABLE IF NOT EXISTS employees (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100),
                    salary NUMERIC(10,2)
                )
                """;

        try (Connection connection =
                     DriverManager.getConnection(jdbcUrl, username, password);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);

            System.out.println("Employees table created successfully.");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create employees table: " + e.getMessage(), e);
        }
    }
}