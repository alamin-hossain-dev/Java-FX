package org.example.demo.service.impl;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import org.example.demo.model.Todo;
import org.example.demo.repository.TodoRepository;
import org.example.demo.repository.impl.TodoRepositoryImpl;
import org.example.demo.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Modern TodoService implementation using Repository pattern
 * Following Clean Architecture and Domain Driven Design principles
 */
@Slf4j
public class TodoServiceImpl {
    
    private static TodoServiceImpl instance;
    private final TodoRepository todoRepository;
    private final ObservableList<Todo> todos;
    private final NotificationService notificationService;
    private boolean databaseAvailable = true;
    
    private TodoServiceImpl() {
        this.todoRepository = new TodoRepositoryImpl();
        this.todos = FXCollections.observableArrayList();
        this.notificationService = initializeNotificationService();
        loadTodosFromRepository();
    }
    
    public static TodoServiceImpl getInstance() {
        if (instance == null) {
            instance = new TodoServiceImpl();
        }
        return instance;
    }
    
    // CRUD Operations
    
    public void createTodo(Todo todo) {
        try {
            Todo savedTodo = todoRepository.save(todo);
            todos.add(savedTodo);
            
            // Schedule notification if due date is set and not completed
            if (notificationService != null && savedTodo.getDueDate() != null && !savedTodo.isCompleted()) {
                notificationService.scheduleNotification(savedTodo);
            }
            
            log.info("Created todo: {}", savedTodo.getTitle());

        } catch (Exception e) {
            log.error("Failed to create todo '{}': {}", todo.getTitle(), e.getMessage(), e);
            databaseAvailable = false;
            
            // Fallback to in-memory
            todo.setId(generateInMemoryId());
            todos.add(todo);
        }
    }
    
    public void updateTodo(Todo todo) {
        try {
            Todo updatedTodo = todoRepository.save(todo);
            
            // Update in an observable list
            for (int i = 0; i < todos.size(); i++) {
                if (todos.get(i).getId() == updatedTodo.getId()) {
                    todos.set(i, updatedTodo);
                    break;
                }
            }
            
            // Handle notification scheduling
            if (notificationService != null) {
                if (updatedTodo.isCompleted() || updatedTodo.getDueDate() == null) {
                    notificationService.cancelNotification(updatedTodo);
                } else {
                    notificationService.scheduleNotification(updatedTodo);
                }
            }
            
            log.info("Updated todo: {}", updatedTodo.getTitle());

        } catch (Exception e) {
            log.error("Failed to update todo '{}': {}", todo.getTitle(), e.getMessage(), e);
            databaseAvailable = false;
            
            // Fallback to in-memory update
            for (int i = 0; i < todos.size(); i++) {
                if (todos.get(i).getId() == todo.getId()) {
                    todos.set(i, todo);
                    break;
                }
            }
        }
    }
    
    public void deleteTodo(Todo todo) {
        try {
            todoRepository.delete(todo);
            todos.remove(todo);
            
            // Cancel any scheduled notifications
            if (notificationService != null) {
                notificationService.cancelNotification(todo);
            }
            
            log.info("Deleted todo: {}", todo.getTitle());
            
        } catch (Exception e) {
            log.error("Failed to delete todo '{}': {}", todo.getTitle(), e.getMessage(), e);
            databaseAvailable = false;
            
            // Fallback to in-memory deletion
            todos.remove(todo);
        }
    }
    
    public void deleteTodoById(int id) {
        Optional<Todo> todo = findTodoById(id);
        if (todo.isPresent()) {
            deleteTodo(todo.get());
        } else {
            log.warn("Attempted to delete non-existent todo with id: {}", id);
        }
    }
    
    // Query Operations
    
    public Optional<Todo> findTodoById(int id) {
        return todos.stream()
                .filter(todo -> todo.getId() == id)
                .findFirst();
    }
    
    public ObservableList<Todo> getAllTodos() {
        return FXCollections.unmodifiableObservableList(todos);
    }
    
    public List<Todo> getCompletedTodos() {
        if (databaseAvailable) {
            try {
                return todoRepository.findCompletedTodos();
            } catch (Exception e) {
                log.warn("Database query failed, using in-memory filter: {}", e.getMessage());
                databaseAvailable = false;
            }
        }
        
        return todos.stream()
                .filter(Todo::isCompleted)
                .collect(Collectors.toList());
    }
    
    public List<Todo> getPendingTodos() {
        if (databaseAvailable) {
            try {
                return todoRepository.findPendingTodos();
            } catch (Exception e) {
                log.warn("Database query failed, using in-memory filter: {}", e.getMessage());
                databaseAvailable = false;
            }
        }
        
        return todos.stream()
                .filter(todo -> !todo.isCompleted())
                .collect(Collectors.toList());
    }
    
    public List<Todo> getOverdueTodos() {
        if (databaseAvailable) {
            try {
                return todoRepository.findOverdueTodos();
            } catch (Exception e) {
                log.warn("Database query failed, using in-memory filter: {}", e.getMessage());
                databaseAvailable = false;
            }
        }
        
        return todos.stream()
                .filter(Todo::isOverdue)
                .collect(Collectors.toList());
    }
    
    public List<Todo> getTodosByPriority(Todo.Priority priority) {
        if (databaseAvailable) {
            try {
                return todoRepository.findByPriority(priority);
            } catch (Exception e) {
                log.warn("Database query failed, using in-memory filter: {}", e.getMessage());
                databaseAvailable = false;
            }
        }
        
        return todos.stream()
                .filter(todo -> todo.getPriority() == priority)
                .collect(Collectors.toList());
    }
    
    public List<Todo> searchTodos(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return List.copyOf(todos);
        }
        
        if (databaseAvailable) {
            try {
                return todoRepository.findByTitleOrDescriptionContaining(searchText.trim());
            } catch (Exception e) {
                log.warn("Database search failed, using in-memory search: {}", e.getMessage());
                databaseAvailable = false;
            }
        }
        
        String searchLower = searchText.toLowerCase().trim();
        return todos.stream()
                .filter(todo -> 
                    todo.getTitle().toLowerCase().contains(searchLower) ||
                    todo.getDescription().toLowerCase().contains(searchLower)
                )
                .collect(Collectors.toList());
    }
    
    public List<Todo> getTodosDueBetween(LocalDateTime start, LocalDateTime end) {
        if (databaseAvailable) {
            try {
                return todoRepository.findTodosDueBetween(start, end);
            } catch (Exception e) {
                log.warn("Database query failed, using in-memory filter: {}", e.getMessage());
                databaseAvailable = false;
            }
        }
        
        return todos.stream()
                .filter(todo -> todo.getDueDate() != null)
                .filter(todo -> !todo.getDueDate().isBefore(start) && !todo.getDueDate().isAfter(end))
                .collect(Collectors.toList());
    }
    
    // Statistics
    
    public long getTotalCount() {
        return todos.size();
    }
    
    public long getCompletedCount() {
        if (databaseAvailable) {
            try {
                return todoRepository.countByCompleted(true);
            } catch (Exception e) {
                log.warn("Database count failed, using in-memory count: {}", e.getMessage());
                databaseAvailable = false;
            }
        }
        
        return todos.stream()
                .mapToLong(todo -> todo.isCompleted() ? 1 : 0)
                .sum();
    }
    
    public long getPendingCount() {
        if (databaseAvailable) {
            try {
                return todoRepository.countByCompleted(false);
            } catch (Exception e) {
                log.warn("Database count failed, using in-memory count: {}", e.getMessage());
                databaseAvailable = false;
            }
        }
        
        return todos.stream()
                .mapToLong(todo -> !todo.isCompleted() ? 1 : 0)
                .sum();
    }
    
    public long getOverdueCount() {
        if (databaseAvailable) {
            try {
                return todoRepository.countOverdueTodos();
            } catch (Exception e) {
                log.warn("Database count failed, using in-memory count: {}", e.getMessage());
                databaseAvailable = false;
            }
        }
        
        return todos.stream()
                .mapToLong(todo -> todo.isOverdue() ? 1 : 0)
                .sum();
    }
    
    // Service Status
    
    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }
    
    public void refreshFromDatabase() {
        loadTodosFromRepository();
    }
    
    public void shutdown() {
        if (notificationService != null) {
            notificationService.shutdown();
        }
        log.info("TodoService shutdown completed");
    }
    
    // Compatibility methods for existing controller code
    
    /**
     * Compatibility method - delegates to createTodo()
     */
    public void addTodo(Todo todo) {
        createTodo(todo);
    }
    
    /**
     * Compatibility method - delegates to findTodoById()
     */
    public Optional<Todo> getTodoById(int id) {
        return findTodoById(id);
    }
    
    /**
     * Test notification method for debugging
     */
    public void testNotification() {
        if (notificationService != null) {
            notificationService.testNotification();
        } else {
            log.warn("Notification service not available for testing");
        }
    }
    
    // Private Helper Methods
    
    private void loadTodosFromRepository() {
        try {
            List<Todo> loadedTodos = todoRepository.findAllOrderByCreatedAtDesc();
            todos.clear();
            todos.addAll(loadedTodos);
            databaseAvailable = true;
            
            log.info("Loaded {} todos from repository", todos.size());
            
            // Schedule notifications for existing todos
            if (notificationService != null) {
                todos.stream()
                    .filter(todo -> !todo.isCompleted() && todo.getDueDate() != null)
                    .forEach(notificationService::scheduleNotification);
            }
            
        } catch (Exception e) {
            log.error("Failed to load todos from repository: {}", e.getMessage(), e);
            databaseAvailable = false;
            loadSampleDataForInMemoryMode();
        }
    }
    
    private void loadSampleDataForInMemoryMode() {
        if (todos.isEmpty()) {
            log.info("Loading sample data for in-memory mode");
            
            Todo sampleTodo = new Todo(
                "Welcome to Todo App (In-Memory Mode)", 
                "Database connection failed. Your data will not be persisted.",
                Todo.Priority.HIGH,
                null
            );
            sampleTodo.setId(generateInMemoryId());
            todos.add(sampleTodo);
        }
    }
    
    private NotificationService initializeNotificationService() {
        try {
            return NotificationService.getInstance();
        } catch (Exception e) {
            log.warn("Could not initialize notification service: {}", e.getMessage());
            return null;
        }
    }
    
    private int generateInMemoryId() {
        return todos.stream()
                .mapToInt(Todo::getId)
                .max()
                .orElse(0) + 1;
    }
}
