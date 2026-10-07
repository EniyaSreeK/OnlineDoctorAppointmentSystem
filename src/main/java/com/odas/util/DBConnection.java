package com.odas.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DBConnection provides database connectivity to Oracle Database 21c XE.
 * 
 * Purpose (for viva):
 * - Prioritizes environment variables (ODAS_DB_URL, ODAS_DB_USER, ODAS_DB_PASSWORD) for secure cloud/container deployments.
 * - Falls back to classpath db.properties for local development without hardcoded secrets.
 * - Implements connection management using the Oracle JDBC driver (ojdbc11).
 * - Centralizes database connection retrieval and resource closing for all DAO classes.
 */
public class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    /**
     * Loads database configuration properties from db.properties in the classpath.
     */
    private static void loadProperties() {
        String[] configFiles = {"db.local.properties", "application.local.properties", "db.properties"};
        boolean loaded = false;
        for (String file : configFiles) {
            try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream(file)) {
                if (input != null) {
                    properties.load(input);
                    LOGGER.info("Loaded database configuration from classpath: " + file);
                    loaded = true;
                    break;
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error reading database configuration file: " + file, e);
            }
        }
        if (!loaded) {
            LOGGER.log(Level.INFO, "No local db properties file found in classpath. Using ODAS_DB_* environment variables or defaults.");
        }

        try {
            // Register Oracle JDBC Driver
            String driver = properties.getProperty("db.driver", "oracle.jdbc.OracleDriver");
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load Oracle JDBC driver", e);
        }
    }

    /**
     * Opens and returns a new Connection to the Oracle 21c XE database.
     * Environment variables take precedence over db.properties values.
     * 
     * @return Connection to Oracle DB
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        String envUrl = System.getenv("ODAS_DB_URL");
        if (envUrl == null || envUrl.trim().isEmpty()) {
            envUrl = System.getProperty("ODAS_DB_URL");
        }
        String envUser = System.getenv("ODAS_DB_USER");
        if (envUser == null || envUser.trim().isEmpty()) {
            envUser = System.getProperty("ODAS_DB_USER");
        }
        String envPassword = System.getenv("ODAS_DB_PASSWORD");
        if (envPassword == null || envPassword.trim().isEmpty()) {
            envPassword = System.getProperty("ODAS_DB_PASSWORD");
        }

        String url = (envUrl != null && !envUrl.trim().isEmpty()) ? envUrl.trim() : properties.getProperty("db.url");
        String username = (envUser != null && !envUser.trim().isEmpty()) ? envUser.trim() : properties.getProperty("db.username");
        String password = (envPassword != null) ? envPassword : properties.getProperty("db.password");

        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Checks whether the Oracle database is reachable and accepting connections.
     * 
     * @return true if database connection is successfully established, false otherwise
     */
    public static boolean isAvailable() {
        try (Connection c = getConnection()) {
            return c != null && !c.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Utility method to safely close JDBC resources (Connection, Statement, ResultSet).
     * 
     * @param closeables resources to close quietly
     */
    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error closing database resource", e);
                }
            }
        }
    }
}
