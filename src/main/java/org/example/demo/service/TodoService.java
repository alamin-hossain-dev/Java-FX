package org.example.demo.service;

import org.example.demo.dao.TodoDAO;
import org.example.demo.model.Todo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TodoService {
    private static final Logger logger = LoggerFactory.getLogger(TodoService.class);
    private static TodoService instance;
    private ObservableList<Todo> todos;
    private TodoDAO todoDAO;
    private boolean databaseAvailable = true;
    private NotificationService notificationService;

    private TodoService() {
        todos = FXCollections.observableArrayList();
        todoDAO = new TodoDAO();
        loadTodosFromDatabase();
        // Initialize notification service after loading todos
        initializeNotificationService();
    }

    private void initializeNotificationService() {
        try {
            notificationService = NotificationService.getInstance();
            // Schedule notifications for existing todos
            todos.stream()
                .filter(todo -> !todo.isCompleted() && todo.getDueDate() != null)
                .forEach(notificationService::scheduleNotification);
        } catch (Exception e) {
            logger.warn("Could not initialize notification service: {}", e.getMessage());
        }
    }

    public static TodoService getInstance() {
        if (instance == null) {
            instance = new TodoService();
        }
        return instance;
    }

    private void loadTodosFromDatabase() {
        try {
            todos.clear();
            todos.addAll(todoDAO.findAll());
            databaseAvailable = true;
            logger.info("Loaded {} todos from database", todos.size());
        } catch (Exception e) {
            logger.error("Error loading todos from database: {}", e.getMessage());
            logger.warn("Falling back to in-memory storage");
            databaseAvailable = false;
            loadSampleDataForInMemoryMode();
        }
    }

    private void loadSampleDataForInMemoryMode() {
        if (todos.isEmpty()) {
            logger.info("Loading sample data for in-memory mode");
            // Add some sample data when database is not available
            todos.add(new Todo(
                "Welcome to Todo App (In-Memory Mode)", 
                "Database connection failed. Your data will not be persisted.",
                Todo.Priority.HIGH,
                null
            ));
            
            todos.add(new Todo(
                "Setup MySQL Database", 
                "Please check your MySQL installation and credentials to enable data persistence.",
                Todo.Priority.MEDIUM,
                null
            ));
        }
    }

    public ObservableList<Todo> getAllTodos() {
        return todos;
    }

    public void addTodo(Todo todo) {
        if (databaseAvailable) {
            try {
                if (todoDAO.save(todo)) {
                    todos.add(todo);
                    // Schedule notification for the new todo
                    if (notificationService != null) {
                        notificationService.scheduleNotification(todo);
                    }
                } else {
                    throw new RuntimeException("Failed to save todo to database");
                }
            } catch (Exception e) {
                logger.error("Database error, falling back to in-memory: {}", e.getMessage());
                databaseAvailable = false;
                todos.add(todo);
                // Schedule notification even in in-memory mode
                if (notificationService != null) {
                    notificationService.scheduleNotification(todo);
                }
            }
        } else {
            todos.add(todo);
            // Schedule notification for in-memory mode
            if (notificationService != null) {
                notificationService.scheduleNotification(todo);
            }
        }
    }

    public void updateTodo(Todo todo) {
        if (databaseAvailable) {
            try {
                if (todoDAO.save(todo)) {
                    // Find and replace the existing todo in the list
                    for (int i = 0; i < todos.size(); i++) {
                        if (todos.get(i).getId() == todo.getId()) {
                            todos.set(i, todo);
                            break;
                        }
                    }
                    // Update notification schedule
                    if (notificationService != null) {
                        if (todo.isCompleted()) {
                            notificationService.cancelNotification(todo);
                        } else {
                            notificationService.scheduleNotification(todo);
                        }
                    }
                } else {
                    throw new RuntimeException("Failed to update todo in database");
                }
            } catch (Exception e) {
                logger.error("Database error, updating in-memory only: {}", e.getMessage());
                databaseAvailable = false;
                // Update in memory only
                for (int i = 0; i < todos.size(); i++) {
                    if (todos.get(i).getId() == todo.getId()) {
                        todos.set(i, todo);
                        break;
                    }
                }
                // Update notification even in in-memory mode
                if (notificationService != null) {
                    if (todo.isCompleted()) {
                        notificationService.cancelNotification(todo);
                    } else {
                        notificationService.scheduleNotification(todo);
                    }
                }
            }
        } else {
            // Update in memory only
            for (int i = 0; i < todos.size(); i++) {
                Todo existing = todos.get(i);
                if (existing.getId() == todo.getId() || existing.getTitle().equals(todo.getTitle())) {
                    existing.setTitle(todo.getTitle());
                    existing.setDescription(todo.getDescription());
                    existing.setPriority(todo.getPriority());
                    existing.setCompleted(todo.isCompleted());
                    existing.setDueDate(todo.getDueDate());
                    break;
                }
            }
            // Update notification for in-memory mode
            if (notificationService != null) {
                if (todo.isCompleted()) {
                    notificationService.cancelNotification(todo);
                } else {
                    notificationService.scheduleNotification(todo);
                }
            }
        }
    }

    public void deleteTodo(Todo todo) {
        if (databaseAvailable) {
            try {
                if (todoDAO.deleteById(todo.getId())) {
                    todos.remove(todo);
                    // Cancel notification for deleted todo
                    if (notificationService != null) {
                        notificationService.cancelNotification(todo);
                    }
                } else {
                    throw new RuntimeException("Failed to delete todo from database");
                }
            } catch (Exception e) {
                logger.error("Database error, deleting from memory only: {}", e.getMessage());
                databaseAvailable = false;
                todos.remove(todo);
                // Cancel notification even in in-memory mode
                if (notificationService != null) {
                    notificationService.cancelNotification(todo);
                }
            }
        } else {
            todos.remove(todo);
            // Cancel notification for in-memory mode
            if (notificationService != null) {
                notificationService.cancelNotification(todo);
            }
        }
    }

    public void deleteTodoById(int id) {
        if (databaseAvailable) {
            try {
                if (todoDAO.deleteById(id)) {
                    todos.removeIf(todo -> todo.getId() == id);
                } else {
                    throw new RuntimeException("Failed to delete todo from database");
                }
            } catch (Exception e) {
                logger.error("Database error, deleting from memory only: {}", e.getMessage());
                databaseAvailable = false;
                todos.removeIf(todo -> todo.getId() == id);
            }
        } else {
            todos.removeIf(todo -> todo.getId() == id);
        }
    }

    public Optional<Todo> getTodoById(int id) {
        return todos.stream()
                .filter(todo -> todo.getId() == id)
                .findFirst();
    }

    public ObservableList<Todo> getCompletedTodos() {
        return todos.filtered(Todo::isCompleted);
    }

    public ObservableList<Todo> getPendingTodos() {
        return todos.filtered(todo -> !todo.isCompleted());
    }

    public ObservableList<Todo> getOverdueTodos() {
        return todos.filtered(Todo::isOverdue);
    }

    public int getTotalCount() {
        return todos.size();
    }

    public int getCompletedCount() {
        return (int) todos.stream().filter(Todo::isCompleted).count();
    }

    public int getPendingCount() {
        return (int) todos.stream().filter(todo -> !todo.isCompleted()).count();
    }

    public int getOverdueCount() {
        return (int) todos.stream().filter(Todo::isOverdue).count();
    }

    public void refreshFromDatabase() {
        if (databaseAvailable) {
            loadTodosFromDatabase();
        }
    }

    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }

    // Notification-related methods
    public void testNotification() {
        if (notificationService != null) {
            notificationService.testNotification();
        }
    }

    public void shutdown() {
        if (notificationService != null) {
            notificationService.shutdown();
        }
    }
}
