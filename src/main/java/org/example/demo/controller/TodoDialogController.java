package org.example.demo.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.demo.model.Todo;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

@Slf4j
public class TodoDialogController implements Initializable {
    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<Todo.Priority> priorityComboBox;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private Spinner<Integer> hourSpinner;
    @FXML
    private Spinner<Integer> minuteSpinner;
    @FXML
    private Button okButton;
    @FXML
    private Button cancelButton;

    @Setter
    private Stage dialogStage;
    private Todo todo;
    @Getter
    private boolean okClicked = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing TodoDialogController");

        try {
            priorityComboBox.getItems().addAll(Todo.Priority.values());
            priorityComboBox.setValue(Todo.Priority.MEDIUM);

            // Setup time spinners with proper value factories
            SpinnerValueFactory<Integer> hourValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9);
            hourSpinner.setValueFactory(hourValueFactory);
            hourSpinner.setEditable(true);

            SpinnerValueFactory<Integer> minuteValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
            minuteSpinner.setValueFactory(minuteValueFactory);
            minuteSpinner.setEditable(true);

            // Set default due date to tomorrow
            dueDatePicker.setValue(LocalDate.now().plusDays(1));

            // Validation
            setupValidation();

            log.info("TodoDialogController initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing TodoDialogController", e);
            throw e;
        }
    }

    private void setupValidation() {
        okButton.disableProperty().bind(
                titleField.textProperty().isEmpty()
        );
    }

    public Todo getTodo() {
        log.info("Getting todo from form");

        try {
            // Get the values from the form
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();
            Todo.Priority priority = priorityComboBox.getValue();

            log.debug("Form values - Title: {}, Priority: {}", title, priority);

            // Set due date with time from spinners
            LocalDateTime dueDate = null;
            LocalDate date = dueDatePicker.getValue();
            if (date != null) {
                LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
                dueDate = LocalDateTime.of(date, time);
                log.debug("Due date set to: {}", dueDate);
            }

            if (todo == null) {
                // Create new toDoInfo with the form values
                todo = new Todo(title, description, priority, dueDate);
                log.info("Created new todo: {}", title);
            } else {
                // Update existing toDoInfo
                todo.setTitle(title);
                todo.setDescription(description);
                todo.setPriority(priority);
                todo.setDueDate(dueDate);
                log.info("Updated existing todo: {}", title);
            }

            return todo;
        } catch (Exception e) {
            log.error("Error getting todo from form", e);
            throw e;
        }
    }

    public void setTodo(Todo todo) {
        log.info("Setting todo for editing: {}", todo != null ? todo.getTitle() : "new todo");

        this.todo = todo;

        if (todo != null) {
            try {
                titleField.setText(todo.getTitle());
                descriptionArea.setText(todo.getDescription());
                priorityComboBox.setValue(todo.getPriority());

                if (todo.getDueDate() != null) {
                    dueDatePicker.setValue(todo.getDueDate().toLocalDate());
                    hourSpinner.getValueFactory().setValue(todo.getDueDate().getHour());
                    minuteSpinner.getValueFactory().setValue(todo.getDueDate().getMinute());
                    log.debug("Set due date: {}", todo.getDueDate());
                }

                log.info("Todo fields populated successfully");
            } catch (Exception e) {
                log.error("Error setting todo fields", e);
            }
        }
    }

    @FXML
    private void handleOk() {
        log.info("OK button clicked, validating input");

        if (isInputValid()) {
            log.info("Input validation passed, closing dialog");
            okClicked = true;
            dialogStage.close();
        } else {
            log.warn("Input validation failed");
        }
    }

    @FXML
    private void handleCancel() {
        log.info("Cancel button clicked, closing dialog without saving");
        dialogStage.close();
    }

    private boolean isInputValid() {
        log.debug("Validating input fields");
        String errorMessage = "";

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errorMessage += "Title is required!\n";
            log.warn("Validation failed: Title is empty");
        }

        if (titleField.getText() != null && titleField.getText().trim().length() > 100) {
            errorMessage += "Title must be less than 100 characters!\n";
            log.warn("Validation failed: Title too long ({})", titleField.getText().trim().length());
        }

        if (descriptionArea.getText() != null && descriptionArea.getText().trim().length() > 500) {
            errorMessage += "Description must be less than 500 characters!\n";
            log.warn("Validation failed: Description too long ({})", descriptionArea.getText().trim().length());
        }

        if (errorMessage.isEmpty()) {
            log.debug("Input validation passed");
            return true;
        } else {
            log.info("Input validation failed with errors: {}", errorMessage.trim());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Please correct the following errors:");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}
