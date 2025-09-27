package com.fjord.ide;

import com.fjord.ide.ui.EnhancedMainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FjordIDE extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Fjord IDE Enhanced - v0.2.0");

        EnhancedMainWindow mainWindow = new EnhancedMainWindow(primaryStage);
        Scene scene = new Scene(mainWindow.getRoot(), 1400, 900);

        // Apply theme
        mainWindow.getThemeManager().attachToScene(scene);

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // Start maximized for better experience
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}