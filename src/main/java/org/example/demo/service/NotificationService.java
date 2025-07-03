package org.example.demo.service;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.example.demo.model.Todo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static NotificationService instance;
    private final ScheduledExecutorService scheduler;
    private TodoService todoService; // Remove final to avoid initialization issues
    private boolean systemTraySupported;
    private SystemTray systemTray;
    private TrayIcon trayIcon;

    private NotificationService() {
        scheduler = Executors.newScheduledThreadPool(2);
        // Don't get TodoService instance here to avoid circular dependency
        initializeSystemTray();
        startNotificationChecker();
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    private void initializeSystemTray() {
        if (SystemTray.isSupported()) {
            systemTraySupported = true;
            systemTray = SystemTray.getSystemTray();
            
            // Create tray icon
            try {
                // Create a simple icon (you can replace with an actual icon file)
                Image icon = Toolkit.getDefaultToolkit().createImage(new byte[0]);
                trayIcon = new TrayIcon(icon, "Todo Reminder");
                trayIcon.setImageAutoSize(true);
                systemTray.add(trayIcon);
                logger.info("System tray initialized successfully");
            } catch (AWTException e) {
                logger.error("Error initializing system tray", e);
                systemTraySupported = false;
            }
        } else {
            logger.warn("System tray not supported on this platform");
            systemTraySupported = false;
        }
    }

    private void startNotificationChecker() {
        // Check for due todos every minute
        scheduler.scheduleAtFixedRate(this::checkForDueTodos, 0, 1, TimeUnit.MINUTES);
        logger.info("Notification checker started - checking every minute");
    }

    private void checkForDueTodos() {
        try {
            // Get TodoService instance when needed to avoid circular dependency
            if (todoService == null) {
                todoService = TodoService.getInstance();
            }
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reminderTime = now.plusMinutes(5);
            
            todoService.getAllTodos().stream()
                .filter(todo -> !todo.isCompleted())
                .filter(todo -> todo.getDueDate() != null)
                .filter(todo -> isWithinReminderWindow(todo.getDueDate(), now, reminderTime))
                .forEach(this::sendNotification);
                
        } catch (Exception e) {
            logger.error("Error checking for due todos", e);
        }
    }

    private boolean isWithinReminderWindow(LocalDateTime dueDate, LocalDateTime now, LocalDateTime reminderTime) {
        // Check if the due date is within the next 5 minutes
        return dueDate.isAfter(now) && dueDate.isBefore(reminderTime.plusMinutes(1));
    }

    private void sendNotification(Todo todo) {
        logger.info("Sending notification for todo: {}", todo.getTitle());
        
        String title = "Todo Reminder";
        String message = String.format("'%s' is due in 5 minutes!\nDue: %s", 
            todo.getTitle(), 
            todo.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));

        // Try system tray notification first
        if (systemTraySupported && trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
        
        // Also show JavaFX dialog notification
        Platform.runLater(() -> showJavaFXNotification(title, message, todo));
    }

    private void showJavaFXNotification(String title, String message, Todo todo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Todo Due Soon!");
        alert.setContentText(message);
        
        // Add custom buttons
        ButtonType markCompleteButton = new ButtonType("Mark Complete");
        ButtonType snoozeButton = new ButtonType("Snooze 10 min");
        ButtonType dismissButton = new ButtonType("Dismiss");
        
        alert.getButtonTypes().setAll(markCompleteButton, snoozeButton, dismissButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == markCompleteButton) {
                markTodoComplete(todo);
            } else if (response == snoozeButton) {
                snoozeTodo(todo, 10);
            }
            // Dismiss does nothing
        });
    }

    private void markTodoComplete(Todo todo) {
        try {
            if (todoService == null) {
                todoService = TodoService.getInstance();
            }
            todo.setCompleted(true);
            todoService.updateTodo(todo);
            logger.info("Marked todo as complete from notification: {}", todo.getTitle());
        } catch (Exception e) {
            logger.error("Error marking todo as complete", e);
        }
    }

    private void snoozeTodo(Todo todo, int minutes) {
        try {
            if (todoService == null) {
                todoService = TodoService.getInstance();
            }
            LocalDateTime newDueDate = todo.getDueDate().plusMinutes(minutes);
            todo.setDueDate(newDueDate);
            todoService.updateTodo(todo);
            logger.info("Snoozed todo for {} minutes: {}", minutes, todo.getTitle());
        } catch (Exception e) {
            logger.error("Error snoozing todo", e);
        }
    }

    public void scheduleNotification(Todo todo) {
        if (todo.getDueDate() == null || todo.isCompleted()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime notificationTime = todo.getDueDate().minusMinutes(5);
        
        if (notificationTime.isAfter(now)) {
            long delayMinutes = java.time.Duration.between(now, notificationTime).toMinutes();
            
            scheduler.schedule(() -> sendNotification(todo), delayMinutes, TimeUnit.MINUTES);
            
            logger.info("Scheduled notification for '{}' in {} minutes", 
                todo.getTitle(), delayMinutes);
        }
    }

    public void cancelNotification(Todo todo) {
        // Note: In a production app, you'd want to track scheduled tasks to cancel them
        logger.info("Notification cancelled for todo: {}", todo.getTitle());
    }

    public void shutdown() {
        scheduler.shutdown();
        if (systemTraySupported && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
        logger.info("NotificationService shutdown completed");
    }

    // Method to test notifications
    public void testNotification() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Test Notification");
            alert.setHeaderText("Notification System Test");
            alert.setContentText("If you can see this, notifications are working!");
            alert.showAndWait();
        });
        
        if (systemTraySupported && trayIcon != null) {
            trayIcon.displayMessage("Test Notification", 
                "System tray notifications are working!", 
                TrayIcon.MessageType.INFO);
        }
    }
}
