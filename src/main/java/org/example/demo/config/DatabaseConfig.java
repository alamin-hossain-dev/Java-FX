package org.example.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String DB_URL = "jdbc:mysql://localhost:3306/todo_app?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root"; // Change this to your MySQL password
    
    private static HikariDataSource dataSource;
    
    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USER);
            config.setPassword(DB_PASSWORD);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            dataSource = new HikariDataSource(config);
            
            // Test the connection
            try (Connection testConn = dataSource.getConnection()) {
                logger.info("Database connection established successfully!");
            }
            
            // Initialize database tables
            initializeDatabase();
            
        } catch (Exception e) {
            logger.error("Failed to initialize database: {}", e.getMessage());
            logger.warn("The application will continue without database functionality.");
            logger.warn("Please check your MySQL installation and credentials.");
            logger.error("Database initialization error", e);
            dataSource = null; // Ensure dataSource is null on failure
        }
    }
    
    public static DataSource getDataSource() {
        return dataSource;
    }
    
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection pool is not initialized. Please check your MySQL configuration.");
        }
        return dataSource.getConnection();
    }
    
    private static void initializeDatabase() {
        if (dataSource == null) {
            logger.error("Cannot initialize database - connection pool is null");
            return;
        }
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create a database if it doesn't exist
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS todo_app");
            stmt.executeUpdate("USE todo_app");
            
            // Create todos table
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS todos (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description TEXT,
                    priority ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'MEDIUM',
                    completed BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    due_date TIMESTAMP NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """;
            
            stmt.executeUpdate(createTableSQL);
            
            logger.info("Database initialized successfully!");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize database tables: {}", e.getMessage(), e);
        }
    }
    
    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
