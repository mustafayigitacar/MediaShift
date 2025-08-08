package com.ffmpeg.gui;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class BatchProcessingTask extends Task<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessingTask.class);
    
    private final List<File> files;
    private final String outputDir;
    private final BatchSettings batchSettings;
    private final FFmpegService ffmpegService;
    private Instant startTime; // Batch işlem başlangıç zamanı
    private volatile boolean isRunning = false; // İşlem durumu
    
    public BatchProcessingTask(List<File> files, String outputDir, BatchSettings batchSettings) {
        this.files = files;
        this.outputDir = outputDir;
        this.batchSettings = batchSettings;
        this.ffmpegService = new FFmpegService();
    }
    
    @Override
    protected Void call() throws Exception {
        logger.info("Batch processing starting: {} files, output: {}", files.size(), outputDir);
        
        // Batch işlem başlangıç zamanını kaydet
        startTime = Instant.now();
        isRunning = true;
        
        // Elapsed time counter thread'ini başlat
        Thread elapsedTimeThread = new Thread(() -> {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(100); // 100ms aralıklarla güncelle
                    
                    if (isRunning) {
                        Duration elapsed = Duration.between(startTime, Instant.now());
                        double elapsedSeconds = elapsed.toMillis() / 1000.0;
                        
                        // Progress'i dosya sayısına göre hesapla (elapsed time'a göre tahmin)
                        double estimatedProgress = Math.min(elapsedSeconds / (files.size() * 30.0), 1.0); 
                        // Her dosya için ortalama 30 saniye varsayımı
                        String progressText = String.format("Batch işlem çalışıyor... %.1f saniye geçti (%%%.1f)", 
                            elapsedSeconds, estimatedProgress * 100);
                        
                        javafx.application.Platform.runLater(() -> {
                            updateProgress(estimatedProgress, 1.0);
                            updateMessage(progressText);
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        elapsedTimeThread.setDaemon(true);
        elapsedTimeThread.start();
        
        try {
            FFmpegService.BatchProcessingCallback callback = new FFmpegService.BatchProcessingCallback() {
                @Override
                public void onFileProgress(int currentFile, int totalFiles, double progress) {
                    double overallProgress = (currentFile + progress) / totalFiles;
                    
                    // Elapsed time'ı hesapla
                    Duration elapsed = Duration.between(startTime, Instant.now());
                    double elapsedSeconds = elapsed.toMillis() / 1000.0;
                    
                    String progressText = String.format("Dosya %d/%d işleniyor... %.1f%% (%.1f saniye geçti)", 
                        currentFile + 1, totalFiles, progress * 100, elapsedSeconds);
                    
                    javafx.application.Platform.runLater(() -> {
                        updateProgress(overallProgress, 1.0);
                        updateMessage(progressText);
                    });
                }
                
                @Override
                public void onFileCompleted(String fileName) {
                    Duration elapsed = Duration.between(startTime, Instant.now());
                    double elapsedSeconds = elapsed.toMillis() / 1000.0;
                    String message = String.format("Dosya tamamlandı: %s (%.1f saniye geçti)", fileName, elapsedSeconds);
                    updateMessage(message);
                }
                
                @Override
                public void onFileError(String fileName, String error) {
                    Duration elapsed = Duration.between(startTime, Instant.now());
                    double elapsedSeconds = elapsed.toMillis() / 1000.0;
                    String message = String.format("Dosya hatası: %s - %s (%.1f saniye geçti)", fileName, error, elapsedSeconds);
                    updateMessage(message);
                }
            };
            
            ffmpegService.processBatchFiles(files, outputDir, batchSettings, callback).get();
            
            // Batch işlem tamamlandı, counter'ı durdur
            isRunning = false;
            
            // Final elapsed time'ı hesapla
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            
            logger.info("Batch processing completed (Total time: {:.1f} seconds)", totalElapsedSeconds);
            
        } catch (Exception e) {
            // Hata durumunda da counter'ı durdur
            isRunning = false;
            logger.error("Batch işlem hatası", e);
            throw e;
        } finally {
            ffmpegService.shutdown();
        }
        
        return null;
    }
    
    @Override
    protected void succeeded() {
        super.succeeded();
        
        // Final elapsed time'ı göster
        if (startTime != null) {
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            String finalMessage = String.format("Batch işlem tamamlandı! (Toplam süre: %.1f saniye)", totalElapsedSeconds);
            updateMessage(finalMessage);
            updateProgress(1.0, 1.0);
        } else {
            updateMessage("Batch işlem tamamlandı!");
            updateProgress(1.0, 1.0);
        }
    }
    
    @Override
    protected void failed() {
        super.failed();
        
        // Hata durumunda da elapsed time'ı göster
        isRunning = false;
        if (startTime != null) {
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            String errorMessage = String.format("Batch işlem başarısız: %s (Geçen süre: %.1f saniye)", 
                getException().getMessage(), totalElapsedSeconds);
            updateMessage(errorMessage);
            updateProgress(0, 1.0);
        } else {
            updateMessage("Batch işlem başarısız: " + getException().getMessage());
            updateProgress(0, 1.0);
        }
    }
    
    @Override
    protected void cancelled() {
        super.cancelled();
        
        // İptal durumunda da elapsed time'ı göster
        isRunning = false;
        if (startTime != null) {
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            String cancelMessage = String.format("Batch işlem iptal edildi (Geçen süre: %.1f saniye)", totalElapsedSeconds);
            updateMessage(cancelMessage);
            updateProgress(0, 1.0);
        } else {
            updateMessage("Batch işlem iptal edildi");
            updateProgress(0, 1.0);
        }
    }
} 