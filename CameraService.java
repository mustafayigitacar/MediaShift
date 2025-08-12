package com.ffmpeg.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeUnit;

/**
 * Kamera servis sınıfı
 */
public class CameraService {
    
    private static final Logger logger = LoggerFactory.getLogger(CameraService.class);
    
    private FFmpegService ffmpegService;
    private ObservableList<CameraDevice> availableCameras;
    
    // Process yönetimi
    private Process ffmpegProcess;
    private Process recordingProcess;
    private Thread frameReaderThread;
    private final Object processLock = new Object();
    
    // Durum değişkenleri
    private volatile boolean isPreviewActive = false;
    private volatile boolean isRecording = false;
    private String currentCameraDevice;
    private String currentCameraDeviceId;
    private String recordingOutputDir;
    private String currentRecordingPath;
    
    // Callbacks
    private PreviewCallback previewCallback;
    private RecordingCallback recordingCallback;
    
    public CameraService(FFmpegService ffmpegService) {
        this.ffmpegService = ffmpegService;
        this.availableCameras = FXCollections.observableArrayList();
        this.recordingOutputDir = System.getProperty("user.home") + "/Desktop/MediaShift_Recordings";
    }
    
    public CompletableFuture<List<CameraDevice>> discoverCameras() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Discovering cameras...");
            List<CameraDevice> cameras = new ArrayList<>();
            
            // FFmpegService null kontrolü
            if (ffmpegService == null) {
                logger.error("FFmpegService is null - cannot discover cameras");
                cameras.add(new CameraDevice("Default Camera", "video=0", "Default Video Device"));
                availableCameras.clear();
                availableCameras.addAll(cameras);
                return cameras;
            }
            
            try {
                String ffmpegPath = ffmpegService.getFfmpegPath();
                logger.info("Using FFmpeg path: {}", ffmpegPath);
                
                ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath, "-f", "dshow", "-list_devices", "true", "-i", "dummy"
                );
                pb.redirectErrorStream(true);
                
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
                String line;
                boolean inVideoSection = false;
                StringBuilder ffmpegOutput = new StringBuilder();
                
                while ((line = reader.readLine()) != null) {
                    ffmpegOutput.append(line).append("\n");
                    logger.debug("FFmpeg: {}", line);
                    
                    // DirectShow device linesı direkt ara - section headera bakmadan
                    if (line.contains("[dshow @") && line.contains("\"") && line.contains("(video)")) {
                        logger.info("Found DirectShow video device line: {}", line);
                        
                        try {
                            int startQuote = line.indexOf("\"");
                            int endQuote = line.indexOf("\"", startQuote + 1);
                            
                            if (startQuote != -1 && endQuote != -1) {
                                String deviceName = line.substring(startQuote + 1, endQuote);
                                logger.info("Extracted device name: '{}'", deviceName);
                                
                                // Boş isim kontrolü
                                if (!deviceName.trim().isEmpty()) {
                                    String deviceId = "video=" + deviceName;
                                    String description = "DirectShow Video Device";
                                    
                                    CameraDevice camera = new CameraDevice(deviceName, deviceId, description);
                                    cameras.add(camera);
                                    
                                    logger.info("✓ FOUND CAMERA: {} with deviceId: {}", deviceName, deviceId);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Error parsing camera line: {}", line, e);
                        }
                    }
                    
                    // Video devices section başlangıcı - daha esnek kontrolle (legacy approach)
                    if (line.toLowerCase().contains("video devices") || 
                        line.toLowerCase().contains("directshow video")) {
                        inVideoSection = true;
                        logger.info("Found video devices section: {}", line);
                        continue;
                    }
                    
                    // Audio devices section başlangıcı - video sectionından çık
                    if (line.toLowerCase().contains("audio devices") || 
                        line.toLowerCase().contains("directshow audio")) {
                        inVideoSection = false;
                        logger.info("Exiting video section, found audio section: {}", line);
                        continue;
                    }
                    
                    // Legacy section-based parsing
                    if (inVideoSection && line.contains("\"")) {
                        logger.info("Processing potential device line (section-based): {}", line);
                        
                        try {
                            int startQuote = line.indexOf("\"");
                            int endQuote = line.indexOf("\"", startQuote + 1);
                            
                            if (startQuote != -1 && endQuote != -1) {
                                String deviceName = line.substring(startQuote + 1, endQuote);
                                logger.info("Extracted device name (section-based): '{}'", deviceName);
                                
                                // Check if this is a video device (not audio)
                                String afterQuote = line.substring(endQuote + 1);
                                boolean isVideoDevice = afterQuote.contains("(video)");
                                logger.info("After quote: '{}', isVideoDevice: {}", afterQuote, isVideoDevice);
                                
                                // Boş isim kontrolü ve video device olması
                                if (!deviceName.trim().isEmpty() && isVideoDevice) {
                                    // Duplicate check
                                    boolean alreadyAdded = cameras.stream()
                                        .anyMatch(cam -> cam.getName().equals(deviceName));
                                    
                                    if (!alreadyAdded) {
                                        String deviceId = "video=" + deviceName;
                                        String description = "DirectShow Video Device";
                                        
                                        CameraDevice camera = new CameraDevice(deviceName, deviceId, description);
                                        cameras.add(camera);
                                        
                                        logger.info("✓ FOUND CAMERA (section-based): {} with deviceId: {}", deviceName, deviceId);
                                    }
                                } else {
                                    logger.info("Skipping device: name='{}', isEmpty={}, isVideoDevice={}", 
                                        deviceName, deviceName.trim().isEmpty(), isVideoDevice);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Error parsing camera line: {}", line, e);
                        }
                    }
                }
                
                int exitCode = process.waitFor();
                reader.close();
                
                logger.info("FFmpeg exit code: {}", exitCode);
                if (exitCode != 0) {
                    logger.warn("FFmpeg returned non-zero exit code, but this is expected for list_devices");
                }
                
            } catch (Exception e) {
                logger.error("Error discovering cameras with DirectShow", e);
                
                // Fallback: Try alternative camera detection methods
                logger.info("Trying alternative camera detection methods...");
                cameras.addAll(tryAlternativeCameraDetection());
            }
            
            // Eğer hiç kamera bulunamadıysa, varsayılan kameraları ekle
            if (cameras.isEmpty()) {
                logger.info("No cameras found, adding default cameras");
                cameras.add(new CameraDevice("Default Camera", "video=0", "Default Video Device (Index 0)"));
                cameras.add(new CameraDevice("USB Camera", "video=USB2.0 HD UVC WebCam", "Common USB Camera"));
                cameras.add(new CameraDevice("Integrated Camera", "video=Integrated Camera", "Built-in Camera"));
            }
            
            availableCameras.clear();
            availableCameras.addAll(cameras);
            
            logger.info("Found {} cameras total", cameras.size());
            return cameras;
        });
    }
    
    /**
     * Alternative kamera algılama yöntemleri
     */
    private List<CameraDevice> tryAlternativeCameraDetection() {
        List<CameraDevice> cameras = new ArrayList<>();
        
        try {
            // Method 1: Try common camera indices
            for (int i = 0; i < 5; i++) {
                try {
                    String testCommand = ffmpegService.getFfmpegPath();
                    ProcessBuilder testPb = new ProcessBuilder(
                        testCommand, "-f", "dshow", "-i", "video=" + i, "-t", "0.1", "-f", "null", "-"
                    );
                    testPb.redirectErrorStream(true);
                    
                    Process testProcess = testPb.start();
                    boolean finished = testProcess.waitFor(3, TimeUnit.SECONDS);
                    
                    if (finished && testProcess.exitValue() == 0) {
                        cameras.add(new CameraDevice("Camera " + i, "video=" + i, "Camera Device Index " + i));
                        logger.info("Found camera at index {}", i);
                    }
                    
                    if (testProcess.isAlive()) {
                        testProcess.destroyForcibly();
                    }
                    
                } catch (Exception e) {
                    logger.debug("Camera index {} not available", i);
                }
            }
            
            // Method 2: Try common camera names
            String[] commonCameraNames = {
                "USB2.0 HD UVC WebCam",
                "Integrated Camera", 
                "USB Camera",
                "HD WebCam",
                "FaceTime HD Camera",
                "Microsoft Camera"
            };
            
            for (String cameraName : commonCameraNames) {
                try {
                    String testCommand = ffmpegService.getFfmpegPath();
                    ProcessBuilder testPb = new ProcessBuilder(
                        testCommand, "-f", "dshow", "-i", "video=" + cameraName, "-t", "0.1", "-f", "null", "-"
                    );
                    testPb.redirectErrorStream(true);
                    
                    Process testProcess = testPb.start();
                    boolean finished = testProcess.waitFor(3, TimeUnit.SECONDS);
                    
                    if (finished && testProcess.exitValue() == 0) {
                        // Bu kameranın daha önce eklenmediğinden emin ol
                        boolean alreadyExists = cameras.stream()
                            .anyMatch(cam -> cam.getName().equals(cameraName));
                        
                        if (!alreadyExists) {
                            cameras.add(new CameraDevice(cameraName, "video=" + cameraName, "Common Camera Device"));
                            logger.info("Found camera by name: {}", cameraName);
                        }
                    }
                    
                    if (testProcess.isAlive()) {
                        testProcess.destroyForcibly();
                    }
                    
                } catch (Exception e) {
                    logger.debug("Camera name '{}' not available", cameraName);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in alternative camera detection", e);
        }
        
        return cameras;
    }
    
    public boolean startPreview(CameraDevice camera, PreviewCallback callback) {
        if (isPreviewActive) {
            logger.warn("Preview is already active");
            return false;
        }
        
        logger.info("Starting camera preview: {}", camera.getName());
        
        this.previewCallback = callback;
        this.currentCameraDevice = camera.getName();
        this.currentCameraDeviceId = camera.getDeviceId();
        
        try {
            startMjpegPreview(camera);
            return true;
        } catch (Exception e) {
            logger.error("Failed to start preview", e);
            if (callback != null) {
                callback.onPreviewError("Önizleme başlatılamadı: " + e.getMessage());
            }
            return false;
        }
    }
    
    private void startMjpegPreview(CameraDevice camera) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(ffmpegService.getFfmpegPath());
        command.add("-f");
        command.add("dshow");
        
        // DirectShow buffer ayarları - stream stabilitesi için
        command.add("-rtbufsize");
        command.add("100M");
        command.add("-thread_queue_size");
        command.add("1024");
        
        // Video input
        command.add("-i");
        String cleanDeviceId = camera.getDeviceId();
        if (cleanDeviceId.startsWith("video=")) {
            cleanDeviceId = cleanDeviceId.substring(6);
        }
        command.add("video=" + cleanDeviceId);
        
        // Video encoding settings - daha stabil stream için
        command.add("-f");
        command.add("mjpeg");
        command.add("-pix_fmt");
        command.add("yuv420p");
        
        // Video filter - aspect ratio korunarak resize
        command.add("-vf");
        command.add("scale=640:480:force_original_aspect_ratio=decrease,pad=640:480:(ow-iw)/2:(oh-ih)/2");
        
        // Frame rate ve kalite ayarları
        command.add("-r");
        command.add("20");  // 15'den 20'ye çıkardık
        command.add("-q:v");
        command.add("3");   // 2'den 3'e çıkardık (biraz daha düşük kalite ama stabil)
        
        // Buffer ayarları
        command.add("-bufsize");
        command.add("2M");
        command.add("-maxrate");
        command.add("2M");
        
        // Output
        command.add("-");
        
        logger.info("FFmpeg command: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        synchronized (processLock) {
            ffmpegProcess = pb.start();
            Thread.sleep(1000);
            
            if (!ffmpegProcess.isAlive()) {
                throw new IOException("FFmpeg process failed to start");
            }
            
            isPreviewActive = true;
            
            frameReaderThread = new Thread(() -> {
                logger.info("Frame reader thread started");
                try (BufferedInputStream inputStream = new BufferedInputStream(ffmpegProcess.getInputStream(), 32768)) {
                    
                    ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
                    byte[] buffer = new byte[16384]; // Büyük buffer
                    int bytesRead;
                    boolean inFrame = false;
                    int frameCount = 0; // Frame sayacı - ilk birkaç framei atlamak için
                    
                    while (isPreviewActive && !Thread.currentThread().isInterrupted() && 
                           (bytesRead = inputStream.read(buffer)) != -1) {
                        
                        if (bytesRead > 0) {
                            // MJPEG frame boundary detection
                            for (int i = 0; i < bytesRead; i++) {
                                frameBuffer.write(buffer[i]);
                                
                                // JPEG start marker (FF D8)
                                if (!inFrame && frameBuffer.size() >= 2) {
                                    byte[] lastTwo = frameBuffer.toByteArray();
                                    int len = lastTwo.length;
                                    if (len >= 2 && lastTwo[len-2] == (byte)0xFF && lastTwo[len-1] == (byte)0xD8) {
                                        inFrame = true;
                                        frameBuffer.reset();
                                        frameBuffer.write((byte)0xFF);
                                        frameBuffer.write((byte)0xD8);
                                    }
                                }
                                
                                // JPEG end marker (FF D9)
                                if (inFrame && frameBuffer.size() >= 2) {
                                    byte[] frameBytes = frameBuffer.toByteArray();
                                    int len = frameBytes.length;
                                    if (len >= 2 && frameBytes[len-2] == (byte)0xFF && frameBytes[len-1] == (byte)0xD9) {
                                        // Complete frame found
                                        frameCount++;
                                        
                                        // İlk 3 framei atla (genellikle bozuk olur)
                                        if (frameCount > 3) {
                                            final byte[] completeFrame = frameBytes.clone();
                                            inFrame = false;
                                            frameBuffer.reset();
                                            
                                            if (previewCallback != null && completeFrame.length > 200) { // Minimum frame size check (200 byte)
                                                Platform.runLater(() -> {
                                                    try {
                                                        previewCallback.onFrameReceived(completeFrame);
                                                    } catch (Exception e) {
                                                        logger.warn("Error processing frame", e);
                                                    }
                                                });
                                            }
                                        } else {
                                            // İlk birkaç framei atla
                                            logger.debug("Skipping initial frame {} (usually corrupted)", frameCount);
                                            inFrame = false;
                                            frameBuffer.reset();
                                        }
                                    }
                                }
                                
                                // Buffer overflow protection
                                if (frameBuffer.size() > 1024 * 1024) { // 1MB limit
                                    logger.warn("Frame buffer overflow, resetting");
                                    frameBuffer.reset();
                                    inFrame = false;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (isPreviewActive) {
                        logger.error("Error in frame reader", e);
                    }
                } finally {
                    logger.info("Frame reader thread exiting");
                }
            });
            
            frameReaderThread.setDaemon(true);
            frameReaderThread.setName("CameraFrameReader");
            frameReaderThread.start();
        }
    }
    
    public void stopPreview() {
        if (!isPreviewActive) {
            return;
        }
        
        logger.info("Stopping camera preview");
        isPreviewActive = false;
        
        synchronized (processLock) {
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                ffmpegProcess.destroy();
                try {
                    ffmpegProcess.waitFor(3000, TimeUnit.MILLISECONDS);
                    if (ffmpegProcess.isAlive()) {
                        ffmpegProcess.destroyForcibly();
                    }
                } catch (Exception e) {
                    logger.warn("Error terminating FFmpeg process", e);
                }
                ffmpegProcess = null;
            }
            
            if (frameReaderThread != null && frameReaderThread.isAlive()) {
                frameReaderThread.interrupt();
                try {
                    frameReaderThread.join(2000);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for frame reader", e);
                }
                frameReaderThread = null;
            }
        }
        
        // Normal stopPreviewda device bilgilerini temizle
        currentCameraDevice = null;
        currentCameraDeviceId = null;
        
        logger.info("Camera preview stopped");
    }
    
    /**
     * Preview durdurur - sadece recording için device bilgilerini korur
     */
    private void stopPreviewForRecording() {
        if (!isPreviewActive) {
            return;
        }
        
        logger.info("Stopping camera preview for recording");
        
        this.isPreviewActive = false;
        
        synchronized (processLock) {
            if (previewProcess != null && previewProcess.isAlive()) {
                previewProcess.destroy();
                try {
                    if (!previewProcess.waitFor(3000, TimeUnit.MILLISECONDS)) {
                        previewProcess.destroyForcibly();
                    }
                } catch (Exception e) {
                    logger.warn("Error waiting for preview process to stop", e);
                }
                previewProcess = null;
            }
        }
        
        // Device bilgilerini koruyoruz - currentCameraDevice ve currentCameraDeviceId
        logger.info("Camera preview stopped for recording");
    }
    
    public boolean startRecording(RecordingCallback callback) {
        if (!isPreviewActive) {
            if (callback != null) {
                callback.onRecordingError("Önce kamera önizlemesini başlatın");
            }
            return false;
        }
        
        if (isRecording) {
            if (callback != null) {
                callback.onRecordingError("Kayıt zaten aktif");
            }
            return false;
        }
        
        this.recordingCallback = callback;
        this.isRecording = true;
        
        try {
            // Preview durdurup recording moduna geç
            if (isPreviewActive) {
                stopPreviewForRecording();
                Thread.sleep(500); // Cameranın serbest kalması için bekle
            }
            
            startRecordingMode();
            
            if (callback != null) {
                Platform.runLater(() -> callback.onRecordingStarted());
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to start recording", e);
            if (callback != null) {
                callback.onRecordingError("Kayıt başlatılamadı: " + e.getMessage());
            }
            this.isRecording = false;
            return false;
        }
    }
    
    private void startRecordingMode() throws Exception {
        // Device ID kontrolü
        if (currentCameraDeviceId == null) {
            throw new IllegalStateException("No camera device ID available for recording");
        }
        
        // Çıkış dizinini oluştur
        java.nio.file.Path outputDir = java.nio.file.Paths.get(recordingOutputDir);
        if (!java.nio.file.Files.exists(outputDir)) {
            java.nio.file.Files.createDirectories(outputDir);
        }
        
        String outputFileName = generateRecordingFileName();
        String outputPath = recordingOutputDir + "/" + outputFileName;
        
        List<String> command = new ArrayList<>();
        command.add(ffmpegService.getFfmpegPath());
        command.add("-f");
        command.add("dshow");
        command.add("-thread_queue_size");
        command.add("512");
        command.add("-i");
        
        String cleanDeviceId = currentCameraDeviceId;
        if (cleanDeviceId.startsWith("video=")) {
            cleanDeviceId = cleanDeviceId.substring(6);
        }
        command.add("video=" + cleanDeviceId);
        
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("fast");
        command.add("-crf");
        command.add("23");
        command.add("-y");
        command.add(outputPath);
        
        logger.info("Recording command: {}", String.join(" ", command));
        
        ProcessBuilder recordPb = new ProcessBuilder(command);
        recordPb.redirectErrorStream(true);
        
        synchronized (processLock) {
            recordingProcess = recordPb.start();
            Thread.sleep(1000);
            
            if (!recordingProcess.isAlive()) {
                throw new IOException("Recording process failed to start");
            }
            
            currentRecordingPath = outputPath;
            logger.info("Recording started: {}", outputPath);
        }
    }
    
    private String generateRecordingFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return "recording_" + sdf.format(new Date()) + ".mp4";
    }
    
    public void stopRecording() {
        if (!isRecording) {
            return;
        }
        
        logger.info("Stopping recording");
        this.isRecording = false;
        
        if (recordingProcess != null && recordingProcess.isAlive()) {
            try {
                recordingProcess.destroy();
                recordingProcess.waitFor(3000, TimeUnit.MILLISECONDS);
                
                if (recordingProcess.isAlive()) {
                    recordingProcess.destroyForcibly();
                }
                
                recordingProcess = null;
            } catch (Exception e) {
                logger.error("Error stopping recording", e);
            }
        }
        
        if (recordingCallback != null) {
            Platform.runLater(() -> recordingCallback.onRecordingStopped());
        }
        
        currentRecordingPath = null;
        logger.info("Recording stopped");
        
        // Recording durduktan sonra previewı yeniden başlat
        if (currentCameraDevice != null) {
            try {
                Thread.sleep(500); // Cameranın serbest kalması için bekle
                startMjpegPreview(currentCameraDevice, previewCallback);
            } catch (Exception e) {
                logger.error("Failed to restart preview after recording", e);
            }
        }
    }
    
    public void shutdown() {
        logger.info("Shutting down CameraService");
        
        if (isRecording) {
            stopRecording();
        }
        
        if (isPreviewActive) {
            stopPreview();
        }
        
        logger.info("CameraService shutdown completed");
    }
    
    // Getter metodları
    public ObservableList<CameraDevice> getAvailableCameras() {
        return availableCameras;
    }
    
    public boolean isPreviewActive() {
        return isPreviewActive;
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public String getCurrentRecordingPath() {
        return currentRecordingPath;
    }
    
    public void setRecordingOutputDir(String outputDir) {
        this.recordingOutputDir = outputDir;
    }
    
    // Eksik metodlar - compatibility için
    public int getCurrentSegmentIndex() {
        return 0; // Basit implementation
    }
    
    public javafx.scene.Node getPreviewNode() {
        // Bu metod eskiden ImageView döndürüyordu, şimdi null dönelim
        return null;
    }
    
    public void setRecordingParams(String outputDir, String format, String codec, 
                                 Integer bitrate, Integer fps, Integer duration, String quality) {
        this.recordingOutputDir = outputDir;
        // Diğer parametreler şimdilik göz ardı edilir
    }
    
    public void pauseRecording() {
        // Basit implementation - şimdilik boş
        logger.info("Pause recording not implemented");
    }
    
    public void resumeRecording() {
        // Basit implementation - şimdilik boş
        logger.info("Resume recording not implemented");
    }
    
    // Callback interfaces
    public interface PreviewCallback {
        void onFrameReceived(byte[] frameData);
        void onPreviewError(String error);
    }
    
    public interface RecordingCallback {
        void onRecordingStarted();
        void onRecordingStopped();
        void onRecordingError(String error);
        void onSegmentCreated(String segmentPath);
    }
    
    // CameraDevice sınıfı
    public static class CameraDevice {
        private final String name;
        private final String deviceId;
        private final String description;
        
        public CameraDevice(String name, String deviceId, String description) {
            this.name = name;
            this.deviceId = deviceId;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDeviceId() {
            return deviceId;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public void setRecordingParams(String outputDir, String format, String quality, 
                                   int fps, int bitrate, int segmentDuration, String fileName) {
        this.recordingOutputDir = outputDir;
        this.recordingFormat = format;
        this.recordingQuality = quality;
        this.recordingFps = fps;
        this.recordingBitrate = bitrate;
        this.segmentDuration = segmentDuration;
        this.recordingFileName = fileName;
    }
    
    // Kayıt parametreleri için alan tanımları
    private volatile String recordingFormat = "mp4";
    private volatile String recordingQuality = "720p";
    private volatile int recordingFps = 25;
    private volatile int recordingBitrate = 2500;
    private volatile int segmentDuration = 300;
    private volatile String recordingFileName = "recording";
}