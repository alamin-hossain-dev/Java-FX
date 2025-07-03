package org.example.demo.repository;

import org.example.demo.model.Todo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Todo repository interface with all necessary operations
 * Simplified approach without generic base interface
 */
public interface TodoRepository {
    
    // Basic CRUD Operations
    Todo save(Todo todo);
    Optional<Todo> findById(int id);
    List<Todo> findAll();
    void deleteById(int id);
    void delete(Todo todo);
    boolean existsById(int id);
    long count();
    
    // Domain-specific Query Methods
    List<Todo> findByCompleted(boolean completed);
    List<Todo> findByPriority(Todo.Priority priority);
    List<Todo> findOverdueTodos();
    List<Todo> findTodosDueBetween(LocalDateTime start, LocalDateTime end);
    List<Todo> findByTitleOrDescriptionContaining(String searchText);
    List<Todo> findPendingTodos();
    List<Todo> findCompletedTodos();
    long countByCompleted(boolean completed);
    long countOverdueTodos();
    List<Todo> findAllOrderByCreatedAtDesc();
    List<Todo> findAllOrderByDueDateAsc();
}
