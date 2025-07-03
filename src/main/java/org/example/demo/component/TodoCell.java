package org.example.demo.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.demo.model.Todo;

public class TodoCell extends ListCell<Todo> {
    private VBox container;
    private HBox mainContent;
    private HBox actionButtons;
    private CheckBox completedCheckBox;
    private Label titleLabel;
    private Label descriptionLabel;
    private Label priorityLabel;
    private Label dueDateLabel;
    private Button editButton;
    private Button deleteButton;

    private final TodoCellCallback callback;

    public interface TodoCellCallback {
        void onEdit(Todo todo);
        void onDelete(Todo todo);
        void onToggleComplete(Todo todo);
    }

    public TodoCell(TodoCellCallback callback) {
        this.callback = callback;
        createLayout();
    }

    private void createLayout() {
        container = new VBox(5);
        container.setPadding(new Insets(10));

        mainContent = new HBox(10);
        mainContent.setAlignment(Pos.CENTER_LEFT);

        // Checkbox for completion
        completedCheckBox = new CheckBox();
        completedCheckBox.setOnAction(e -> {
            if (getItem() != null && callback != null) {
                callback.onToggleComplete(getItem());
            }
        });

        // Content area
        VBox contentArea = new VBox(3);
        contentArea.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        titleLabel = new Label();
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        descriptionLabel = new Label();
        descriptionLabel.setTextFill(Color.GRAY);
        descriptionLabel.setWrapText(true);

        HBox metaInfo = new HBox(15);
        priorityLabel = new Label();
        dueDateLabel = new Label();
        dueDateLabel.setTextFill(Color.DARKGRAY);
        dueDateLabel.setFont(Font.font("System", 11));
        metaInfo.getChildren().addAll(priorityLabel, dueDateLabel);

        contentArea.getChildren().addAll(titleLabel, descriptionLabel, metaInfo);

        // Action buttons
        actionButtons = new HBox(5);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        editButton = new Button("Edit");
        editButton.getStyleClass().add("edit-button");
        editButton.setOnAction(e -> {
            if (getItem() != null && callback != null) {
                callback.onEdit(getItem());
            }
        });

        deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("delete-button");
        deleteButton.setOnAction(e -> {
            if (getItem() != null && callback != null) {
                callback.onDelete(getItem());
            }
        });

        actionButtons.getChildren().addAll(editButton, deleteButton);

        mainContent.getChildren().addAll(completedCheckBox, contentArea, actionButtons);
        container.getChildren().add(mainContent);
    }

    @Override
    protected void updateItem(Todo todo, boolean empty) {
        super.updateItem(todo, empty);

        if (empty || todo == null) {
            setGraphic(null);
        } else {
            updateContent(todo);
            setGraphic(container);
        }
    }

    private void updateContent(Todo todo) {
        completedCheckBox.setSelected(todo.isCompleted());
        titleLabel.setText(todo.getTitle());
        descriptionLabel.setText(todo.getDescription());

        // Update priority label
        priorityLabel.setText(todo.getPriority().getDisplayName());
        priorityLabel.setStyle("-fx-background-color: " + todo.getPriority().getColor() + 
                              "; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3;");

        // Update due date
        dueDateLabel.setText("Due: " + todo.getFormattedDueDate());
        if (todo.isOverdue()) {
            dueDateLabel.setTextFill(Color.RED);
            dueDateLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        } else {
            dueDateLabel.setTextFill(Color.DARKGRAY);
            dueDateLabel.setFont(Font.font("System", 11));
        }

        // Style based on completion status
        if (todo.isCompleted()) {
            titleLabel.setStyle("-fx-strikethrough: true; -fx-text-fill: gray;");
            descriptionLabel.setStyle("-fx-strikethrough: true; -fx-text-fill: lightgray;");
            container.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 5;");
        } else {
            titleLabel.setStyle("-fx-strikethrough: false; -fx-text-fill: black;");
            descriptionLabel.setStyle("-fx-strikethrough: false; -fx-text-fill: gray;");
            if (todo.isOverdue()) {
                container.setStyle("-fx-background-color: #ffebee; -fx-background-radius: 5; -fx-border-color: #f44336; -fx-border-radius: 5;");
            } else {
                container.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
            }
        }
    }
}
