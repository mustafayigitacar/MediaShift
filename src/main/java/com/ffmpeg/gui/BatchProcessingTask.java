package com.ffmpeg.gui;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchProcessingTask extends Task<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessingTask.class);
    
    private final List<File> files;
    private final String outputDir;
    private final BatchSettings batchSettings;
    private final FFmpegService ffmpegService;
    private Instant startTime; // Batch işlem başlangıç zamanı
    private volatile boolean isRunning = false; // İşlem durumu
    
    // Log callback interface
    public interface LogCallback {
        void onLog(String message);
    }
    
    private LogCallback logCallback;
    
    public BatchProcessingTask(List<File> files, String outputDir, BatchSettings batchSettings) {
        this.files = files;
        this.outputDir = outputDir;
        this.batchSettings = batchSettings;
        this.ffmpegService = new FFmpegService();
    }
    
    public void setLogCallback(LogCallback callback) {
        this.logCallback = callback;
    }
    
    private void log(String message) {
        if (logCallback != null) {
            javafx.application.Platform.runLater(() -> {
                logCallback.onLog(message);
            });
        }
        logger.info(message);
    }
    
    @Override
    protected Void call() throws Exception {
        log("Batch processing starting: " + files.size() + " files, output: " + outputDir);
        
        // Batch işlem başlangıç zamanını kaydet
        startTime = Instant.now();
        isRunning = true;
        
        // Başlangıç progress'ini ayarla
        updateProgress(0, 1.0);
        updateMessage("Batch işlem başlatılıyor...");
        
        try {
            final int totalFiles = files.size();
            final Instant finalStartTime = startTime;
            
            FFmpegService.BatchProcessingCallback callback = new FFmpegService.BatchProcessingCallback() {
                @Override
                public void onFileProgress(int currentFile, int totalFilesParam, double progress) {
                    // Progress hesaplaması: tamamlanan dosyalar + mevcut dosyanın progress'i
                    double completedFiles = Math.max(0, currentFile);
                    double currentFileProgress = Math.max(0, Math.min(1.0, progress));
                    double overallProgress = (completedFiles + currentFileProgress) / totalFiles;
                    
                    // Progress'i 0-1 arasında sınırla
                    overallProgress = Math.max(0, Math.min(1.0, overallProgress));
                    
                    // Elapsed time'ı hesapla
                    Duration elapsed = Duration.between(finalStartTime, Instant.now());
                    double elapsedSeconds = elapsed.toMillis() / 1000.0;
                    
                    final String progressText = String.format("Dosya %d/%d işleniyor... %.1f%% (%.1f saniye geçti)", 
                        currentFile + 1, totalFiles, currentFileProgress * 100, elapsedSeconds);
                    final double finalOverallProgress = overallProgress;
                    
                    javafx.application.Platform.runLater(() -> {
                        updateProgress(finalOverallProgress, 1.0);
                        updateMessage(progressText);
                    });
                }
                
                @Override
                public void onFileCompleted(String fileName) {
                    Duration elapsed = Duration.between(finalStartTime, Instant.now());
                    double elapsedSeconds = elapsed.toMillis() / 1000.0;
                    String message = String.format("Dosya tamamlandı: %s (%.1f saniye geçti)", fileName, elapsedSeconds);
                    log(message);
                    updateMessage(message);
                }
                
                @Override
                public void onFileError(String fileName, String error) {
                    Duration elapsed = Duration.between(finalStartTime, Instant.now());
                    double elapsedSeconds = elapsed.toMillis() / 1000.0;
                    String message = String.format("Dosya hatası: %s - %s (%.1f saniye geçti)", fileName, error, elapsedSeconds);
                    log("HATA: " + message);
                    updateMessage(message);
                }
            };
            
            log("FFmpegService batch processing başlatılıyor...");
            ffmpegService.processBatchFiles(files, outputDir, batchSettings, callback).get(120, java.util.concurrent.TimeUnit.MINUTES);
            
            // Batch işlem tamamlandı, counter'ı durdur
            isRunning = false;
            
            // Final elapsed time'ı hesapla
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            
            log("Batch processing completed (Total time: " + String.format("%.1f", totalElapsedSeconds) + " seconds)");
            
        } catch (java.util.concurrent.TimeoutException e) {
            // Timeout durumunda da counter'ı durdur
            isRunning = false;
            log("HATA: Batch işlem timeout (120 dakika)");
            logger.error("Batch işlem timeout (120 dakika)", e);
            throw new RuntimeException("Batch işlem timeout: 120 dakika içinde tamamlanamadı", e);
        } catch (Exception e) {
            // Hata durumunda da counter'ı durdur
            isRunning = false;
            log("HATA: Batch işlem hatası - " + e.getMessage());
            logger.error("Batch işlem hatası", e);
            throw e;
        } finally {
            log("FFmpegService kapatılıyor...");
            ffmpegService.shutdown();
        }
        
        return null;
    }
    
    @Override
    protected void succeeded() {
        super.succeeded();
        
        // İşlem durumunu durdur
        isRunning = false;
        
        // Final elapsed time'ı göster ve progress'i tamamla
        if (startTime != null) {
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            String finalMessage = String.format("Batch işlem tamamlandı! (Toplam süre: %.1f saniye)", totalElapsedSeconds);
            updateMessage(finalMessage);
            updateProgress(1.0, 1.0); // Progress'i tamamla
        } else {
            updateMessage("Batch işlem tamamlandı!");
            updateProgress(1.0, 1.0); // Progress'i tamamla
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