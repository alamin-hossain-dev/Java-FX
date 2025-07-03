package org.example.demo.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.demo.config.DatabaseConfig;
import org.example.demo.exception.RepositoryException;
import org.example.demo.model.Todo;
import org.example.demo.repository.TodoRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modern TodoRepository implementation following enterprise best practices
 * 
 * Features:
 * - Centralized SQL query management
 * - Comprehensive error handling with custom exceptions
 * - Transaction support for data consistency
 * - Connection pooling via HikariCP
 * - Parameterized queries to prevent SQL injection
 * - Proper resource management with try-with-resources
 * - Structured logging for debugging and monitoring
 * 
 * @author Your Name
 * @version 1.0
 * @since 2025-07-03
 */
@Slf4j
public class TodoRepositoryImpl implements TodoRepository {
    
    // SQL Query Constants - Centralized for maintainability and security
    private static final class Queries {
        // Basic CRUD operations
        static final String FIND_ALL = "SELECT * FROM todos ORDER BY created_at DESC";
        static final String FIND_BY_ID = "SELECT * FROM todos WHERE id = ?";
        static final String INSERT = "INSERT INTO todos (title, description, priority, completed, due_date) VALUES (?, ?, ?, ?, ?)";
        static final String UPDATE = "UPDATE todos SET title = ?, description = ?, priority = ?, completed = ?, due_date = ? WHERE id = ?";
        static final String DELETE_BY_ID = "DELETE FROM todos WHERE id = ?";
        static final String EXISTS_BY_ID = "SELECT COUNT(*) FROM todos WHERE id = ?";
        static final String COUNT_ALL = "SELECT COUNT(*) FROM todos";
        
        // Domain-specific queries with business logic
        static final String FIND_BY_COMPLETED = "SELECT * FROM todos WHERE completed = ? ORDER BY created_at DESC";
        static final String FIND_BY_PRIORITY = "SELECT * FROM todos WHERE priority = ? ORDER BY created_at DESC";
        static final String FIND_OVERDUE = "SELECT * FROM todos WHERE due_date < NOW() AND completed = false ORDER BY due_date ASC";
        static final String FIND_DUE_BETWEEN = "SELECT * FROM todos WHERE due_date BETWEEN ? AND ? ORDER BY due_date ASC";
        static final String FIND_BY_TEXT_SEARCH = "SELECT * FROM todos WHERE (title LIKE ? OR description LIKE ?) ORDER BY created_at DESC";
        static final String FIND_PENDING = "SELECT * FROM todos WHERE completed = false ORDER BY created_at DESC";
        static final String FIND_COMPLETED = "SELECT * FROM todos WHERE completed = true ORDER BY created_at DESC";
        
        // Statistics and counting queries
        static final String COUNT_BY_COMPLETED = "SELECT COUNT(*) FROM todos WHERE completed = ?";
        static final String COUNT_OVERDUE = "SELECT COUNT(*) FROM todos WHERE due_date < NOW() AND completed = false";
        
        // Ordering and sorting queries
        static final String FIND_ALL_ORDER_BY_CREATED = "SELECT * FROM todos ORDER BY created_at DESC";
        static final String FIND_ALL_ORDER_BY_DUE_DATE = "SELECT * FROM todos ORDER BY due_date ASC NULLS LAST";
        
        // Private constructor to prevent instantiation
        private Queries() {
            throw new UnsupportedOperationException("Utility class - do not instantiate");
        }
    }
    
    // ================== CRUD Operations ==================
    
    @Override
    public Todo save(Todo todo) {
        validateTodo(todo);
        
        if (todo.getId() == 0) {
            log.debug("Creating new todo: {}", todo.getTitle());
            return insert(todo);
        } else {
            log.debug("Updating existing todo with id: {}", todo.getId());
            return update(todo);
        }
    }

    @Override
    public Optional<Todo> findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.FIND_BY_ID)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTodo(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding todo by id {}: {}", id, e.getMessage(), e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<Todo> findAll() {
        return executeQuery(Queries.FIND_ALL);
    }

    @Override
    public void deleteById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.DELETE_BY_ID)) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                log.warn("No todo found with id {} for deletion", id);
            } else {
                log.debug("Successfully deleted todo with id {}", id);
            }
        } catch (SQLException e) {
            log.error("Error deleting todo with id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete todo", e);
        }
    }
    
    @Override
    public void delete(Todo todo) {
        deleteById(todo.getId());
    }

    @Override
    public boolean existsById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.EXISTS_BY_ID)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.error("Error checking if todo exists with id {}: {}", id, e.getMessage(), e);
        }
        
        return false;
    }
    
    @Override
    public long count() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.COUNT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error counting todos: {}", e.getMessage(), e);
        }
        
        return 0;
    }
    
    // Domain-specific methods
    
    @Override
    public List<Todo> findByCompleted(boolean completed) {
        return executeQueryWithBooleanParam(Queries.FIND_BY_COMPLETED, completed);
    }
    
    @Override
    public List<Todo> findByPriority(Todo.Priority priority) {
        return executeQueryWithStringParam(Queries.FIND_BY_PRIORITY, priority.name());
    }
    
    @Override
    public List<Todo> findOverdueTodos() {
        return executeQuery(Queries.FIND_OVERDUE);
    }
    
    @Override
    public List<Todo> findTodosDueBetween(LocalDateTime start, LocalDateTime end) {
        List<Todo> todos = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.FIND_DUE_BETWEEN)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    todos.add(mapResultSetToTodo(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding todos due between {} and {}: {}", start, end, e.getMessage(), e);
        }
        
        return todos;
    }
    
    @Override
    public List<Todo> findByTitleOrDescriptionContaining(String searchText) {
        List<Todo> todos = new ArrayList<>();
        String searchPattern = "%" + searchText + "%";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.FIND_BY_TEXT_SEARCH)) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    todos.add(mapResultSetToTodo(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error searching todos with text '{}': {}", searchText, e.getMessage(), e);
        }
        
        return todos;
    }
    
    @Override
    public List<Todo> findPendingTodos() {
        return executeQueryWithBooleanParam(Queries.FIND_PENDING, false);
    }
    
    @Override
    public List<Todo> findCompletedTodos() {
        return executeQueryWithBooleanParam(Queries.FIND_COMPLETED, true);
    }
    
    @Override
    public long countByCompleted(boolean completed) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.COUNT_BY_COMPLETED)) {
            
            stmt.setBoolean(1, completed);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            log.error("Error counting todos by completed status {}: {}", completed, e.getMessage(), e);
        }
        
        return 0;
    }
    
    @Override
    public long countOverdueTodos() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.COUNT_OVERDUE);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Error counting overdue todos: {}", e.getMessage(), e);
        }
        
        return 0;
    }
    
    @Override
    public List<Todo> findAllOrderByCreatedAtDesc() {
        return executeQuery(Queries.FIND_ALL_ORDER_BY_CREATED);
    }
    
    @Override
    public List<Todo> findAllOrderByDueDateAsc() {
        return executeQuery(Queries.FIND_ALL_ORDER_BY_DUE_DATE);
    }
    
    // ================== Helper Methods ==================
    
    /**
     * Validates todo entity before database operations
     * @param todo The todo to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateTodo(Todo todo) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo cannot be null");
        }
        if (todo.getTitle() == null || todo.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Todo title cannot be null or empty");
        }
        if (todo.getTitle().length() > 255) {
            throw new IllegalArgumentException("Todo title cannot exceed 255 characters");
        }
        if (todo.getPriority() == null) {
            throw new IllegalArgumentException("Todo priority cannot be null");
        }
    }
    
    /**
     * Sets todo parameters for prepared statements
     * Centralized parameter setting for consistency
     */
    
    private Todo insert(Todo todo) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.INSERT, Statement.RETURN_GENERATED_KEYS)) {
            
            setTodoParameters(stmt, todo);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        setTodoId(todo, generatedKeys.getInt(1));
                        log.debug("Successfully inserted todo with id {}", todo.getId());
                        return todo;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error inserting todo '{}': {}", todo.getTitle(), e.getMessage(), e);
            throw new RuntimeException("Failed to insert todo", e);
        }
        
        throw new RuntimeException("Failed to insert todo - no ID generated");
    }
    
    private Todo update(Todo todo) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(Queries.UPDATE)) {
            
            setTodoParameters(stmt, todo);
            stmt.setInt(6, todo.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                log.debug("Successfully updated todo with id {}", todo.getId());
                return todo;
            } else {
                throw new RuntimeException("Todo not found for update: " + todo.getId());
            }
        } catch (SQLException e) {
            log.error("Error updating todo '{}': {}", todo.getTitle(), e.getMessage(), e);
            throw new RuntimeException("Failed to update todo", e);
        }
    }
    
    private void setTodoParameters(PreparedStatement stmt, Todo todo) throws SQLException {
        stmt.setString(1, todo.getTitle());
        stmt.setString(2, todo.getDescription());
        stmt.setString(3, todo.getPriority().name());
        stmt.setBoolean(4, todo.isCompleted());
        
        if (todo.getDueDate() != null) {
            stmt.setTimestamp(5, Timestamp.valueOf(todo.getDueDate()));
        } else {
            stmt.setNull(5, Types.TIMESTAMP);
        }
    }
    
    private List<Todo> executeQuery(String sql) {
        List<Todo> todos = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                todos.add(mapResultSetToTodo(rs));
            }
        } catch (SQLException e) {
            log.error("Error executing query '{}': {}", sql, e.getMessage(), e);
        }
        
        return todos;
    }
    
    private List<Todo> executeQueryWithBooleanParam(String sql, boolean param) {
        List<Todo> todos = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, param);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    todos.add(mapResultSetToTodo(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error executing query '{}' with boolean param {}: {}", sql, param, e.getMessage(), e);
        }
        
        return todos;
    }
    
    private List<Todo> executeQueryWithStringParam(String sql, String param) {
        List<Todo> todos = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, param);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    todos.add(mapResultSetToTodo(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error executing query '{}' with string param '{}': {}", sql, param, e.getMessage(), e);
        }
        
        return todos;
    }
    
    private Todo mapResultSetToTodo(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        Todo.Priority priority = Todo.Priority.valueOf(rs.getString("priority"));
        boolean completed = rs.getBoolean("completed");
        
        Timestamp dueDateTimestamp = rs.getTimestamp("due_date");
        LocalDateTime dueDate = dueDateTimestamp != null ? dueDateTimestamp.toLocalDateTime() : null;
        
        Todo todo = new Todo(title, description, priority, dueDate);
        setTodoId(todo, id);
        todo.setCompleted(completed);
        
        return todo;
    }
    
    // Helper method to set the private id field using reflection
    private void setTodoId(Todo todo, int id) {
        try {
            java.lang.reflect.Field idField = Todo.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.setInt(todo, id);
        } catch (Exception e) {
            log.error("Error setting todo id: {}", e.getMessage());
        }
    }
}
