package org.example.demo.dao;

import lombok.extern.slf4j.Slf4j;
import org.example.demo.config.DatabaseConfig;
import org.example.demo.model.Todo;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class TodoDAO {
    
    public List<Todo> findAll() {
        List<Todo> todos = new ArrayList<>();
        String sql = "SELECT * FROM todos ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                todos.add(mapResultSetToTodo(rs));
            }
        } catch (SQLException e) {
            log.error("Error fetching todos: {}", e.getMessage(), e);
        }
        
        return todos;
    }
    
    public Optional<Todo> findById(int id) {
        String sql = "SELECT * FROM todos WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTodo(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching todo by id: {}", e.getMessage(), e);
        }
        
        return Optional.empty();
    }
    
    public boolean save(Todo todo) {
        if (todo.getId() == 0) {
            return insert(todo);
        } else {
            return update(todo);
        }
    }
    
    private boolean insert(Todo todo) {
        String sql = "INSERT INTO todos (title, description, priority, completed, due_date) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, todo.getTitle());
            stmt.setString(2, todo.getDescription());
            stmt.setString(3, todo.getPriority().name());
            stmt.setBoolean(4, todo.isCompleted());
            
            if (todo.getDueDate() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(todo.getDueDate()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        // Set the generated ID back to the todo object
                        setTodoId(todo, generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("Error inserting todo: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    private boolean update(Todo todo) {
        String sql = "UPDATE todos SET title = ?, description = ?, priority = ?, completed = ?, due_date = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, todo.getTitle());
            stmt.setString(2, todo.getDescription());
            stmt.setString(3, todo.getPriority().name());
            stmt.setBoolean(4, todo.isCompleted());
            
            if (todo.getDueDate() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(todo.getDueDate()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }
            
            stmt.setInt(6, todo.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating todo: {}", e.getMessage());
        }
        
        return false;
    }
    
    public boolean deleteById(int id) {
        String sql = "DELETE FROM todos WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error deleting todo: {}", e.getMessage());
        }
        
        return false;
    }
    
    private Todo mapResultSetToTodo(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        Todo.Priority priority = Todo.Priority.valueOf(rs.getString("priority"));
        boolean completed = rs.getBoolean("completed");
        
        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;
        
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
