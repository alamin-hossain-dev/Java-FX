package org.example.demo.exception;

/**
 * Custom exception for repository layer operations
 * Provides better error handling and debugging information
 */
public class RepositoryException extends RuntimeException {
    
    private final String operation;
    private final Object entityId;
    
    public RepositoryException(String message) {
        super(message);
        this.operation = "unknown";
        this.entityId = null;
    }
    
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
        this.operation = "unknown";
        this.entityId = null;
    }
    
    public RepositoryException(String operation, Object entityId, String message, Throwable cause) {
        super(String.format("Repository operation '%s' failed for entity ID '%s': %s", operation, entityId, message));
        this.operation = operation;
        this.entityId = entityId;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public Object getEntityId() {
        return entityId;
    }
}
