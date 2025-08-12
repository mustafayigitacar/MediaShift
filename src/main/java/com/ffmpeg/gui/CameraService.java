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
    private Process previewProcess;
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
                    
                    // DirectShow device lines direkt ara - section headera bakmadan
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

                                // Videomu ses mi bunun kontrolü
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
                        // Bu kameranın daha önce eklenmediğinden emin olmak için
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
        // Eğer zaten aktif bir önizleme varsa, önce onu durdur
        if (isPreviewActive) {
            logger.info("Preview already active, stopping current preview first");
            stopPreview();
            // Kısa bir bekleme süresi ekle
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting to restart preview", e);
            }
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
        // Format kontrolü sadece debug için - üretimde kaldırıldı
        // logger.info("Checking supported formats for camera: {}", camera.getName());
        // checkCameraFormats(camera);
        
        List<String> command = new ArrayList<>();
        command.add(ffmpegService.getFfmpegPath());
        command.add("-f");
        command.add("dshow");
        
        // DirectShow buffer ayarları - ultra düşük gecikme için
        command.add("-rtbufsize");
        command.add("512K"); // Ultra küçük buffer (minimum gecikme)
        command.add("-thread_queue_size");
        command.add("16");   // En küçük queue size
        command.add("-fflags");
        command.add("nobuffer"); // Düşük gecikme modu
        
        // Video size - yüksek çözünürlük için
        command.add("-video_size");
        command.add("1280x720");  // HD çözünürlük (daha iyi kalite)
        command.add("-framerate");
        command.add("30");  // Kameranın doğal frame ratei
        
        // Video input
        command.add("-i");
        String cleanDeviceId = camera.getDeviceId();
        if (cleanDeviceId.startsWith("video=")) {
            cleanDeviceId = cleanDeviceId.substring(6);
        }
        command.add("video=" + cleanDeviceId);
        
        // Video encoding settings - hızlı MJPEG
        command.add("-f");
        command.add("mjpeg");
        command.add("-pix_fmt");
        command.add("yuv420p");
        
        // Video filter - basit resize, hızlı işlem
        command.add("-vf");
        command.add("scale=640:480:flags=lanczos");  // Kaliteli scaling
        
        // Frame rate ayarı - doğal
        command.add("-r");
        command.add("30");  // Doğal frame rate
        
        // Kalite ayarları - performans odaklı
        command.add("-q:v");
        command.add("6");   // Daha iyi kalite, hızlı işlem
        
        // Buffer ayarları - ultra düşük gecikme için optimize edilmiş
        command.add("-bufsize");
        command.add("256K"); // Ultra küçük buffer size (minimum gecikme)
        command.add("-maxrate");
        command.add("1.5M"); // Daha düşük bitrate (daha hızlı işlem)
        
        // Log seviyesini ayarla
        command.add("-loglevel");
        command.add("error");
        
        // Output
        command.add("-");
        
        logger.info("FFmpeg command: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        synchronized (processLock) {
            ffmpegProcess = pb.start();
            
            // Processin başlamasını bekle
            try {
                Thread.sleep(2000); // Daha uzun bekleme süresi
                
                if (!ffmpegProcess.isAlive()) {
                    // Error outputu oku
                    BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
                    StringBuilder errorOutput = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                        logger.error("FFmpeg preview error: {}", line);
                    }
                    reader.close();
                    throw new IOException("FFmpeg preview process failed to start. FFmpeg output: " + errorOutput.toString());
                }
                
                logger.info("FFmpeg preview process started successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for FFmpeg process to start");
            }
            
            isPreviewActive = true;
            
            frameReaderThread = new Thread(() -> {
                logger.info("Frame reader thread started");
                try (BufferedInputStream inputStream = new BufferedInputStream(ffmpegProcess.getInputStream(), 8192)) {  // Buffer boyutunu artır
                    
                    ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096]; // Buffer boyutunu artır
                    int bytesRead;
                    boolean inFrame = false;
                    int frameCount = 0; // Frame sayacı
                    long lastFrameTime = 0;
                    final long MIN_FRAME_INTERVAL = 16; // ~60 FPS için minimum interval (maksimum responsiveness)
                    
                    while (isPreviewActive && !Thread.currentThread().isInterrupted()) {
                        
                        try {
                            // Check for interruption before reading
                            if (Thread.currentThread().isInterrupted()) {
                                logger.info("Frame reader thread interrupted");
                                break;
                            }
                            
                            bytesRead = inputStream.read(buffer);
                            if (bytesRead == -1) {
                                logger.info("End of stream reached");
                                break;
                            }
                        } catch (IOException e) {
                            if (Thread.currentThread().isInterrupted() || !isPreviewActive) {
                                logger.info("Frame reader interrupted or preview stopped");
                                break;
                            }
                            logger.warn("Read error in frame reader", e);
                            continue;
                        }
                        
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
                                        
                                        // Frame rate kontrolü - çok hızlı frameleri filtrele
                                        long currentTime = System.currentTimeMillis();
                                        if (currentTime - lastFrameTime >= MIN_FRAME_INTERVAL) {
                                            lastFrameTime = currentTime;
                                            
                                            // İlk birkaç frameyi atla, sonrasında tüm frameleri göster
                                            if (frameCount > 3) {
                                                final byte[] completeFrame = frameBytes.clone();
                                                inFrame = false;
                                                frameBuffer.reset();
                                                
                                                // Frame validation - siyah/boş frameleri filtrele
                                                if (previewCallback != null && completeFrame.length > 1000 && !isBlackFrame(completeFrame)) {
                                                    Platform.runLater(() -> {
                                                        try {
                                                            previewCallback.onFrameReceived(completeFrame);
                                                        } catch (Exception e) {
                                                            logger.warn("Error processing frame", e);
                                                        }
                                                    });
                                                }
                                            } else {
                                                // İlk birkaç frameyi atla (genellikle bozuk)
                                                logger.debug("Skipping initial frame {} (usually corrupted)", frameCount);
                                                inFrame = false;
                                                frameBuffer.reset();
                                            }
                                        } else {
                                            // Frame rate çok yüksek, bu frameyi atla
                                            logger.debug("Skipping frame due to high frame rate");
                                            inFrame = false;
                                            frameBuffer.reset();
                                        }
                                    }
                                }
                                
                                // Buffer overflow protection
                                if (frameBuffer.size() > 2 * 1024 * 1024) { // 2MB limit
                                    logger.warn("Frame buffer overflow, resetting");
                                    frameBuffer.reset();
                                    inFrame = false;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (isPreviewActive && !Thread.currentThread().isInterrupted()) {
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
            // Önce threadi interrupt et
            if (frameReaderThread != null && frameReaderThread.isAlive()) {
                frameReaderThread.interrupt();
            }
            
            // Sonra processi kapat
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                try {
                    // Streamleri kapat
                    if (ffmpegProcess.getInputStream() != null) {
                        ffmpegProcess.getInputStream().close();
                    }
                    if (ffmpegProcess.getOutputStream() != null) {
                        ffmpegProcess.getOutputStream().close();
                    }
                    if (ffmpegProcess.getErrorStream() != null) {
                        ffmpegProcess.getErrorStream().close();
                    }
                } catch (Exception e) {
                    logger.debug("Error closing streams", e);
                }
                
                ffmpegProcess.destroy();
                try {
                    boolean terminated = ffmpegProcess.waitFor(2000, TimeUnit.MILLISECONDS);
                    if (!terminated || ffmpegProcess.isAlive()) {
                        ffmpegProcess.destroyForcibly();
                        ffmpegProcess.waitFor(1000, TimeUnit.MILLISECONDS);
                    }
                } catch (Exception e) {
                    logger.warn("Error terminating FFmpeg process", e);
                }
                ffmpegProcess = null;
            }
            
            // Threadin kapanmasını bekle
            if (frameReaderThread != null && frameReaderThread.isAlive()) {
                try {
                    frameReaderThread.join(3000);
                    if (frameReaderThread.isAlive()) {
                        logger.warn("Frame reader thread still alive after interrupt");
                    }
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for frame reader", e);
                }
                frameReaderThread = null;
            }
        }
        
        // Device bilgilerini koruyoruz - önizleme tekrar başlatılabilir olsun
        // currentCameraDevice ve currentCameraDeviceId korunuyor
        // previewCallback da korunuyor
        
        logger.info("Camera preview stopped - device info preserved for restart");
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
            // Önce threadi interrupt et
            if (frameReaderThread != null && frameReaderThread.isAlive()) {
                frameReaderThread.interrupt();
            }
            
            // Sonra processi kapat
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                try {
                    // Streamleri kapat
                    if (ffmpegProcess.getInputStream() != null) {
                        ffmpegProcess.getInputStream().close();
                    }
                    if (ffmpegProcess.getOutputStream() != null) {
                        ffmpegProcess.getOutputStream().close();
                    }
                    if (ffmpegProcess.getErrorStream() != null) {
                        ffmpegProcess.getErrorStream().close();
                    }
                } catch (Exception e) {
                    logger.debug("Error closing streams", e);
                }
                
                ffmpegProcess.destroy();
                try {
                    boolean terminated = ffmpegProcess.waitFor(2000, TimeUnit.MILLISECONDS);
                    if (!terminated || ffmpegProcess.isAlive()) {
                        ffmpegProcess.destroyForcibly();
                        ffmpegProcess.waitFor(1000, TimeUnit.MILLISECONDS);
                    }
                } catch (Exception e) {
                    logger.warn("Error terminating FFmpeg process", e);
                }
                ffmpegProcess = null;
            }
            
            // Threadin kapanmasını bekle
            if (frameReaderThread != null && frameReaderThread.isAlive()) {
                try {
                    frameReaderThread.join(3000);
                    if (frameReaderThread.isAlive()) {
                        logger.warn("Frame reader thread still alive after interrupt");
                    }
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for frame reader", e);
                }
                frameReaderThread = null;
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
        
        try {
            // Mevcut önizlemeyi durdur ve tee filter ile hem önizleme hem kayıt yap
            stopPreviewForRecording();
            Thread.sleep(500); // Kameranın serbest kalması için bekle
            
            // Tek process ile hem önizleme hem kayıt
            startPreviewWithRecording();
            this.isRecording = true;
            
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
    
    /**
     * Ayrı process ile recording başlat - previewa dokunmadan
     */
    private void startRecordingProcess() throws Exception {
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
        
        // Ayrı FFmpeg process ile sadece kayıt yap
        List<String> command = new ArrayList<>();
        command.add(ffmpegService.getFfmpegPath());
        command.add("-f");
        command.add("dshow");
        command.add("-rtbufsize");
        command.add("512K"); // Ultra küçük buffer (minimum gecikme)
        command.add("-thread_queue_size");
        command.add("16");   // En küçük queue size
        command.add("-fflags");
        command.add("nobuffer"); // Düşük gecikme modu
        command.add("-video_size");
        command.add("1280x720"); // HD çözünürlük 
        command.add("-framerate");
        command.add("30");
        command.add("-i");
        
        String cleanDeviceId = currentCameraDeviceId;
        if (cleanDeviceId.startsWith("video=")) {
            cleanDeviceId = cleanDeviceId.substring(6);
        }
        command.add("video=" + cleanDeviceId);
        
        // Kayıt ayarları - seek optimizasyonu ile
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("ultrafast"); // Düşük gecikme için hızlı preset
        command.add("-crf");
        command.add("23");         // Balanced kalite-hız (düşük gecikme için)
        command.add("-profile:v");
        command.add("high");    // H.264 High Profile (daha iyi kalite)
        command.add("-level");
        command.add("4.0");     // H.264 Level 4.0 (1080p için)
        command.add("-tune");
        command.add("zerolatency"); // Düşük gecikme için optimize
        command.add("-pix_fmt");
        command.add("yuv420p");
        // Seek için keyframe interval - çok önemli!
        command.add("-g");
        command.add("30");      // Her 1 saniyede keyframe (30fps varsayarak)
        command.add("-keyint_min");
        command.add("15");      // Minimum keyframe interval
        command.add("-sc_threshold");
        command.add("0");       // Scene change detection kapalı - düzenli keyframes
        command.add("-force_key_frames");
        command.add("expr:gte(t,n_forced*" + segmentDuration + ")");
        // MP4 segment ayarları - seek optimizasyonu
        command.add("-movflags");
        command.add("+frag_keyframe+empty_moov+default_base_moof+faststart");
        command.add("-fflags");
        command.add("+genpts");
        // Timestamp düzeltmeleri
        command.add("-avoid_negative_ts");
        command.add("make_zero");
        
        // Log seviyesini ayarla - debug için info seviyesi
        command.add("-loglevel");
        command.add("info");
        
        // Segment desteği
        if (segmentDuration > 0) {
            logger.info("Recording with segments enabled - Duration: {} seconds", segmentDuration);
            command.add("-f");
            command.add("segment");
            command.add("-segment_time");
            command.add(String.valueOf(segmentDuration));
            command.add("-segment_start_number");
            command.add("0");  // 000'dan başlasın
            command.add("-reset_timestamps");
            command.add("1");
            command.add("-segment_format");
            command.add("mp4");
            command.add("-segment_format_options");
            command.add("movflags=+faststart");
            command.add("-break_non_keyframes");
            command.add("1");
            
            // Timer henüz başlamadıysa timestamp oluştur
            if (recordingStartTimestamp == null) {
                recordingStartTimestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            }
            String segmentPattern = java.nio.file.Paths.get(recordingOutputDir, "MediaShift_" + recordingStartTimestamp + "_segment_%03d." + recordingFormat).toString();
            command.add(segmentPattern);
        } else {
            command.add("-y");
            command.add(outputPath);
        }
        
        logger.info("Recording command: {}", String.join(" ", command));
        
        ProcessBuilder recordPb = new ProcessBuilder(command);
        recordPb.redirectErrorStream(true);
        
        synchronized (processLock) {
            recordingProcess = recordPb.start();
            
            // Processin başlamasını bekle
            try {
                Thread.sleep(2000); // Daha uzun bekleme süresi
                
                if (!recordingProcess.isAlive()) {
                    // Error outputu oku
                    BufferedReader reader = new BufferedReader(new InputStreamReader(recordingProcess.getInputStream()));
                    StringBuilder errorOutput = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                        logger.error("FFmpeg recording error: {}", line);
                    }
                    reader.close();
                    throw new IOException("Recording process failed to start. FFmpeg output: " + errorOutput.toString());
                }
                
                logger.info("FFmpeg recording process started successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for recording process to start");
            }
            
            if (segmentDuration > 0) {
                currentRecordingPath = recordingOutputDir;
            } else {
                currentRecordingPath = outputPath;
            }
            
            logger.info("Recording started: {}", (segmentDuration > 0) ? recordingOutputDir : outputPath);
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
        
        // Kayıt zamanlayıcısını durdur
        stopRecordingTimer();
        
        // Tee filter processini durdur
        synchronized (processLock) {
            // Önce threadi interrupt et
            if (frameReaderThread != null && frameReaderThread.isAlive()) {
                frameReaderThread.interrupt();
            }

            // Sonra processi kapat
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                try {
                    // Streamleri kapat
                    if (ffmpegProcess.getInputStream() != null) {
                        ffmpegProcess.getInputStream().close();
                    }
                    if (ffmpegProcess.getOutputStream() != null) {
                        ffmpegProcess.getOutputStream().close();
                    }
                    if (ffmpegProcess.getErrorStream() != null) {
                        ffmpegProcess.getErrorStream().close();
                    }
                } catch (Exception e) {
                    logger.debug("Error closing streams", e);
                }
                
                ffmpegProcess.destroy();
                try {
                    boolean terminated = ffmpegProcess.waitFor(3000, TimeUnit.MILLISECONDS);
                    if (!terminated || ffmpegProcess.isAlive()) {
                        ffmpegProcess.destroyForcibly();
                        ffmpegProcess.waitFor(1000, TimeUnit.MILLISECONDS);
                    }
                } catch (Exception e) {
                    logger.warn("Error terminating FFmpeg process", e);
                }
                ffmpegProcess = null;
                recordingProcess = null;
            }
            
            // Threadin kapanmasını bekle
            if (frameReaderThread != null && frameReaderThread.isAlive()) {
                try {
                    frameReaderThread.join(3000);
                    if (frameReaderThread.isAlive()) {
                        logger.warn("Frame reader thread still alive after interrupt");
                    }
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for frame reader", e);
                }
                frameReaderThread = null;
            }
        }
        
        if (recordingCallback != null) {
            Platform.runLater(() -> recordingCallback.onRecordingStopped());
        }
        
        currentRecordingPath = null;
        isPreviewActive = false;
        logger.info("Recording stopped successfully");
        
        // Kayıt bittikten sonra sadece önizlemeyi yeniden başlat
        if (currentCameraDevice != null && currentCameraDeviceId != null && previewCallback != null) {
            try {
                Thread.sleep(500); // Kameranın serbest kalması için bekle
                CameraDevice deviceToRestart = new CameraDevice(currentCameraDevice, currentCameraDeviceId, "Last used device");
                logger.info("Restarting preview after recording stopped");
                startPreview(deviceToRestart, previewCallback);
            } catch (Exception e) {
                logger.error("Failed to restart preview after recording", e);
                if (previewCallback != null) {
                    Platform.runLater(() -> previewCallback.onPreviewError("Önizleme yeniden başlatılamadı."));
                }
            }
        }
    }
    
    /**
     * Hem preview hem recording yapan tee filter metodu
     */
    private void startPreviewWithRecording() throws Exception {
        if (currentCameraDeviceId == null) {
            throw new IllegalStateException("No camera device ID available");
        }
        
        // Çıkış dizinini oluştur
        java.nio.file.Path outputDir = java.nio.file.Paths.get(recordingOutputDir);
        if (!java.nio.file.Files.exists(outputDir)) {
            java.nio.file.Files.createDirectories(outputDir);
        }

        String outputFileName = generateRecordingFileName();
        String outputPath = recordingOutputDir + "/" + outputFileName;        // FFmpeg tee filter komutu - performans optimizasyonu
        List<String> command = new ArrayList<>();
        command.add(ffmpegService.getFfmpegPath());
        command.add("-f");
        command.add("dshow");
        command.add("-rtbufsize");
        command.add("2M");   // Segment geçişleri için biraz daha büyük buffer
        command.add("-thread_queue_size");
        command.add("64");   // Segment geçişlerinde queue overflow önlemek için
        command.add("-fflags");
        command.add("nobuffer+genpts"); // Buffer devre dışı + PTS generation
        command.add("-video_size");
        command.add("1280x720"); // HD çözünürlük
        command.add("-framerate");
        command.add("30");
        command.add("-i");
        command.add(currentCameraDeviceId);

        // Log seviyesini ayarla - debug için info seviyesi
        command.add("-loglevel");
        command.add("info");
        
        // Video çıkışları için split filter - minimal işlem
        // Tee filter ile hem preview hem recording
        command.add("-filter_complex");
        
        if (segmentDuration > 0) {
            // Segment recording için split filter - timestampi önceden oluştur
            if (recordingStartTimestamp == null) {
                recordingStartTimestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            }
            String timestamp = recordingStartTimestamp;
            String segmentPattern = java.nio.file.Paths.get(recordingOutputDir, "MediaShift_" + timestamp + "_segment_%03d." + recordingFormat).toString();
            
            command.add("[0:v]split=2[v1][v2]; [v1]scale=640:360:flags=lanczos:force_original_aspect_ratio=decrease[preview]");
            
            // Preview output
            command.add("-map");
            command.add("[preview]");
            command.add("-f");
            command.add("mjpeg");
            command.add("-pix_fmt");
            command.add("yuv420p");
            command.add("-r");
            command.add("30");
            command.add("-q:v");
            command.add("6");
            command.add("pipe:1");
            
            // Recording output with segments - seek optimizasyonu
            command.add("-map");
            command.add("[v2]");
            command.add("-c:v");
            command.add("libx264");
            command.add("-preset");
            command.add("fast");     // Hızlı encoding için
            command.add("-crf");
            command.add("23");       // Balanced kalite-hız
            command.add("-tune");
            command.add("zerolatency"); // Düşük gecikme için
            command.add("-pix_fmt");
            command.add("yuv420p");
            // Seek için keyframe ve segment ayarları - segment geçişlerinde stabil olması için
            command.add("-g");
            command.add("30");      // 30fps için her saniye keyframe
            command.add("-keyint_min");
            command.add("30");      // Keyframe intervalı artır (stabil geçiş için)
            command.add("-sc_threshold");
            command.add("0");       // Düzenli keyframeler için
            command.add("-force_key_frames");
            command.add("expr:gte(t,n_forced*" + segmentDuration + ")");
            logger.info("Split recording with segments - Duration: {} seconds", segmentDuration);
            command.add("-f");
            command.add("segment");
            command.add("-segment_time");
            command.add(String.valueOf(segmentDuration));
            command.add("-segment_start_number");
            command.add("0");
            command.add("-reset_timestamps");
            command.add("0");  // Timestampleri reset etme (süreklilik için)
            command.add("-segment_atclocktime");
            command.add("1");  // Smooth segment transitions
            command.add("-segment_format");
            command.add("mp4");
            command.add("-segment_format_options");
            command.add("movflags=+frag_keyframe+empty_moov+default_base_moof+faststart:avoid_negative_ts=make_zero");
            command.add("-break_non_keyframes");
            command.add("0");  // Non-keyframede break etme (süreklilik için)
            command.add(segmentPattern);
        } else {
            // Normal recording için split filter
            command.add("[0:v]split=2[out1][out2]; [out1]scale=640:360:flags=lanczos:force_original_aspect_ratio=decrease[preview]");
            
            // Preview output
            command.add("-map");
            command.add("[preview]");
            command.add("-f");
            command.add("mjpeg");
            command.add("-pix_fmt");
            command.add("yuv420p");
            command.add("-r");
            command.add("30");
            command.add("-q:v");
            command.add("6");
            command.add("pipe:1");
            
            // Recording output - seek optimizasyonu ile
            command.add("-map");
            command.add("[out2]");
            command.add("-c:v");
            command.add("libx264");
            command.add("-preset");
            command.add("fast");     // Hızlı encoding için
            command.add("-crf");
            command.add("23");       // Balanced kalite-hız
            command.add("-tune");
            command.add("zerolatency"); // Düşük gecikme için
            command.add("-pix_fmt");
            command.add("yuv420p");
            // Seek için keyframe ayarları
            command.add("-g");
            command.add("30");      // Her saniye keyframe
            command.add("-keyint_min");
            command.add("15");      // Minimum keyframe interval
            command.add("-sc_threshold");
            command.add("0");       // Düzenli keyframeler
            // MP4 optimizasyonları - seek desteği
            command.add("-movflags");
            command.add("+faststart");
            command.add("-y");
            command.add(outputPath);
        }
        
        logger.info("Split filter command: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        synchronized (processLock) {
            ffmpegProcess = pb.start();
            recordingProcess = ffmpegProcess; // Aynı process
            
            // Processin başlamasını bekle
            try {
                Thread.sleep(2000); // Daha uzun bekleme süresi
                
                if (!ffmpegProcess.isAlive()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));
                    StringBuilder errorOutput = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                        logger.error("FFmpeg tee filter error: {}", line);
                    }
                    reader.close();
                    throw new IOException("Tee filter process failed. FFmpeg output: " + errorOutput.toString());
                }
                
                logger.info("FFmpeg tee filter process started successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for tee filter process to start");
            }
            
            // Frame readerı başlat
            frameReaderThread = new Thread(() -> {
                try (BufferedInputStream bis = new BufferedInputStream(ffmpegProcess.getInputStream(), 32768)) { // Buffer boyutunu artır
                    byte[] buffer = new byte[16384]; // Buffer boyutunu artır
                    ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
                    int bytesRead;
                    int frameSkipCount = 0;
                    final int FRAMES_TO_SKIP = 2; // İlk frame sayısını artır
                    long lastFrameTime = 0;
                    final long MIN_FRAME_INTERVAL = 16; // ~60 FPS için minimum interval (maksimum responsiveness)
                    
                    logger.info("Frame reader thread started");
                    
                    while ((bytesRead = bis.read(buffer)) != -1 && !Thread.currentThread().isInterrupted()) {
                        frameBuffer.write(buffer, 0, bytesRead);
                        
                        // JPEG marker kontrolü
                        byte[] currentData = frameBuffer.toByteArray();
                        if (currentData.length >= 2) {
                            for (int i = 0; i < currentData.length - 1; i++) {
                                if ((currentData[i] & 0xFF) == 0xFF && (currentData[i + 1] & 0xFF) == 0xD9) {
                                    // JPEG sonu bulundu
                                    byte[] frameData = new byte[i + 2];
                                    System.arraycopy(currentData, 0, frameData, 0, i + 2);
                                    
                                    frameSkipCount++;
                                    if (frameSkipCount <= FRAMES_TO_SKIP) {
                                        logger.debug("Skipping initial frame {} (usually corrupted)", frameSkipCount);
                                    } else {
                                        // Frame rate kontrolü
                                        long currentTime = System.currentTimeMillis();
                                        if (currentTime - lastFrameTime >= MIN_FRAME_INTERVAL) {
                                            lastFrameTime = currentTime;
                                            
                                            // Frame validation ve UI threade gönder
                                            if (previewCallback != null && frameData.length > 1000 && !isBlackFrame(frameData)) {
                                                Platform.runLater(() -> previewCallback.onFrameReceived(frameData));
                                            }
                                        } else {
                                            logger.debug("Skipping frame due to high frame rate");
                                        }
                                    }
                                    
                                    // Bufferı temizle ve kalan veriyi başa al
                                    frameBuffer.reset();
                                    if (i + 2 < currentData.length) {
                                        frameBuffer.write(currentData, i + 2, currentData.length - i - 2);
                                    }
                                    break;
                                }
                            }
                        }
                        
                        // Buffer overflow protection
                        if (frameBuffer.size() > 4 * 1024 * 1024) { // 4MB limit
                            logger.warn("Frame buffer overflow, resetting");
                            frameBuffer.reset();
                        }
                    }
                } catch (Exception e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        logger.error("Frame reader error", e);
                    }
                } finally {
                    logger.info("Frame reader thread exiting");
                }
            }, "CameraFrameReader");
            frameReaderThread.start();
            
            if (segmentDuration > 0) {
                currentRecordingPath = recordingOutputDir;
            } else {
                currentRecordingPath = outputPath;
            }
            
            // Kayıt zamanlayıcısını başlat
            startRecordingTimer();
            
            isPreviewActive = true;
            logger.info("Tee filter active - both preview and recording: {}", (segmentDuration > 0) ? recordingOutputDir : outputPath);
        }
    }
    
    private void checkCameraFormats(CameraDevice camera) {
        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegService.getFfmpegPath());
            command.add("-f");
            command.add("dshow");
            command.add("-list_options");
            command.add("true");
            command.add("-i");
            String cleanDeviceId = camera.getDeviceId();
            if (cleanDeviceId.startsWith("video=")) {
                cleanDeviceId = cleanDeviceId.substring(6);
            }
            command.add("video=" + cleanDeviceId);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                logger.info("=== Camera {} Supported Formats ===", camera.getName());
                while ((line = reader.readLine()) != null) {
                    if (line.contains("pixel_format=") || line.contains("fps=") || line.contains("s=")) {
                        logger.info("Format: {}", line.trim());
                    }
                }
                logger.info("=== End Camera Formats ===");
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            logger.warn("Could not check camera formats: {}", e.getMessage());
        }
    }

    public void shutdown() {
        logger.info("Shutting down CameraService");
        
        // Kayıt durumunu kontrol et ve durdur
        if (isRecording) {
            logger.info("Stopping active recording...");
            isRecording = false;
            
            // Recording processi durdur
            synchronized (processLock) {
                if (recordingProcess != null && recordingProcess.isAlive()) {
                    try {
                        recordingProcess.destroy();
                        boolean terminated = recordingProcess.waitFor(3, TimeUnit.SECONDS);
                        if (!terminated) {
                            recordingProcess.destroyForcibly();
                            recordingProcess.waitFor(2, TimeUnit.SECONDS);
                        }
                    } catch (Exception e) {
                        logger.warn("Error waiting for recording process termination", e);
                    }
                    recordingProcess = null;
                }
            }
        }
        
        // Preview durumunu kontrol et ve durdur
        if (isPreviewActive) {
            logger.info("Stopping active preview...");
            stopPreview();
        }
        
        // Tüm processleri zorla kapat
        synchronized (processLock) {
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                try {
                    ffmpegProcess.destroyForcibly();
                    ffmpegProcess.waitFor(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.warn("Error terminating ffmpeg process", e);
                }
                ffmpegProcess = null;
            }
            
            if (previewProcess != null && previewProcess.isAlive()) {
                try {
                    previewProcess.destroyForcibly();
                    previewProcess.waitFor(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.warn("Error terminating preview process", e);
                }
                previewProcess = null;
            }
        }
        
        // Frame reader threadini zorla sonlandır
        if (frameReaderThread != null && frameReaderThread.isAlive()) {
            frameReaderThread.interrupt();
            try {
                frameReaderThread.join(2000);
                if (frameReaderThread.isAlive()) {
                    logger.warn("Frame reader thread still alive, forcing termination");
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for frame reader termination", e);
            }
            frameReaderThread = null;
        }
        
        // Tüm durumları sıfırla
        isPreviewActive = false;
        isRecording = false;
        currentCameraDevice = null;
        currentCameraDeviceId = null;
        currentRecordingPath = null;
        previewCallback = null;
        recordingCallback = null;
        
        // Camera listesini temizle
        if (availableCameras != null) {
            availableCameras.clear();
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
    
    public String getSelectedCameraDevice() {
        return currentCameraDeviceId;
    }
    
    public void setRecordingOutputDir(String outputDir) {
        this.recordingOutputDir = outputDir;
    }
    
    // Eksik metodlar - compatibility için
    public int getCurrentSegmentIndex() {
        return currentSegmentIndex;
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
        
        logger.info("Recording parameters set - Segment Duration: {} seconds, Quality: {}, FPS: {}, Bitrate: {}", 
                   segmentDuration, quality, fps, bitrate);
    }
    
    // Kayıt parametreleri için alan tanımları
    private volatile String recordingFormat = "mp4";
    private volatile String recordingQuality = "720p";
    private volatile int recordingFps = 25;
    private volatile int recordingBitrate = 2500;
    private volatile int segmentDuration = 5; // Varsayılan 5 saniye
    private volatile String recordingFileName = "recording";
    
    // Kayıt zamanlayıcısı için alanlar
    private volatile long recordingStartTime = 0;
    private volatile long totalRecordingTime = 0;
    private volatile long pausedDuration = 0;
    private volatile long lastPauseTime = 0;
    private volatile boolean isPaused = false;
    private volatile int currentSegmentIndex = 0;
    private volatile long segmentStartTime = 0;
    private volatile String currentSegmentPath = null;
    private volatile String recordingStartTimestamp = null;  // Kayıt başlangıç timestampi
    private final List<String> recordedSegments = new ArrayList<>();
    private final Object segmentLock = new Object();
    private Thread recordingTimerThread = null;

    /**
     * Önizleme streaminden kayıt yap - Virtual Camera yaklaşımı
     */
    private void startRecordingFromPreviewStream() throws Exception {
        if (currentCameraDeviceId == null) {
            throw new IllegalStateException("No camera device ID available for recording");
        }
        
        // Çıkış dizinini oluştur
        java.nio.file.Path outputDir = java.nio.file.Paths.get(recordingOutputDir);
        if (!java.nio.file.Files.exists(outputDir)) {
            java.nio.file.Files.createDirectories(outputDir);
        }
        
        // Kayıt zamanlayıcısını başlat
        startRecordingTimer();
        
        // Gerçek FFmpeg kayıt işlemini başlat
        startFFmpegRecording();
        
        if (segmentDuration > 0) {
            currentRecordingPath = recordingOutputDir;
        } else {
            String outputFileName = generateRecordingFileName();
            currentRecordingPath = recordingOutputDir + "/" + outputFileName;
        }
        
        logger.info("Recording started from preview stream: {}", currentRecordingPath);
    }
    
    /**
     * FFmpeg ile gerçek kayıt işlemini başlat
     */
    private void startFFmpegRecording() throws Exception {
        if (currentCameraDeviceId == null) {
            throw new IllegalStateException("No camera device ID available for recording");
        }
        
        // Çıkış dizinini oluştur
        java.nio.file.Path outputDir = java.nio.file.Paths.get(recordingOutputDir);
        if (!java.nio.file.Files.exists(outputDir)) {
            java.nio.file.Files.createDirectories(outputDir);
        }
        
        String cleanDeviceId = currentCameraDeviceId;
        if (cleanDeviceId.startsWith("video=")) {
            cleanDeviceId = cleanDeviceId.substring(6);
        }
        
        List<String> command = new ArrayList<>();
        command.add(ffmpegService.getFfmpegPath());
        command.add("-f");
        command.add("dshow");
        command.add("-rtbufsize");
        command.add("1M");   // Düşük gecikme için küçük buffer
        command.add("-thread_queue_size");
        command.add("32");   // Küçük queue size
        command.add("-fflags");
        command.add("nobuffer"); // Bufferı devre dışı bırak
        command.add("-video_size");
        command.add("1280x720"); // HD çözünürlük
        command.add("-framerate");
        command.add(String.valueOf(recordingFps));
        command.add("-i");
        command.add("video=" + cleanDeviceId);
        
        // Kayıt ayarları
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("ultrafast"); // En hızlı encoding
        command.add("-crf");
        command.add("25");         // Hızlı processing için
        command.add("-tune");
        command.add("zerolatency"); // Düşük gecikme için
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-loglevel");
        command.add("error");
        
        // Segment desteği
        if (segmentDuration > 0) {
            logger.info("Screen recording with segments - Duration: {} seconds", segmentDuration);
            command.add("-f");
            command.add("segment");
            command.add("-segment_time");
            command.add(String.valueOf(segmentDuration));
            command.add("-reset_timestamps");
            command.add("1");
            command.add("-segment_format");
            command.add("mp4");
            command.add("-segment_format_options");
            command.add("movflags=+faststart");
            command.add("-break_non_keyframes");
            command.add("1");
            command.add("-strftime");
            command.add("1");
            String segmentPattern = java.nio.file.Paths.get(recordingOutputDir, "segment_%03d." + recordingFormat).toString();
            command.add(segmentPattern);
        } else {
            String outputFileName = generateRecordingFileName();
            String outputPath = recordingOutputDir + "/" + outputFileName;
            command.add("-y");
            command.add(outputPath);
        }
        
        logger.info("FFmpeg recording command: {}", String.join(" ", command));
        
        ProcessBuilder recordPb = new ProcessBuilder(command);
        recordPb.redirectErrorStream(true);
        
        synchronized (processLock) {
            recordingProcess = recordPb.start();
            
            // Processin başlamasını bekle
            try {
                Thread.sleep(2000);
                
                if (!recordingProcess.isAlive()) {
                    // Error outputu oku
                    BufferedReader reader = new BufferedReader(new InputStreamReader(recordingProcess.getInputStream()));
                    StringBuilder errorOutput = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                        logger.error("FFmpeg recording error: {}", line);
                    }
                    reader.close();
                    throw new IOException("Recording process failed to start. FFmpeg output: " + errorOutput.toString());
                }
                
                logger.info("FFmpeg recording process started successfully");
                
                // Segment monitoring threadini başlat
                if (segmentDuration > 0) {
                    startSegmentMonitoring();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for recording process to start");
            }
        }
    }
    
    /**
     * Segment dosyalarını izle ve yeni segmentler oluştuğunda callback çağır
     */
    private void startSegmentMonitoring() {
        Thread segmentMonitorThread = new Thread(() -> {
            try {
                java.nio.file.Path outputDir = java.nio.file.Paths.get(recordingOutputDir);
                java.util.Set<java.nio.file.Path> existingFiles = new java.util.HashSet<>();
                
                // Mevcut dosyaları tara
                if (java.nio.file.Files.exists(outputDir)) {
                    java.nio.file.Files.list(outputDir)
                        .filter(path -> path.toString().endsWith("." + recordingFormat))
                        .forEach(existingFiles::add);
                }
                
                while (isRecording && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000); // Her saniye kontrol et
                    
                    if (java.nio.file.Files.exists(outputDir)) {
                        java.util.Set<java.nio.file.Path> currentFiles = new java.util.HashSet<>();
                        java.nio.file.Files.list(outputDir)
                            .filter(path -> path.toString().endsWith("." + recordingFormat))
                            .forEach(currentFiles::add);
                        
                        // Yeni dosyaları bul
                        for (java.nio.file.Path file : currentFiles) {
                            if (!existingFiles.contains(file)) {
                                existingFiles.add(file);
                                String segmentPath = file.toString();
                                
                                synchronized (segmentLock) {
                                    recordedSegments.add(segmentPath);
                                }
                                
                                // Callback çağır
                                if (recordingCallback != null) {
                                    Platform.runLater(() -> {
                                        try {
                                            recordingCallback.onSegmentCreated(segmentPath);
                                        } catch (Exception e) {
                                            logger.debug("Segment callback error", e);
                                        }
                                    });
                                }
                                
                                logger.info("New segment detected: {}", segmentPath);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Segment monitoring thread interrupted");
            } catch (Exception e) {
                logger.error("Error in segment monitoring thread", e);
            }
        }, "SegmentMonitorThread");
        
        segmentMonitorThread.setDaemon(true);
        segmentMonitorThread.start();
        logger.info("Segment monitoring started");
    }
    
    /**
     * Kayıt zamanlayıcısını başlat
     */
    private void startRecordingTimer() {
        recordingStartTime = System.currentTimeMillis();
        totalRecordingTime = 0;
        pausedDuration = 0;
        isPaused = false;
        currentSegmentIndex = 0;
        segmentStartTime = recordingStartTime;
        
        // Timestamp zaten startPreviewWithRecordingde oluşturuldu
        // recordingStartTimestampi burada tekrar oluşturmuyoruz
        
        // Recorded segments listesini temizle
        recordedSegments.clear();
        
        // Zamanlayıcı threadini başlat
        recordingTimerThread = new Thread(() -> {
            try {
                while (isRecording && !Thread.currentThread().isInterrupted()) {
                    if (!isPaused) {
                        long currentTime = System.currentTimeMillis();
                        totalRecordingTime = currentTime - recordingStartTime - pausedDuration;
                        
                        // Segment kontrolü - FFmpegin oluşturduğu dosyaları kontrol et
                        if (segmentDuration > 0) {
                            long elapsedSeconds = totalRecordingTime / 1000;
                            int expectedSegmentCount = (int) (elapsedSeconds / segmentDuration);
                            
                            // FFmpegin oluşturduğu segment dosyalarını kontrol et
                            checkForNewSegments(expectedSegmentCount);
                        }
                        
                        // Zaman güncellemesi callbacke gönder
                        if (recordingCallback != null) {
                            String formattedTime = formatTime(totalRecordingTime);
                            Platform.runLater(() -> {
                                try {
                                    // RecordingCallbacke onTimeUpdate ekle
                                    if (recordingCallback instanceof ExtendedRecordingCallback) {
                                        ((ExtendedRecordingCallback) recordingCallback).onTimeUpdate(formattedTime);
                                    }
                                } catch (Exception e) {
                                    logger.debug("Time update callback error", e);
                                }
                            });
                        }
                    }
                    
                    Thread.sleep(100); // 100ms aralıklarla güncelle
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Recording timer thread interrupted");
            } catch (Exception e) {
                logger.error("Error in recording timer thread", e);
            }
        }, "RecordingTimerThread");
        
        recordingTimerThread.setDaemon(true);
        recordingTimerThread.start();
        logger.info("Recording timer started");
    }
    
    /**
     * FFmpegin oluşturduğu yeni segment dosyalarını kontrol et
     */
    private void checkForNewSegments(int expectedSegmentCount) {
        synchronized (segmentLock) {
            try {
                // Kayıt dizinini kontrol et
                java.io.File recordingDir = new java.io.File(recordingOutputDir);
                if (!recordingDir.exists()) {
                    logger.warn("Recording directory does not exist: {}", recordingOutputDir);
                    return;
                }
                
                // Timestampi al
                String timestamp = recordingStartTimestamp;
                logger.debug("checkForNewSegments - timestamp: {}, expectedSegmentCount: {}", timestamp, expectedSegmentCount);
                
                if (timestamp == null) {
                    logger.debug("Timestamp is null, scanning directory for MediaShift files");
                    // Klasördeki tüm MediaShift dosyalarını kontrol et
                    java.io.File[] files = recordingDir.listFiles((dir, name) -> 
                        name.startsWith("MediaShift_") && name.endsWith(".mp4"));
                    
                    if (files != null && files.length > 0) {
                        // En son oluşturulan segment dosyalarını bul
                        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                        
                        for (java.io.File file : files) {
                            if (!recordedSegments.contains(file.getAbsolutePath())) {
                                recordedSegments.add(file.getAbsolutePath());
                                
                                if (recordingCallback != null) {
                                    recordingCallback.onSegmentCreated(file.getAbsolutePath());
                                }
                                
                                logger.info("New segment detected: {}", file.getAbsolutePath());
                            }
                        }
                    }
                    return;
                }
                
                // Mevcut segment indexinden başlayarak yeni dosyaları kontrol et
                logger.debug("Checking segments with timestamp: {}", timestamp);
                int foundSegments = 0;
                for (int i = 0; i <= expectedSegmentCount; i++) {
                    String segmentFileName = String.format("MediaShift_%s_segment_%03d.%s", timestamp, i, recordingFormat);
                    String segmentPath = java.nio.file.Paths.get(recordingOutputDir, segmentFileName).toString();
                    java.io.File segmentFile = new java.io.File(segmentPath);
                    
                    logger.debug("Checking segment: {} - exists: {}", segmentPath, segmentFile.exists());
                    
                    // Dosya varsa ve daha önce eklenmemişse listeye ekle
                    if (segmentFile.exists() && !recordedSegments.contains(segmentPath)) {
                        recordedSegments.add(segmentPath);
                        currentSegmentIndex = Math.max(currentSegmentIndex, i);
                        foundSegments++;
                        
                        if (recordingCallback != null) {
                            recordingCallback.onSegmentCreated(segmentPath);
                        }
                        
                        logger.info("New segment detected: {}", segmentPath);
                    }
                }
                
                logger.debug("Segment check completed - found {} new segments", foundSegments);
                
            } catch (Exception e) {
                logger.error("Error checking for new segments", e);
            }
        }
    }
    
    /**
     * Segment oluşturma - sadece loglar için kullanılıyor
     */
    private void createNewSegment() {
        synchronized (segmentLock) {
            try {
                currentSegmentIndex++;
                segmentStartTime = System.currentTimeMillis();
                
                logger.info("Segment index incremented to: {}", currentSegmentIndex);
                
            } catch (Exception e) {
                logger.error("Error in segment creation", e);
            }
        }
    }
    
    /**
     * Zamanı formatla (HH:MM:SS)
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }
    
    /**
     * Kayıt zamanlayıcısını durdur
     */
    private void stopRecordingTimer() {
        if (recordingTimerThread != null && recordingTimerThread.isAlive()) {
            recordingTimerThread.interrupt();
            try {
                recordingTimerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            recordingTimerThread = null;
        }
        
        // FFmpegin son segmenti yazması için ekstra bekle
        if (segmentDuration > 0) {
            try {
                Thread.sleep(1500); // Son segmentin yazılması için bekle
                logger.info("Waiting for final segment completion");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Son segmenti kaydet (normal recording için)
        if (segmentDuration > 0 && currentSegmentPath != null) {
            synchronized (segmentLock) {
                recordedSegments.add(currentSegmentPath);
                if (recordingCallback != null) {
                    recordingCallback.onSegmentCreated(currentSegmentPath);
                }
            }
        }
        
        logger.info("Recording timer stopped");
    }
    
    /**
     * Kayıt duraklat
     */
    public void pauseRecording() {
        if (isRecording && !isPaused) {
            isPaused = true;
            lastPauseTime = System.currentTimeMillis();
            logger.info("Recording paused");
            
            if (recordingCallback != null) {
                Platform.runLater(() -> {
                    try {
                        if (recordingCallback instanceof ExtendedRecordingCallback) {
                            ((ExtendedRecordingCallback) recordingCallback).onRecordingPaused();
                        }
                    } catch (Exception e) {
                        logger.debug("Pause callback error", e);
                    }
                });
            }
        }
    }
    
    /**
     * Kayıt devam ettir
     */
    public void resumeRecording() {
        if (isRecording && isPaused) {
            long currentTime = System.currentTimeMillis();
            pausedDuration += (currentTime - lastPauseTime);
            isPaused = false;
            logger.info("Recording resumed");
            
            if (recordingCallback != null) {
                Platform.runLater(() -> {
                    try {
                        if (recordingCallback instanceof ExtendedRecordingCallback) {
                            ((ExtendedRecordingCallback) recordingCallback).onRecordingResumed();
                        }
                    } catch (Exception e) {
                        logger.debug("Resume callback error", e);
                    }
                });
            }
        }
    }
    
    /**
     * Toplam kayıt süresini döndür
     */
    public long getTotalRecordingTime() {
        if (recordingStartTime == 0) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long totalTime = currentTime - recordingStartTime - pausedDuration;
        
        if (isPaused && lastPauseTime > 0) {
            totalTime -= (currentTime - lastPauseTime);
        }
        
        return totalTime;
    }
    
    /**
     * Kaydedilen segmentleri döndür
     */
    public List<String> getRecordedSegments() {
        synchronized (segmentLock) {
            return new ArrayList<>(recordedSegments);
        }
    }
    

    
    /**
     * Genişletilmiş kayıt callback arayüzü
     */
    public interface ExtendedRecordingCallback extends RecordingCallback {
        void onTimeUpdate(String formattedTime);
        void onRecordingPaused();
        void onRecordingResumed();
    }
    
    /**
     * Siyah/boş frameleri tespit eder
     */
    private boolean isBlackFrame(byte[] frameData) {
        if (frameData == null || frameData.length < 1000) {
            return true; // Çok küçük frameler muhtemelen boş
        }
        
        // JPEG framelerde 0x00 bytelarının oranını kontrol et
        int zeroCount = 0;
        int sampleSize = Math.min(frameData.length, 5000); // İlk 5KBı sample olarak al
        
        for (int i = 0; i < sampleSize; i++) {
            if (frameData[i] == 0x00) {
                zeroCount++;
            }
        }
        
        // %80'den fazla sıfır byte varsa siyah frame olarak kabul et
        double zeroRatio = (double) zeroCount / sampleSize;
        return zeroRatio > 0.8;
    }
}