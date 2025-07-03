package org.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.demo.service.impl.TodoServiceImpl;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("todo-main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
        stage.setTitle("Advanced Todo List App");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        
        // Handle application shutdown
        stage.setOnCloseRequest(event -> {
            TodoServiceImpl.getInstance().shutdown();
        });
        
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}