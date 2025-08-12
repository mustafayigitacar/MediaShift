package com.ffmpeg.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Canlı video kayıt ve segment yönetimi sınıfı
 */
public class LiveRecordingTask extends Task<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(LiveRecordingTask.class);
    
    private final String cameraDevice;
    private final String outputDirectory;
    private final LiveRecordingParams params;
    private final RecordingCallback callback;
    
    private Process ffmpegProcess;
    private Process ffmpegSegmentProcess; // Segment yazımı için ayrı process
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicLong recordingStartTime = new AtomicLong(0);
    private final AtomicLong pausedDuration = new AtomicLong(0);
    private final AtomicLong lastPauseTime = new AtomicLong(0);
    
    private final List<VideoSegment> recordedSegments = new ArrayList<>();
    private final Set<String> processedSegmentFiles = new HashSet<>(); // Yeni tracking
    private int currentSegmentIndex = 0;
    private String currentSegmentPath;
    private long segmentStartTime = 0;
    
    // Callback arayüzleri
    public interface RecordingCallback {
        void onRecordingStarted();
        void onRecordingPaused();
        void onRecordingResumed();
        void onRecordingStopped();
        void onSegmentCompleted(VideoSegment segment);
        void onRecordingError(String error);
        void onTimeUpdate(String formattedTime);
    }
    
    /**
     * Video segment bilgileri
     */
    public static class VideoSegment {
        private final String filePath;
        private final long duration; // milliseconds
        private final LocalDateTime createdAt;
        private final long fileSize;
        
        public VideoSegment(String filePath, long duration, LocalDateTime createdAt, long fileSize) {
            this.filePath = filePath;
            this.duration = duration;
            this.createdAt = createdAt;
            this.fileSize = fileSize;
        }
        
        public String getFilePath() { return filePath; }
        public long getDuration() { return duration; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public long getFileSize() { return fileSize; }
        
        public String getFileName() {
            return Paths.get(filePath).getFileName().toString();
        }
        
        public String getFormattedDuration() {
            long seconds = duration / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            
            return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
        }
        
        public String getFormattedFileSize() {
            if (fileSize < 1024) return fileSize + " B";
            int exp = (int) (Math.log(fileSize) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp-1) + "";
            return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
        }
        
        @Override
        public String toString() {
            return getFileName() + " (" + getFormattedDuration() + ", " + getFormattedFileSize() + ")";
        }
    }
    
    /**
     * Canlı kayıt parametreleri
     */
    public static class LiveRecordingParams {
        private final String format;
        private final String quality;
        private final int fps;
        private final int bitrate;
        private final int segmentDuration; // seconds
        
        public LiveRecordingParams(String format, String quality, int fps, int bitrate, int segmentDuration) {
            this.format = format;
            this.quality = quality;
            this.fps = fps;
            this.bitrate = bitrate;
            this.segmentDuration = segmentDuration;
        }
        
        public String getFormat() { return format; }
        public String getQuality() { return quality; }
        public int getFps() { return fps; }
        public int getBitrate() { return bitrate; }
        public int getSegmentDuration() { return segmentDuration; }
    }
    
    public LiveRecordingTask(String cameraDevice, String outputDirectory, 
                           LiveRecordingParams params, RecordingCallback callback) {
        this.cameraDevice = cameraDevice;
        this.outputDirectory = outputDirectory;
        this.params = params;
        this.callback = callback;
        
        // Segment tracking Setini temizle - yeni kayıt için
        this.processedSegmentFiles.clear();
    }
    
    @Override
    protected Void call() throws Exception {
        try {
            logger.info("Starting live recording task");
            startRecording();
            
            // HLS segmentation ile tek FFmpeg process - kamera donması önlenir
            startContinuousRecording();
            
            // Segment izleme döngüsü - dosya sistemi izlemesi
            long lastSegmentCheck = System.currentTimeMillis();
            int consecutiveEmptyChecks = 0;
            
            while (isRecording.get() && !isCancelled()) {
                // Cancellation kontrolü
                if (Thread.currentThread().isInterrupted() || isCancelled()) {
                    logger.info("Recording task cancelled");
                    break;
                }
                
                // FFmpeg process kontrolü
                if (ffmpegProcess != null && !ffmpegProcess.isAlive()) {
                    logger.warn("FFmpeg process died unexpectedly");
                    if (callback != null) {
                        Platform.runLater(() -> callback.onRecordingError("FFmpeg process terminated unexpectedly"));
                    }
                    break;
                }
                
                // Segment kontrolü - daha seyrek aralıklarla
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSegmentCheck > 5000) { // 5 saniyede bir kontrol (daha az sıklık)
                    int segmentsBefore = recordedSegments.size();
                    checkForNewSegments();
                    int segmentsAfter = recordedSegments.size();
                    
                    if (segmentsBefore == segmentsAfter) {
                        consecutiveEmptyChecks++;
                    } else {
                        consecutiveEmptyChecks = 0;
                    }
                    
                    // Çok uzun süre segment oluşmamışsa uyarı
                    if (consecutiveEmptyChecks > 10) { // 50 saniye segment yok
                        logger.warn("No new segments created for 50 seconds");
                        consecutiveEmptyChecks = 0; // Reset to avoid spam
                    }
                    
                    lastSegmentCheck = currentTime;
                }
                
                // Zaman güncellemesi
                updateRecordingTime();
                
                // Daha kısa sleep - daha responsive
                Thread.sleep(250); // 250ms kontrol aralığı
            }
            
        } catch (InterruptedException e) {
            logger.info("Live recording task interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error in live recording task", e);
            if (callback != null) {
                Platform.runLater(() -> callback.onRecordingError("Recording error: " + e.getMessage()));
            }
            throw e;
        } finally {
            stopRecording();
        }
        
        return null;
    }
    
    /**
     * Kaydı başlatır
     */
    private void startRecording() {
        try {
            // Çıkış klasörünü oluştur
            Path outputPath = Paths.get(outputDirectory);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
            
            // ESKİ SEGMENT DOSYALARINI TEMİZLE - segment explosionı önler
            cleanupOldSegmentFiles(outputPath);
            
            isRecording.set(true);
            recordingStartTime.set(System.currentTimeMillis());
            
            logger.info("Recording started to directory: {}", outputDirectory);
            
            if (callback != null) {
                Platform.runLater(() -> callback.onRecordingStarted());
            }
            
        } catch (Exception e) {
            logger.error("Error starting recording", e);
            throw new RuntimeException("Failed to start recording", e);
        }
    }
    
    /**
     * Eski segment dosyalarını temizler - segment explosionı önler
     */
    private void cleanupOldSegmentFiles(Path outputPath) {
        try {
            logger.info("Cleaning up old segment files before starting new recording...");
            
            int deletedCount = 0;
            if (Files.exists(outputPath)) {
                List<Path> oldSegments = Files.list(outputPath)
                    .filter(path -> path.toString().endsWith(".mp4"))
                    .filter(path -> path.getFileName().toString().contains("segment_"))
                    .collect(Collectors.toList());
                
                for (Path oldSegment : oldSegments) {
                    try {
                        Files.deleteIfExists(oldSegment);
                        deletedCount++;
                        logger.debug("Deleted old segment: {}", oldSegment.getFileName());
                    } catch (Exception e) {
                        logger.warn("Failed to delete old segment: {}", oldSegment.getFileName(), e);
                    }
                }
            }
            
            // Setleri de temizle
            processedSegmentFiles.clear();
            recordedSegments.clear();
            
            logger.info("Cleanup completed: {} old segments deleted", deletedCount);
            
        } catch (Exception e) {
            logger.warn("Error during segment cleanup", e);
        }
    }
    
    /**
     * Sürekli kayıt başlatır (HLS segmentation ile)
     */
    private void startContinuousRecording() {
        try {
            // Segment pattern
            String segmentPattern = Paths.get(outputDirectory, "segment_%03d.mp4").toString();
            
            // HLS segmentation ile FFmpeg komutunu oluştur (DUAL OUTPUT: MP4 + MJPEG)
            List<String> command = buildHLSSegmentCommand(segmentPattern);
            
            logger.info("FFmpeg command: {}", String.join(" ", command));
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // redirectErrorStream KALDIRILDI - ayrı error handling için
            
            ffmpegProcess = processBuilder.start();
            segmentStartTime = System.currentTimeMillis();
            
            // *** SAFE OUTPUT HANDLING - Anti-freeze ***
            // Non-blocking reader ile FFmpeg donmasını önleme
            BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()));
            
            // STDOUT okuma (non-blocking)
            CompletableFuture.runAsync(() -> {
                try {
                    String line;
                    while (isRecording.get() && !isCancelled()) {
                        if (reader.ready()) { // Non-blocking check
                            line = reader.readLine();
                            if (line != null) {
                                // Progress information parsing
                                if (line.startsWith("frame=")) {
                                    logger.debug("Progress: {}", line);
                                } else {
                                    logger.debug("FFmpeg: {}", line);
                                }
                            }
                        } else {
                            Thread.sleep(10); // Kısa bekleme
                        }
                        
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    if (isRecording.get() && !isCancelled()) {
                        logger.warn("Error reading FFmpeg output", e);
                    }
                } finally {
                    try { reader.close(); } catch (Exception ignored) {}
                }
            }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS));
            
            // STDERR okuma (hata mesajları)
            CompletableFuture.runAsync(() -> {
                try {
                    String line;
                    while (isRecording.get() && !isCancelled()) {
                        if (errorReader.ready()) { // Non-blocking check
                            line = errorReader.readLine();
                            if (line != null) {
                                // Hata tespiti
                                if (line.toLowerCase().contains("error") || 
                                    line.toLowerCase().contains("failed") ||
                                    line.toLowerCase().contains("could not")) {
                                    logger.warn("FFmpeg potential error: {}", line);
                                } else {
                                    logger.debug("FFmpeg stderr: {}", line);
                                }
                            }
                        } else {
                            Thread.sleep(10); // Kısa bekleme
                        }
                        
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    if (isRecording.get() && !isCancelled()) {
                        logger.warn("Error reading FFmpeg error output", e);
                    }
                } finally {
                    try { errorReader.close(); } catch (Exception ignored) {}
                }
            }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS));
            
        } catch (Exception e) {
            logger.error("Error starting continuous recording", e);
            throw new RuntimeException("Failed to start continuous recording", e);
        }
    }
    
    /**
     * Yeni segmentleri kontrol eder
     */
    private void checkForNewSegments() {
        try {
            // Segment klasöründeki dosyaları kontrol et
            Path outputPath = Paths.get(outputDirectory);
            if (!Files.exists(outputPath)) {
                return;
            }
            
            // Sadece çok yeni segment dosyalarını bul
            List<Path> newSegments = Files.list(outputPath)
                .filter(path -> path.toString().endsWith(".mp4"))
                .filter(path -> path.toString().contains("segment_"))
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    // Bu dosya daha önce işlendi mi?
                    boolean notProcessed = !processedSegmentFiles.contains(fileName);
                    return notProcessed;
                })
                .filter(path -> {
                    try {
                        // Dosya boyutu kontrol et - son segment küçük olabilir, minimum şartı düşür
                        long size = Files.size(path);
                        return size > 1024; // En az 1KB olmalı (son segment küçük olabilir)
                    } catch (Exception e) {
                        return false;
                    }
                })
                .limit(5) // Aynı anda max 5 segment işle - explosionı önler
                .collect(Collectors.toList());
                
            // Yeni segmentleri işle
            if (!newSegments.isEmpty()) {
                logger.debug("Found {} new segments to process", newSegments.size());
                for (Path segmentPath : newSegments) {
                    processNewSegment(segmentPath);
                }
            }
                
        } catch (Exception e) {
            logger.warn("Error checking for new segments", e);
        }
    }
    
    /**
     * Yeni segment işlemi
     */
    private void processNewSegment(Path segmentPath) {
        try {
            String fileName = segmentPath.getFileName().toString();
            
            // Thread-safe double-check: Bu segment zaten işlendi mi?
            synchronized (processedSegmentFiles) {
                if (processedSegmentFiles.contains(fileName)) {
                    return; // Zaten işlenmiş, atla
                }
                
                // Dosyayı işlenmiş olarak işaretle
                processedSegmentFiles.add(fileName);
            }
            
            long fileSize = Files.size(segmentPath);
            long duration = params.getSegmentDuration() * 1000L; // Approximate
            LocalDateTime createdAt = LocalDateTime.now();
            
            VideoSegment segment = new VideoSegment(segmentPath.toString(), duration, createdAt, fileSize);
            
            // Thread-safe segment list update
            synchronized (recordedSegments) {
                recordedSegments.add(segment);
            }
            
            logger.info("New segment processed: {} ({})", fileName, segment.getFormattedFileSize());
            
            if (callback != null) {
                Platform.runLater(() -> callback.onSegmentCompleted(segment));
            }
            
        } catch (Exception e) {
            logger.warn("Error processing new segment: " + segmentPath, e);
        }
    }
    
    /**
     * Yeni segment başlatır
     */
    private void startNewSegment() {
        try {
            // Önceki processin tamamen temizlendiğinden emin ol
            if (ffmpegProcess != null) {
                ffmpegProcess = null;
                // Ek güvenlik için kısa bir gecikme
                Thread.sleep(200);
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            // MP4 segment - birleştirme için optimize edilmiş format
            String segmentFileName = String.format("segment_%s_%03d.mp4", 
                timestamp, currentSegmentIndex);
            
            currentSegmentPath = Paths.get(outputDirectory, segmentFileName).toString();
            
            logger.info("Starting new segment: {}", currentSegmentPath);
            
            // FFmpeg komutu oluştur
            List<String> command = buildFFmpegCommand();
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            ffmpegProcess = processBuilder.start();
            
            // FFmpeg çıktısını logla
            BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
            CompletableFuture.runAsync(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null && isRecording.get()) {
                        logger.debug("FFmpeg: {}", line);
                    }
                } catch (Exception e) {
                    logger.warn("Error reading FFmpeg output", e);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error starting new segment", e);
            throw new RuntimeException("Failed to start new segment", e);
        }
    }
    
    /**
     * FFmpeg komutunu oluşturur
     */
    private List<String> buildFFmpegCommand() {
        List<String> command = new ArrayList<>();
        
        command.add("ffmpeg");
        command.add("-y"); // Dosya üzerine yaz
        
        // Platform-specific input ayarları
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows DirectShow - kamera cihazının daha güvenilir kullanımı için ek parametreler
            command.add("-f");
            command.add("dshow");
            command.add("-rtbufsize");
            command.add("32M"); // Orta boyut buffer - donmayı önler
            command.add("-thread_queue_size");
            command.add("512"); // Orta boyut queue
            command.add("-fflags");
            command.add("nobuffer"); // Bufferı minimum tut
            command.add("-flags");
            command.add("low_delay"); // Düşük gecikme modu
            command.add("-i");
            
            // Device ID formatını kontrol et - zaten video= prefixi varsa doğrudan kullan
            String deviceInput = cameraDevice;
            if (!deviceInput.startsWith("video=")) {
                deviceInput = "video=" + cameraDevice;
            }
            command.add(deviceInput);
        } else if (os.contains("mac")) {
            // macOS AVFoundation
            command.add("-f");
            command.add("avfoundation");
            command.add("-i");
            command.add(extractCameraIndex(cameraDevice));
        } else {
            // Linux Video4Linux
            command.add("-f");
            command.add("v4l2");
            command.add("-i");
            command.add(cameraDevice);
        }
        
        // Video encoder ayarları - MP4 optimizasyonu
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("fast"); // Daha iyi kalite için fast preset
        command.add("-tune");
        command.add("zerolatency"); // Düşük gecikme
        command.add("-profile:v");
        command.add("baseline"); // En uyumlu profil
        command.add("-level");
        command.add("3.1"); // Geniş cihaz uyumluluğu
        command.add("-pix_fmt");
        command.add("yuv420p"); // Standart pixel format
        
        // Kalite ayarları
        if (params.getQuality().equals("1080p")) {
            command.add("-s");
            command.add("1920x1080");
        } else if (params.getQuality().equals("720p")) {
            command.add("-s");
            command.add("1280x720");
        } else if (params.getQuality().equals("480p")) {
            command.add("-s");
            command.add("854x480");
        }
        
        // FPS ayarları
        command.add("-r");
        command.add(String.valueOf(params.getFps()));
        
        // Bitrate ayarları - CRF kullan (daha iyi kalite)
        command.add("-crf");
        command.add("23"); // Orta kalite
        command.add("-maxrate");
        command.add(params.getBitrate() + "k");
        command.add("-bufsize");
        command.add((params.getBitrate() * 2) + "k");
        
        // Audio ayarları
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");
        command.add("-ar");
        command.add("44100"); // Sample rate
        
        // MP4 optimizasyonu - concatenation için uyumlu segmentler oluştur
        command.add("-movflags");
        command.add("+faststart+frag_keyframe+empty_moov");
        
        // Hassas segment süresi - nokta ile decimal format (FFmpeg için)
        command.add("-t");
        command.add(String.valueOf(params.getSegmentDuration()));
        
        // Force keyframe interval - segment sınırlarında temiz kesim
        command.add("-g");
        command.add(String.valueOf(params.getFps())); // Her saniyede bir keyframe
        
        // Hassas zamanlama için
        command.add("-avoid_negative_ts");
        command.add("make_zero");
        
        // Çıkış dosyası
        command.add(currentSegmentPath);
        
        logger.info("FFmpeg command: {}", String.join(" ", command));
        
        return command;
    }
    
    /**
     * HLS segmentation için FFmpeg komutu oluşturur (tek process - kamera donmaz)
     */
    private List<String> buildHLSSegmentCommand(String segmentPattern) {
        List<String> command = new ArrayList<>();
        
        command.add("ffmpeg");
        command.add("-y"); // Dosya üzerine yaz
        
        // Platform-specific input ayarları
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows DirectShow - anti-freeze optimizations
            command.add("-f");
            command.add("dshow");
            command.add("-rtbufsize");
            command.add("8M"); // Çok küçük buffer - donmayı önler
            command.add("-thread_queue_size");
            command.add("64"); // Minimal queue - donma riski azalır
            command.add("-fflags");
            command.add("nobuffer+flush_packets"); // Agresif flush
            command.add("-flags");
            command.add("low_delay");
            command.add("-probesize");
            command.add("1M"); // Hızlı başlatma
            command.add("-analyzeduration");
            command.add("1M"); // Hızlı analiz
            command.add("-i");
            
            // Device ID formatını kontrol et - zaten video= prefixi varsa doğrudan kullan
            String deviceInput = cameraDevice;
            if (!deviceInput.startsWith("video=")) {
                deviceInput = "video=" + cameraDevice;
            }
            command.add(deviceInput);
        } else if (os.contains("mac")) {
            command.add("-f");
            command.add("avfoundation");
            command.add("-i");
            command.add(extractCameraIndex(cameraDevice));
        } else {
            command.add("-f");
            command.add("v4l2");
            command.add("-i");
            command.add(cameraDevice);
        }
        
        // Video encoder ayarları - hızlı encoding
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("ultrafast"); // En hızlı preset - donmayı önler
        command.add("-tune");
        command.add("zerolatency"); // Gecikme yok
        command.add("-profile:v");
        command.add("baseline");
        command.add("-level");
        command.add("3.1");
        command.add("-pix_fmt");
        command.add("yuv420p");
        
        // Kalite ayarları
        if (params.getQuality().equals("1080p")) {
            command.add("-s");
            command.add("1920x1080");
        } else if (params.getQuality().equals("720p")) {
            command.add("-s");
            command.add("1280x720");
        } else if (params.getQuality().equals("480p")) {
            command.add("-s");
            command.add("854x480");
        }
        
        command.add("-r");
        command.add(String.valueOf(params.getFps()));
        
        // Bitrate ayarları - sabit bitrate donmayı önler
        command.add("-b:v");
        command.add(params.getBitrate() + "k");
        command.add("-maxrate");
        command.add(params.getBitrate() + "k");
        command.add("-bufsize");
        command.add((params.getBitrate() / 2) + "k"); // Küçük buffer
        
        // Audio ayarları - basit
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("64k"); // Daha düşük audio bitrate
        command.add("-ar");
        command.add("22050"); // Daha düşük sample rate
        command.add("-ac");
        command.add("1"); // Mono ses - performans için
        
        // *** ÇİFT ÇIKTI SİSTEMİ - TEK FFMPEG PROSESİ ***
        // İlk çıktı: MP4 segmentleri (kayıt için)
        command.add("-f");
        command.add("segment");
        command.add("-segment_time");
        command.add(String.valueOf(params.getSegmentDuration()));
        command.add("-segment_format");
        command.add("mp4");
        command.add("-segment_list_type");
        command.add("flat");
        command.add("-reset_timestamps");
        command.add("1");
        command.add("-avoid_negative_ts");
        command.add("make_zero");
        
        // GOP ayarları - segmentlerde temiz kesim
        command.add("-g");
        command.add(String.valueOf(params.getFps() * 2)); // 2 saniye GOP
        command.add("-keyint_min");
        command.add(String.valueOf(params.getFps()));
        
        // Segmentlerde keyframe zorla
        command.add("-force_key_frames");
        command.add("expr:gte(t,n_forced*" + params.getSegmentDuration() + ")");
        
        // MP4 faststart - streaming için
        command.add("-movflags");
        command.add("+faststart+frag_keyframe");
        
        // Error resilience - donma durumunda otomatik devam
        command.add("-err_detect");
        command.add("ignore_err"); // Hataları yok say, devam et
        command.add("-reconnect");
        command.add("1");
        command.add("-reconnect_at_eof");
        command.add("1");
        command.add("-reconnect_delay_max");
        command.add("2");
        
        // MP4 segment çıktısı
        command.add(segmentPattern);
        
        // MJPEG dual output KALDIRILDI - donma riskini önlemek için
        // Sadece MP4 kayıt yapıyoruz, önizleme ayrı kamera previewdan gelecek
        
        return command;
    }
    
    /**
     * Kamera indeksini extract eder
     */
    private String extractCameraIndex(String deviceId) {
        if (deviceId.contains("=")) {
            String index = deviceId.substring(deviceId.lastIndexOf("=") + 1);
            try {
                Integer.parseInt(index);
                return index;
            } catch (NumberFormatException e) {
                return "0";
            }
        }
        return "0";
    }
    
    /**
     * Mevcut segmenti tamamlar
     */
    private void completeCurrentSegment() {
        try {
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                // FFmpeg procesini düzgün şekilde sonlandır - önce SIGTERM, sonra SIGKILL
                try {
                    ffmpegProcess.destroy();
                    boolean finished = ffmpegProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (!finished) {
                        ffmpegProcess.destroyForcibly();
                        ffmpegProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ffmpegProcess.destroyForcibly();
                }
            }
            
            // Processin tamamen temizlenmesi ve kamera cihazının serbest bırakılması için gecikme
            Thread.sleep(1500);
            
            // Segment dosyasının oluşup oluşmadığını kontrol et
            Path segmentPath = Paths.get(currentSegmentPath);
            if (Files.exists(segmentPath) && Files.size(segmentPath) > 0) {
                long fileSize = Files.size(segmentPath);
                
                // Gerçek segment süresini hesapla (approximate)
                long actualDuration = estimateSegmentDuration(segmentPath);
                LocalDateTime createdAt = LocalDateTime.now();
                
                VideoSegment segment = new VideoSegment(currentSegmentPath, actualDuration, createdAt, fileSize);
                recordedSegments.add(segment);
                
                logger.info("Segment completed: {} ({}ms, {})", 
                    segment.getFileName(), actualDuration, segment.getFormattedFileSize());
                
                if (callback != null) {
                    Platform.runLater(() -> callback.onSegmentCompleted(segment));
                }
                
                currentSegmentIndex++;
            } else {
                logger.warn("Segment file not created or empty: {}", currentSegmentPath);
            }
            
        } catch (Exception e) {
            logger.error("Error completing segment", e);
        }
    }
    
    /**
     * Segment süresini tahmin eder
     */
    private long estimateSegmentDuration(Path segmentPath) {
        try {
            // FFprobe ile gerçek süreyi al (eğer mümkünse)
            // Şimdilik configured durationı kullan
            return params.getSegmentDuration() * 1000L;
        } catch (Exception e) {
            logger.debug("Could not estimate segment duration, using configured value", e);
            return params.getSegmentDuration() * 1000L;
        }
    }
    
    /**
     * Kayıt süresini günceller
     */
    private void updateRecordingTime() {
        if (callback != null && recordingStartTime.get() > 0) {
            long currentTime = System.currentTimeMillis();
            long totalRecordingTime = currentTime - recordingStartTime.get() - pausedDuration.get();
            
            if (isPaused.get() && lastPauseTime.get() > 0) {
                totalRecordingTime -= (currentTime - lastPauseTime.get());
            }
            
            String formattedTime = formatTime(totalRecordingTime);
            Platform.runLater(() -> callback.onTimeUpdate(formattedTime));
        }
    }
    
    /**
     * Zamanı formatlar (HH:MM:SS)
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }
    
    /**
     * Kaydı duraklatır
     */
    public void pauseRecording() {
        if (isRecording.get() && !isPaused.get()) {
            isPaused.set(true);
            lastPauseTime.set(System.currentTimeMillis());
            
            // Mevcut FFmpeg procesini durdur
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                ffmpegProcess.destroy();
            }
            
            logger.info("Recording paused");
            
            if (callback != null) {
                Platform.runLater(() -> callback.onRecordingPaused());
            }
        }
    }
    
    /**
     * Kaydı devam ettirir
     */
    public void resumeRecording() {
        if (isRecording.get() && isPaused.get()) {
            if (lastPauseTime.get() > 0) {
                pausedDuration.addAndGet(System.currentTimeMillis() - lastPauseTime.get());
                lastPauseTime.set(0);
            }
            
            isPaused.set(false);
            
            logger.info("Recording resumed");
            
            if (callback != null) {
                Platform.runLater(() -> callback.onRecordingResumed());
            }
        }
    }
    
    /**
     * Kaydı durdurur
     */
    public void stopRecording() {
        logger.info("Stopping recording");
        
        isRecording.set(false);
        isPaused.set(false);
        
        // FFmpeg procesini güvenli şekilde sonlandır
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            try {
                // Önce streamleri kapat
                try {
                    if (ffmpegProcess.getOutputStream() != null) {
                        ffmpegProcess.getOutputStream().close();
                    }
                    if (ffmpegProcess.getInputStream() != null) {
                        ffmpegProcess.getInputStream().close();
                    }
                    if (ffmpegProcess.getErrorStream() != null) {
                        ffmpegProcess.getErrorStream().close();
                    }
                } catch (Exception e) {
                    logger.debug("Error closing FFmpeg streams", e);
                }
                
                // Graceful termination
                ffmpegProcess.destroy();
                boolean terminated = ffmpegProcess.waitFor(5, TimeUnit.SECONDS);
                
                if (!terminated || ffmpegProcess.isAlive()) {
                    logger.warn("FFmpeg process did not terminate gracefully, forcing termination");
                    ffmpegProcess.destroyForcibly();
                    ffmpegProcess.waitFor(3, TimeUnit.SECONDS);
                }
                
                logger.info("FFmpeg process terminated");
            } catch (Exception e) {
                logger.error("Error terminating FFmpeg process", e);
                try {
                    ffmpegProcess.destroyForcibly();
                } catch (Exception e2) {
                    logger.error("Error force-terminating FFmpeg process", e2);
                }
            }
            ffmpegProcess = null;
        }
        
        // Son segmentleri kontrol et ve işle
        try {
            // FFmpegin dosya yazımını tamamlaması için daha uzun bekle
            Thread.sleep(2000);
            
            // Birden fazla kez kontrol et çünkü son segment geç oluşabilir
            for (int i = 0; i < 3; i++) {
                checkForNewSegments();
                Thread.sleep(500); // Her kontrol arasında bekle
            }
            
            logger.info("Final segment check completed, total segments: {}", recordedSegments.size());
        } catch (Exception e) {
            logger.warn("Error during final segment check", e);
        }
        
        if (callback != null) {
            Platform.runLater(() -> callback.onRecordingStopped());
        }
    }
    
    /**
     * Kaydedilen segmentleri döndürür
     */
    public List<VideoSegment> getRecordedSegments() {
        return new ArrayList<>(recordedSegments);
    }
    
    /**
     * Kayıt durumunu kontrol eder
     */
    public boolean isRecording() {
        return isRecording.get();
    }
    
    /**
     * Duraklatma durumunu kontrol eder
     */
    public boolean isPaused() {
        return isPaused.get();
    }
    
    /**
     * Toplam kayıt süresini döndürür
     */
    public long getTotalRecordingTime() {
        if (recordingStartTime.get() == 0) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long totalTime = currentTime - recordingStartTime.get() - pausedDuration.get();
        
        if (isPaused.get() && lastPauseTime.get() > 0) {
            totalTime -= (currentTime - lastPauseTime.get());
        }
        
        return totalTime;
    }
}

