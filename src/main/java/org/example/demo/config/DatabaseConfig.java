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
    
    // Configuration keys
    private static final String DB_URL_KEY = "db.url";
    private static final String DB_USERNAME_KEY = "db.username";
    private static final String DB_PASSWORD_KEY = "db.password";
    private static final String DB_SCHEMA_KEY = "db.schema.name";
    private static final String DB_AUTO_CREATE_KEY = "db.schema.autoCreate";
    
    // Connection pool configuration keys
    private static final String POOL_MAX_SIZE_KEY = "db.pool.maximumPoolSize";
    private static final String POOL_MIN_IDLE_KEY = "db.pool.minimumIdle";
    private static final String POOL_CONNECTION_TIMEOUT_KEY = "db.pool.connectionTimeout";
    private static final String POOL_IDLE_TIMEOUT_KEY = "db.pool.idleTimeout";
    private static final String POOL_MAX_LIFETIME_KEY = "db.pool.maxLifetime";
    
    private static HikariDataSource dataSource;
    
    static {
        try {
            // Load database configuration from properties
            String dbUrl = ConfigurationManager.getProperty(DB_URL_KEY);
            String dbUsername = ConfigurationManager.getProperty(DB_USERNAME_KEY);
            String dbPassword = ConfigurationManager.getProperty(DB_PASSWORD_KEY);
            
            // Validate required configuration
            if (dbUrl == null || dbUsername == null || dbPassword == null) {
                throw new IllegalStateException("Missing required database configuration. Please check your database.properties file.");
            }
            
            logger.info("Initializing database connection pool...");
            logger.debug("Database URL: {}", dbUrl.replaceAll("password=[^&]*", "password=***"));
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            
            // Configure connection pool from properties
            config.setMaximumPoolSize(ConfigurationManager.getIntProperty(POOL_MAX_SIZE_KEY, 10));
            config.setMinimumIdle(ConfigurationManager.getIntProperty(POOL_MIN_IDLE_KEY, 2));
            config.setConnectionTimeout(ConfigurationManager.getIntProperty(POOL_CONNECTION_TIMEOUT_KEY, 30000));
            config.setIdleTimeout(ConfigurationManager.getIntProperty(POOL_IDLE_TIMEOUT_KEY, 600000));
            config.setMaxLifetime(ConfigurationManager.getIntProperty(POOL_MAX_LIFETIME_KEY, 1800000));
            
            dataSource = new HikariDataSource(config);
            
            // Test the connection
            try (Connection testConn = dataSource.getConnection()) {
                logger.info("Database connection established successfully!");
            }
            
            // Initialize database tables if auto-create is enabled
            if (ConfigurationManager.getBooleanProperty(DB_AUTO_CREATE_KEY, true)) {
                initializeDatabase();
            }
            
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
            
            String schemaName = ConfigurationManager.getProperty(DB_SCHEMA_KEY, "todo_app");
            
            // Create a database if it doesn't exist
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + schemaName);
            stmt.executeUpdate("USE " + schemaName);
            
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
