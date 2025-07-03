package org.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration properties loader following enterprise software architecture best practices.
 * Supports multiple configuration sources with fallback mechanism.
 */
public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    
    private static final String[] CONFIG_FILES = {
        "database-local.properties",    // Local override (highest priority)
        "database.properties",          // Default configuration
        "database-default.properties"   // Fallback configuration
    };
    
    private static Properties properties;
    
    static {
        loadConfiguration();
    }
    
    private static void loadConfiguration() {
        properties = new Properties();
        
        // Load configuration files in order of priority
        for (String configFile : CONFIG_FILES) {
            if (loadPropertiesFile(configFile)) {
                logger.info("Loaded configuration from: {}", configFile);
                break; // Use the first available configuration file
            }
        }
        
        // Override with environment variables if present
        loadEnvironmentVariables();
        
        // Override with system properties (highest priority)
        loadSystemProperties();
    }
    
    private static boolean loadPropertiesFile(String fileName) {
        try (InputStream input = ConfigurationManager.class.getClassLoader()
                .getResourceAsStream(fileName)) {
            
            if (input != null) {
                properties.load(input);
                return true;
            }
        } catch (IOException e) {
            logger.debug("Could not load configuration file: {} ({})", fileName, e.getMessage());
        }
        return false;
    }
    
    private static void loadEnvironmentVariables() {
        // Map environment variables to properties
        String[] envMappings = {
            "DB_URL:db.url",
            "DB_USERNAME:db.username", 
            "DB_PASSWORD:db.password",
            "DB_SCHEMA:db.schema.name"
        };
        
        for (String mapping : envMappings) {
            String[] parts = mapping.split(":");
            String envVar = System.getenv(parts[0]);
            if (envVar != null) {
                properties.setProperty(parts[1], envVar);
                logger.debug("Loaded {} from environment variable", parts[1]);
            }
        }
    }
    
    private static void loadSystemProperties() {
        // Override with system properties (e.g., -Ddb.url=...)
        System.getProperties().forEach((key, value) -> {
            String keyStr = key.toString();
            if (keyStr.startsWith("db.")) {
                properties.setProperty(keyStr, value.toString());
                logger.debug("Loaded {} from system property", keyStr);
            }
        });
    }
    
    /**
     * Get a configuration property with a default value
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get a configuration property
     */
    public static String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            logger.warn("Configuration property '{}' not found", key);
        }
        return value;
    }
    
    /**
     * Get a configuration property as integer
     */
    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for property '{}': {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get a configuration property as boolean
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    /**
     * Check if a property exists
     */
    public static boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    /**
     * Reload configuration (useful for testing or dynamic reconfiguration)
     */
    public static void reload() {
        logger.info("Reloading configuration...");
        loadConfiguration();
    }
}
