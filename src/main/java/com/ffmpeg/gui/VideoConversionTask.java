package com.ffmpeg.gui;

import javafx.concurrent.Task;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class VideoConversionTask extends Task<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoConversionTask.class);
    
    private final VideoConversionParams params;
    private final FFmpegService ffmpegService;
    private javafx.scene.control.ProgressBar logProgressBar;
    private javafx.scene.control.Label progressLabel;
    private double totalDuration = 0.0; // Video toplam süresi
    private Instant startTime; // Dönüştürme başlangıç zamanı
    private volatile boolean isRunning = false; // Dönüştürme durumu
    private volatile double actualProgress = 0.0; // FFmpegden gelen gerçek progress
    
    public VideoConversionTask(String inputPath, String outputPath, String format, 
                             String codec, int bitrate, int width, int height, double fps,
                             javafx.scene.control.ProgressBar logProgressBar, 
                             javafx.scene.control.Label progressLabel) {
        String updatedOutputPath = updateOutputPathForFormat(outputPath, format);
        
        this.params = new VideoConversionParams(inputPath, updatedOutputPath, format, codec, bitrate, width, height, fps);
        this.ffmpegService = new FFmpegService();
        this.logProgressBar = logProgressBar;
        this.progressLabel = progressLabel;
    }
    
    private String updateOutputPathForFormat(String outputPath, String format) {
        if (format == null || format.isEmpty()) {
            return outputPath;
        }
        
        String formatLower = format.toLowerCase();
        String basePath = outputPath;
        
        if (basePath.contains(".")) {
            basePath = basePath.substring(0, basePath.lastIndexOf('.'));
        }
        
        return basePath + "." + formatLower;
    }
    
    @Override
    protected Void call() throws Exception {
        logger.info("Video conversion starting: {}", params);
        
        try {
            // Video süresini al (basitleştirilmiş)
            try {
                FFmpegProbeResult probeResult = ffmpegService.videoBilgisiAl(params.getInputPath());
                if (probeResult != null && probeResult.getStreams() != null && !probeResult.getStreams().isEmpty()) {
                    FFmpegStream stream = probeResult.getStreams().get(0);
                    totalDuration = stream.duration;
                    logger.info("Video duration: {} seconds", totalDuration);
                }
            } catch (Exception e) {
                logger.warn("Video duration could not be retrieved, using default 60 seconds: {}", e.getMessage());
                totalDuration = 60.0;
            }
            
            // Dönüştürme başlangıç zamanını kaydet
            startTime = Instant.now();
            isRunning = true;
            
            // Elapsed time counter threadini başlat
            Thread elapsedTimeThread = new Thread(() -> {
                while (isRunning && !Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(100); // 100ms aralıklarla güncelle
                        
                        if (isRunning) {
                            Duration elapsed = Duration.between(startTime, Instant.now());
                            double elapsedSeconds = elapsed.toMillis() / 1000.0;
                            
                            // Gerçek FFmpeg progressini kullan, elapsed time sadece süre için
                            String progressText = String.format("Video dönüştürülüyor... %.1f saniye geçti (%%%.1f)", 
                                elapsedSeconds, actualProgress * 100);
                            
                            javafx.application.Platform.runLater(() -> {
                                updateProgress(actualProgress, 1.0);
                                updateMessage(progressText);
                                
                                if (logProgressBar != null) {
                                    logProgressBar.setProgress(actualProgress);
                                }
                                if (progressLabel != null) {
                                    progressLabel.setText(progressText);
                                }
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
            
            FFmpegService.ProgressCallback callback = new FFmpegService.ProgressCallback() {
                @Override
                public void onProgress(double currentTime) {
                    // FFmpegden gelen gerçek progressi kaydet
                    if (totalDuration > 0) {
                        actualProgress = Math.min(currentTime / totalDuration, 1.0);
                    } else {
                        actualProgress = Math.min(currentTime / 60.0, 1.0);
                    }
                }
            };
            
            ffmpegService.convertVideo(params, callback).get();
            
            // Dönüştürme tamamlandı, counterı durdur
            isRunning = false;
            actualProgress = 1.0; // Tamamlandığında %100 göster
            
            // Final elapsed time hesapla
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            
            logger.info("Video conversion completed: {} (Total time: {:.1f} seconds)", params.getOutputPath(), totalElapsedSeconds);
            
        } catch (Exception e) {
            // Hata durumunda da counterı durdur
            isRunning = false;
            logger.error("Video conversion error", e);
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
            String finalMessage = String.format("Video dönüştürme tamamlandı! (Toplam süre: %.1f saniye)", totalElapsedSeconds);
            
            if (logProgressBar != null) {
                logProgressBar.setProgress(1.0);
            }
            if (progressLabel != null) {
                progressLabel.setText(finalMessage);
            }
            updateMessage(finalMessage);
        } else {
            if (logProgressBar != null) {
                logProgressBar.setProgress(1.0);
            }
            if (progressLabel != null) {
                progressLabel.setText("Video dönüştürme tamamlandı!");
            }
            updateMessage("Video dönüştürme tamamlandı!");
        }
    }
    
    @Override
    protected void failed() {
        super.failed();
        
        // Hata durumunda da elapsed time göster
        isRunning = false;
        if (startTime != null) {
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            String errorMessage = String.format("Video dönüştürme başarısız: %s (Geçen süre: %.1f saniye)", 
                getException().getMessage(), totalElapsedSeconds);
            
            if (logProgressBar != null) {
                logProgressBar.setProgress(0.0);
            }
            if (progressLabel != null) {
                progressLabel.setText(errorMessage);
            }
            updateMessage(errorMessage);
        } else {
            if (logProgressBar != null) {
                logProgressBar.setProgress(0.0);
            }
            if (progressLabel != null) {
                progressLabel.setText("Video dönüştürme başarısız: " + getException().getMessage());
            }
            updateMessage("Video dönüştürme başarısız: " + getException().getMessage());
        }
    }
    
    @Override
    protected void cancelled() {
        super.cancelled();
        
        // İptal durumunda da elapsed time göster
        isRunning = false;
        if (startTime != null) {
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            String cancelMessage = String.format("Video dönüştürme iptal edildi (Geçen süre: %.1f saniye)", totalElapsedSeconds);
            
            if (logProgressBar != null) {
                logProgressBar.setProgress(0.0);
            }
            if (progressLabel != null) {
                progressLabel.setText(cancelMessage);
            }
            updateMessage(cancelMessage);
        } else {
            if (logProgressBar != null) {
                logProgressBar.setProgress(0.0);
            }
            if (progressLabel != null) {
                progressLabel.setText("Video dönüştürme iptal edildi");
            }
            updateMessage("Video dönüştürme iptal edildi");
        }
    }
} 