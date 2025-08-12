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
            logger.info("MediaShift application starting...");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Parent root = loader.load();
            
            controller = loader.getController();
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
            
            // Window close event handler - kamera kapanmasını garantile
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Application close requested, shutting down services...");
                
                // Controllerı kapat
                if (controller != null) {
                    controller.shutdown();
                }
                
                // Platformu kapat
                javafx.application.Platform.exit();
                
                logger.info("Application shutdown completed");
            });
            
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            primaryStage.setMaximized(true);
            primaryStage.show();
            
            logger.info("MediaShift application started successfully");
            
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
        logger.info("MediaShift application shutting down...");
        
        // Controllerı kapat
        if (controller != null) {
            controller.shutdown();
        }
    }
    
    private MainWindowController controller;
    
    public static void main(String[] args) {
        // JVM shutdown hook ekle - ani kapanma durumunda cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM shutdown hook triggered - performing emergency cleanup");
            
            // Emergency cleanup - tüm FFmpeg processlerini sonlandır
            try {
                // Platform bağımsız process termination
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                
                if (os.contains("win")) {
                    // Windowsta tüm ffmpeg processlerini sonlandır
                    pb = new ProcessBuilder("taskkill", "/F", "/IM", "ffmpeg.exe");
                } else {
                    // Unix-like sistemlerde
                    pb = new ProcessBuilder("pkill", "-f", "ffmpeg");
                }
                
                Process cleanup = pb.start();
                boolean finished = cleanup.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                
                if (!finished) {
                    cleanup.destroyForcibly();
                }
                
                logger.info("Emergency FFmpeg cleanup completed");
                
            } catch (Exception e) {
                logger.warn("Error during emergency cleanup", e);
            }
        }, "MediaShift-Emergency-Cleanup"));
        
        launch(args);
    }
} 