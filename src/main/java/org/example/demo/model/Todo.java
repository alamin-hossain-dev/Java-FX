package org.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Todo {
    // Getters and Setters
    private int id;
    private String title;
    private String description;
    private Priority priority;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;

    @Getter
    public enum Priority {
        LOW("Low", "#4CAF50"),
        MEDIUM("Medium", "#FF9800"),
        HIGH("High", "#F44336");

        private final String displayName;
        private final String color;

        Priority(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public Todo(String title, String description, Priority priority, LocalDateTime dueDate) {
        this.id = 0; // Will be set by database
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.dueDate = dueDate;
    }

    public String getFormattedCreatedAt() {
        return createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }

    public String getFormattedDueDate() {
        if (dueDate == null) return "No due date";
        return dueDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }

    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && !completed;
    }

    @Override
    public String toString() {
        return title;
    }
}
