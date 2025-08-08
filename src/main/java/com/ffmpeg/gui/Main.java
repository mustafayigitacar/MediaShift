package com.ffmpeg.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class Main extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("FFmpeg GUI application starting...");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Parent root = loader.load();
            
            MainWindowController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            primaryStage.setTitle("MediaShift - Professional Media Converter");
            
            try {
                InputStream iconStream = getClass().getResourceAsStream("/images/javafx-icon.png");
                if (iconStream != null) {
                    Image icon = new Image(iconStream);
                    primaryStage.getIcons().add(icon);
                    logger.info("Application icon loaded successfully");
                } else {
                    logger.warn("Application icon not found: /images/javafx-icon.png");
                }
            } catch (Exception e) {
                logger.warn("Error loading application icon", e);
            }
            
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            primaryStage.setMaximized(true);
            primaryStage.show();
            
            logger.info("FFmpeg GUI application started successfully");
            
        } catch (IOException e) {
            logger.error("Error loading FXML file", e);
            hataGoster("Error", "Application failed to start", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during application startup", e);
            hataGoster("Critical Error", "Application failed to start", e.getMessage());
        }
    }
    
    private void hataGoster(String title, String header, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    @Override
    public void stop() {
        logger.info("FFmpeg GUI application shutting down...");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 