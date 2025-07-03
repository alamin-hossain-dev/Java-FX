package org.example.demo.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.example.demo.component.TodoCell;
import org.example.demo.model.Todo;
import org.example.demo.service.TodoService;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class TodoMainController implements Initializable, TodoCell.TodoCellCallback {


    @FXML
    private ListView<Todo> todoListView;
    @FXML
    private Button addTodoButton;
    @FXML
    private Button refreshButton;
    @FXML
    private ComboBox<String> filterComboBox;
    @FXML
    private TextField searchField;
    @FXML
    private Label totalCountLabel;
    @FXML
    private Label completedCountLabel;
    @FXML
    private Label pendingCountLabel;
    @FXML
    private Label overdueCountLabel;
    @FXML
    private VBox emptyStateContainer;

    private TodoService todoService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing TodoMainController");

        try {
            todoService = TodoService.getInstance();
            setupListView();
            setupFilters();
            setupSearch();
            updateStatistics();
            refreshTodoList();

            log.info("TodoMainController initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing TodoMainController", e);
            throw e;
        }
    }

    private void setupListView() {
        todoListView.setCellFactory(listView -> new TodoCell(this));
        todoListView.setPlaceholder(createEmptyStateView());
    }

    private void setupFilters() {
        filterComboBox.getItems().addAll("All", "Pending", "Completed", "Overdue");
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> refreshTodoList());
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshTodoList());
    }

    private VBox createEmptyStateView() {
        VBox emptyState = new VBox(10);
        emptyState.setStyle("-fx-alignment: center; -fx-padding: 50;");

        Label emptyLabel = new Label("No todos found");
        emptyLabel.setStyle("-fx-font-size: 18; -fx-text-fill: gray;");

        Label subtitleLabel = new Label("Click 'Add Todo' to create your first task");
        subtitleLabel.setStyle("-fx-font-size: 14; -fx-text-fill: lightgray;");

        emptyState.getChildren().addAll(emptyLabel, subtitleLabel);
        return emptyState;
    }

    @FXML
    private void handleAddTodo() {
        openTodoDialog(null);
    }

    @FXML
    private void handleRefresh() {
        refreshTodoList();
        updateStatistics();
    }

    @FXML
    private void handleTestNotification() {
        todoService.testNotification();
    }

    private void refreshTodoList() {
        var allTodos = todoService.getAllTodos();
        var filteredTodos = allTodos.filtered(todo -> {
            // Apply filter
            boolean matchesFilter = switch (filterComboBox.getValue()) {
                case "Pending" -> !todo.isCompleted();
                case "Completed" -> todo.isCompleted();
                case "Overdue" -> todo.isOverdue();
                default -> true; // "All"
            };

            // Apply search
            String searchText = searchField.getText().toLowerCase().trim();
            boolean matchesSearch = searchText.isEmpty() ||
                    todo.getTitle().toLowerCase().contains(searchText) ||
                    todo.getDescription().toLowerCase().contains(searchText);

            return matchesFilter && matchesSearch;
        });

        todoListView.setItems(filteredTodos);
        updateStatistics();
    }

    private void updateStatistics() {
        Platform.runLater(() -> {
            totalCountLabel.setText(String.valueOf(todoService.getTotalCount()));
            completedCountLabel.setText(String.valueOf(todoService.getCompletedCount()));
            pendingCountLabel.setText(String.valueOf(todoService.getPendingCount()));
            overdueCountLabel.setText(String.valueOf(todoService.getOverdueCount()));
        });
    }

    private void openTodoDialog(Todo todo) {
        log.info("Opening todo dialog for: {}", todo != null ? "editing " + todo.getTitle() : "new todo");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/demo/todo-dialog.fxml"));
            Parent root = loader.load();

            TodoDialogController controller = loader.getController();
            controller.setTodo(todo);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(todo == null ? "Add Todo" : "Edit Todo");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(addTodoButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                Todo result = controller.getTodo();
                try {
                    if (todo == null) {
                        todoService.addTodo(result);
                        log.info("Added new todo: {}", result.getTitle());
                    } else {
                        todoService.updateTodo(result);
                        log.info("Updated todo: {}", result.getTitle());
                    }
                    refreshTodoList();
                } catch (Exception e) {
                    log.error("Error saving todo: {}", e.getMessage(), e);
                    showErrorAlert("Save Error", "Failed to save todo: " + e.getMessage());
                }
            } else {
                log.debug("Dialog cancelled by user");
            }
        } catch (IOException e) {
            log.error("Error opening todo dialog", e);
            showErrorAlert("Error", "Could not open todo dialog: " + e.getMessage());
        }
    }

    @Override
    public void onEdit(Todo todo) {
        // Prevent editing completed todos
        if (todo.isCompleted()) {
            log.warn("Attempted to edit completed todo: {}", todo.getTitle());
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Edit Completed Todo");
            alert.setHeaderText("Todo is Completed");
            alert.setContentText("You cannot edit a completed todo. Please mark it as incomplete first if you need to make changes.");
            alert.showAndWait();
            return;
        }
        
        openTodoDialog(todo);
    }

    @Override
    public void onDelete(Todo todo) {
        log.info("Delete requested for todo: {}", todo.getTitle());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Todo");
        alert.setHeaderText("Delete Todo Item");
        alert.setContentText("Are you sure you want to delete \"" + todo.getTitle() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                todoService.deleteTodo(todo);
                log.info("Successfully deleted todo: {}", todo.getTitle());
                refreshTodoList();
            } catch (Exception e) {
                log.error("Error deleting todo: {}", todo.getTitle(), e);
                showErrorAlert("Delete Error", "Failed to delete todo: " + e.getMessage());
            }
        } else {
            log.debug("Delete cancelled by user for todo: {}", todo.getTitle());
        }
    }

    @Override
    public void onToggleComplete(Todo todo) {
        log.info("Toggling completion status for todo: {} (current: {})", todo.getTitle(), todo.isCompleted());

        try {
            todo.setCompleted(!todo.isCompleted());
            todoService.updateTodo(todo);
            log.info("Successfully toggled completion for todo: {} (new status: {})", todo.getTitle(), todo.isCompleted());
            refreshTodoList();
        } catch (Exception e) {
            log.error("Error updating todo completion status: {}", todo.getTitle(), e);
            showErrorAlert("Update Error", "Failed to update todo: " + e.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
