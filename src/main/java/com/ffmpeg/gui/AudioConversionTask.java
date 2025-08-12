package com.ffmpeg.gui;

import javafx.concurrent.Task;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class AudioConversionTask extends Task<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioConversionTask.class);
    
    private final AudioConversionParams params;
    private final FFmpegService ffmpegService;
    private javafx.scene.control.ProgressBar logProgressBar;
    private javafx.scene.control.Label progressLabel;
    private double totalDuration = 0.0;                     // Audio toplam süresi
    private Instant startTime;                             // Dönüştürme başlangıç zamanı
    private volatile boolean isRunning = false;           // Dönüştürme durumu
    private volatile double actualProgress = 0.0;        // FFmpeg'den gelen gerçek progress
    
    public AudioConversionTask(String inputPath, String outputPath, String format, 
                             String codec, int bitrate, int sampleRate, int channels,
                             javafx.scene.control.ProgressBar logProgressBar, 
                             javafx.scene.control.Label progressLabel) {
        String updatedOutputPath = updateOutputPathForFormat(outputPath, format);
        
        this.params = new AudioConversionParams(inputPath, updatedOutputPath, format, codec, bitrate, sampleRate, channels);
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
        logger.info("Audio conversion starting: {}", params);
        
        try {
            // Audio süresini al
            try {
                FFmpegProbeResult probeResult = ffmpegService.videoBilgisiAl(params.getInputPath());
                if (probeResult != null && probeResult.getStreams() != null && !probeResult.getStreams().isEmpty()) {
                    // Audio stream'ini bul
                    for (FFmpegStream stream : probeResult.getStreams()) {
                        if (stream.codec_type == FFmpegStream.CodecType.AUDIO) {
                            totalDuration = stream.duration;
                            logger.info("Audio duration: {} seconds", totalDuration);
                            break;
                        }
                    }
                    // Eğer audio stream bulunamazsa, ilk streami kullan
                    if (totalDuration == 0.0 && !probeResult.getStreams().isEmpty()) {
                        totalDuration = probeResult.getStreams().get(0).duration;
                        logger.info("Audio stream not found, using first stream duration: {} seconds", totalDuration);
                    }
                }
            } catch (Exception e) {
                logger.warn("Audio duration could not be determined, using default 60 seconds: {}", e.getMessage());
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
                            String progressText = String.format("Audio dönüştürülüyor... %.1f saniye geçti (%%%.1f)", 
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
                    // FFmpeg'den gelen gerçek progressi kaydet
                    if (totalDuration > 0) {
                        actualProgress = Math.min(currentTime / totalDuration, 1.0);
                    } else {
                        actualProgress = Math.min(currentTime / 60.0, 1.0);
                    }
                }
            };
            
            ffmpegService.convertAudio(params, callback).get();
        
            // Dönüştürme tamamlandı, counterı durdur
            isRunning = false;
            actualProgress = 1.0; // Tamamlandığında %100 göster
            
            // Final elapsed time'ı hesapla
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            
            logger.info("Audio conversion completed: {} (Total time: {:.1f} seconds)", params.getOutputPath(), totalElapsedSeconds);
            
        } catch (Exception e) {
            // Hata durumunda da counterı durdur
            isRunning = false;
            logger.error("Audio conversion failed", e);
            throw e;
        } finally {
            ffmpegService.shutdown();
        }
        
        return null;
    }
    
    @Override
    protected void succeeded() {
        super.succeeded();
        
        // Final elapsed time göster
        if (startTime != null) {
            Duration totalElapsed = Duration.between(startTime, Instant.now());
            double totalElapsedSeconds = totalElapsed.toMillis() / 1000.0;
            String finalMessage = String.format("Audio dönüştürme tamamlandı! (Toplam süre: %.1f saniye)", totalElapsedSeconds);
            
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
                progressLabel.setText("Audio dönüştürme tamamlandı!");
            }
            updateMessage("Audio dönüştürme tamamlandı!");
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
            String errorMessage = String.format("Audio dönüştürme başarısız: %s (Geçen süre: %.1f saniye)", 
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
                progressLabel.setText("Audio dönüştürme başarısız: " + getException().getMessage());
            }
            updateMessage("Audio dönüştürme başarısız: " + getException().getMessage());
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
            String cancelMessage = String.format("Audio dönüştürme iptal edildi (Geçen süre: %.1f saniye)", totalElapsedSeconds);
            
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
                progressLabel.setText("Audio dönüştürme iptal edildi");
            }
            updateMessage("Audio dönüştürme iptal edildi");
        }
    }
} 