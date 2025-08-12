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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Video segmentlerini birleştiren sınıf
 */
public class VideoSegmentMerger extends Task<String> {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoSegmentMerger.class);
    
    private final List<String> segmentPaths;
    private final String outputPath;
    private final MergeCallback callback;
    
    private Process ffmpegProcess;
    private double totalDuration = 0;
    private double currentProgress = 0;
    
    // Callback arayüzü
    public interface MergeCallback {
        void onMergeStarted();
        void onMergeProgress(double progress);
        void onMergeCompleted(String outputPath);
        void onMergeError(String error);
        void onMergeLog(String logMessage);
    }
    
    public VideoSegmentMerger(List<String> segmentPaths, 
                            String outputPath, MergeCallback callback) {
        this.segmentPaths = new ArrayList<>(segmentPaths);
        this.outputPath = outputPath;
        this.callback = callback;
    }
    
    @Override
    protected String call() throws Exception {
        try {
            logger.info("Starting video segment merge");
            logMessage("Video segment birleştirme işlemi başlatılıyor...");
            
            if (segmentPaths.isEmpty()) {
                throw new IllegalArgumentException("No segments to merge");
            }
            
            // Toplam süreyi hesapla
            calculateTotalDuration();
            
            if (callback != null) {
                Platform.runLater(() -> callback.onMergeStarted());
            }
            
            // Geçici concat dosyası oluştur
            String concatFilePath = createConcatFile();
            
            try {
                // FFmpeg ile birleştirme işlemi
                String finalOutputPath = mergeSegments(concatFilePath);
                
                logMessage("Video segment birleştirme işlemi tamamlandı!");
                logger.info("Video segments merged successfully: {}", finalOutputPath);
                
                if (callback != null) {
                    Platform.runLater(() -> callback.onMergeCompleted(finalOutputPath));
                }
                
                return finalOutputPath;
                
            } finally {
                // Geçici dosyayı temizle
                cleanupTempFile(concatFilePath);
            }
            
        } catch (Exception e) {
            String errorMsg = "Video birleştirme hatası: " + e.getMessage();
            logger.error("Error merging video segments", e);
            logMessage(errorMsg);
            
            if (callback != null) {
                Platform.runLater(() -> callback.onMergeError(errorMsg));
            }
            
            throw e;
        }
    }
    
    /**
     * Toplam süreyi hesaplar
     */
    private void calculateTotalDuration() {
        // Süre tahmini: dosya adından hesaplayamıyoruz basitçe parça sayısına göre tahmin etmiyoruz.
        // İstenirse burada ffprobe ile süre toplanabilir şimdilik toplam süreyi bilinmiyor şeklinde
        totalDuration = 0;
        
        logger.info("Total duration to merge: {} seconds", totalDuration);
        logMessage(String.format("Toplam birleştirilecek süre: %.1f saniye", totalDuration));
    }
    
    /**
     * FFmpeg concat dosyası oluşturur
     */
    private String createConcatFile() throws IOException {
        // Geçici concat dosyası
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path concatFilePath = tempDir.resolve("concat_" + timestamp + ".txt");
        
        logger.info("Creating concat file: {}", concatFilePath);
        logMessage("Geçici birleştirme dosyası oluşturuluyor...");
        
        int added = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(concatFilePath, java.nio.charset.StandardCharsets.UTF_8)) {
            for (String segmentFullPath : segmentPaths) {
                // Dosya yolunu kontrol et
                Path segmentPath = Paths.get(segmentFullPath);
                if (!Files.exists(segmentPath)) {
                    throw new FileNotFoundException("Segment file not found: " + segmentFullPath);
                }
                
                // Dosya hala yazılıyor olabilir; boyut stabilize olana kadar bekle,kontrol et
                // Ayrıca MP4 dosyası için moov atom kontrolü yap
                try {
                    long size1 = Files.size(segmentPath);
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    long size2 = Files.size(segmentPath);
                    
                    // Dosya boyutu büyüyorsa hala yazılıyor
                    if (size2 > size1) {
                        logger.warn("Segment still growing, skipping: {} ({} -> {} bytes)", segmentFullPath, size1, size2);
                        logMessage("Segment dosyası henüz yazılıyor, atlanıyor: " + segmentPath.getFileName());
                        continue;
                    }
                    
                    // Dosya boyutu çok küçükse geçersiz (minimum 10KB)
                    if (size2 < 10240) { // 10KB
                        logger.warn("Segment file too small ({} bytes), skipping: {}", size2, segmentFullPath);
                        logMessage("Segment dosyası çok küçük (" + size2 + " bytes), atlanıyor: " + segmentPath.getFileName());
                        continue;
                    }
                    
                    // MP4 segmentler için basit boyut kontrolü
                    if (segmentFullPath.toLowerCase().endsWith(".mp4")) {
                        logger.debug("MP4 segment found, using basic validation: {}", segmentPath.getFileName());
                    }
                    
                } catch (IOException io) {
                    logger.warn("Couldn't stat segment file, skipping: {}", segmentFullPath, io);
                    logMessage("Segment dosyası okunamıyor, atlanıyor: " + segmentPath.getFileName());
                    continue;
                }
                
                // FFmpeg concat format: file 'path/to/file.mp4'
                writer.write("file '" + segmentFullPath.replace("\\", "/") + "'");
                writer.newLine();
                added++;
                
                logger.debug("Added to concat file: {}", segmentFullPath);
            }
        }
        
        if (added == 0) {
            throw new IOException("Uygun segment bulunamadı. Lütfen segmentlerin tamamlandığından emin olun ve tekrar deneyin.");
        }
        
        logMessage("Concat dosyası oluşturuldu: " + added + " segment");
        return concatFilePath.toString();
    }
    
    /**
     * FFmpeg ile segmentleri birleştirir
     */
    private String mergeSegments(String concatFilePath) throws IOException, InterruptedException {
        // Çıkış dosyası yolunu ayarla
        String finalOutputPath = ensureOutputPath();
        
        // Çıkış formatını MP4e zorla (daha iyi uyumluluk için)
        if (!finalOutputPath.toLowerCase().endsWith(".mp4")) {
            String base = getBaseName(finalOutputPath);
            finalOutputPath = base + ".mp4";
        }
        
        // GÜVENLİ ÇÖZÜM: MP4 → MP4 RE-ENCODE (seek ve timestamp sorunlarını çözümle)
        // NOT: Re-encode kullanıyor, keyframe optimizasyonu ile seek desteği
        logger.info("=== BAŞLATILIYOR: MP4 → MP4 RE-ENCODE (SEEK OPTİMİZASYONU İÇİN) ===");
        List<String> commandReencode = buildMp4CopyCommand(concatFilePath, finalOutputPath);
        logger.info("FFmpeg MP4 seekable re-encode command: {}", String.join(" ", commandReencode));
        logMessage("=== FFmpeg MP4 → MP4 re-encode komutu çalıştırılıyor (seek desteği için) ===");

        int exitCode = runFfmpegAndWait(commandReencode);
        logger.info("=== MP4 SEEKABLE RE-ENCODE BİTTİ, EXIT CODE: {} ===", exitCode);
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg exited with code: " + exitCode);
        }
        
        // Çıkış dosyasının oluştuğunu kontrol et
        Path outputFile = Paths.get(finalOutputPath);
        if (!Files.exists(outputFile)) {
            throw new RuntimeException("Output file was not created: " + finalOutputPath);
        }
        
        long outputSize = Files.size(outputFile);
        logMessage(String.format("Birleştirme tamamlandı! Dosya boyutu: %s", formatFileSize(outputSize)));
        
        // BİRLEŞTİRME BAŞARILI - SEGMENTLERİ SİL
        deleteSegmentFiles(segmentPaths);
        
        return finalOutputPath;
    }
    
    /**
     * Çıkış dosyası yolunu düzenler ve kontrol eder
     */
    private String ensureOutputPath() throws IOException {
        Path outputPath = Paths.get(this.outputPath);
        
        logger.info("=== ÇIKIŞ DOSYASI YOL DÜZENLEMESİ ===");
        logger.info("Gelen output path: {}", this.outputPath);
        logMessage("Çıkış dosyası yol düzenlemesi başlatılıyor: " + this.outputPath);
        
        // Dosya uzantısını kontrol et ve düzelt
        String fileName = outputPath.getFileName().toString();
        logger.info("Dosya ismi: {}", fileName);
        
        if (!fileName.contains(".")) {
            // Uzantı yoksa ilk segmentin formatını kullan
            String firstSegmentPath = segmentPaths.get(0);
            String extension = getFileExtension(firstSegmentPath);
            fileName += "." + extension;
            outputPath = outputPath.getParent().resolve(fileName);
            logger.info("Uzantı eklendi, yeni dosya ismi: {}", fileName);
            logMessage("Dosya uzantısı eklendi: " + extension);
        }
        
        // Çıkış klasörünü oluştur
        Path parentDir = outputPath.getParent();
        logger.info("Hedef klasör: {}", parentDir);
        logMessage("Hedef klasör: " + parentDir);
        
        if (parentDir != null && !Files.exists(parentDir)) {
            logger.info("Klasör mevcut değil, oluşturuluyor: {}", parentDir);
            logMessage("Klasör oluşturuluyor: " + parentDir);
            Files.createDirectories(parentDir);
            logger.info("Klasör başarıyla oluşturuldu");
            logMessage("Klasör başarıyla oluşturuldu");
        } else {
            logger.info("Klasör zaten mevcut: {}", parentDir);
            logMessage("Klasör zaten mevcut");
        }
        
        // Dosya varsa, yeni isim oluştur
        String finalPath = outputPath.toString();
        logger.info("Başlangıç dosya yolu: {}", finalPath);
        
        int counter = 1;
        while (Files.exists(Paths.get(finalPath))) {
            String baseName = getBaseName(outputPath.toString());
            String extension = getFileExtension(outputPath.toString());
            finalPath = baseName + "_" + counter + "." + extension;
            counter++;
            logger.info("Dosya mevcut, yeni isim deneniyor: {}", finalPath);
            logMessage("Dosya mevcut, yeni isim: " + Paths.get(finalPath).getFileName());
        }
        
        logger.info("Final output path: {}", finalPath);
        logMessage("Son çıkış dosyası yolu: " + finalPath);
        
        return finalPath;
    }
    
    /**
     * MP4 → MP4 copy komutu - güvenli birleştirme ve seek optimizasyonu
     */
    private List<String> buildMp4CopyCommand(String concatFilePath, String outputPath) {
        List<String> command = new ArrayList<>();
        
        // FFmpegin tam yolunu kullan
        String ffmpegExe = "ffmpeg";
        try {
            FFmpegService svc = new FFmpegService();
            String p = svc.getFfmpegPath();
            if (p != null && !p.isBlank()) {
                ffmpegExe = p;
            }
        } catch (Exception ignored) {}

        command.add(ffmpegExe);
        command.add("-y"); // Dosya üzerine yaz
        command.add("-f");
        command.add("concat");
        command.add("-safe");
        command.add("0");
        command.add("-i");
        command.add(concatFilePath);
        
        // Segment video uyumluluğu için RE-ENCODE (copy yerine güvenli çözüm)
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("medium");
        command.add("-crf");
        command.add("20"); // İyi kalite
        command.add("-pix_fmt");
        command.add("yuv420p");
        
        // Audio codec - uyumlu codec
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");
        command.add("-ar");
        command.add("44100");
        
        // MP4 format optimizasyonları - seek için kritik!
        command.add("-f");
        command.add("mp4");
        command.add("-movflags");
        command.add("+faststart+frag_keyframe+empty_moov+default_base_moof");
        
        // Seek optimizasyonu için KEYFRAME ayarları - çok önemli!
        command.add("-g");
        command.add("30"); // Her 30 framede bir keyframe (1 saniye @ 30fps)
        command.add("-keyint_min");
        command.add("15"); // Minimum keyframe interval
        command.add("-sc_threshold");
        command.add("0"); // Scene change detection kapalı - düzenli keyframeler
        
        // Timestamp ve frame düzeltmeleri - çok önemli!
        command.add("-avoid_negative_ts");
        command.add("make_zero");
        command.add("-fflags");
        command.add("+genpts+discardcorrupt");
        command.add("-vsync");
        command.add("1"); // Constant frame rate - seek için gerekli
        
        // MP4 indexing ve seek optimizasyonu
        command.add("-strict");
        command.add("-2"); // Experimental features için
        command.add("-max_muxing_queue_size");
        command.add("1024"); // Büyük queue size
        
        // Metadata ve header güvenliği
        command.add("-map_metadata");
        command.add("0"); // İlk dosyadan metadatayı kopyala
        command.add("-metadata");
        command.add("title=MediaShift Merged Video");
        command.add("-metadata");
        command.add("comment=Seekable merged segments with keyframe optimization");
        
        // Çıkış dosyası
        command.add(outputPath);
        
        return command;
    }
    
    /**
     * FFmpeg komutunu oluşturur (kullanılmıyor artık)
     */
    private List<String> buildFFmpegCommand(String concatFilePath, String outputPath, boolean reencode) {
        List<String> command = new ArrayList<>();
        
        // FFmpegin tam yolunu kullan
        String ffmpegExe = "ffmpeg";
        try {
            FFmpegService svc = new FFmpegService();
            String p = svc.getFfmpegPath();
            if (p != null && !p.isBlank()) {
                ffmpegExe = p;
            }
        } catch (Exception ignored) {}

        command.add(ffmpegExe);
        command.add("-y"); // Dosya üzerine yaz
        command.add("-f");
        command.add("concat");
        command.add("-safe");
        command.add("0");
        command.add("-i");
        command.add(concatFilePath);
        // Basit input ayarları
        command.add("-avoid_negative_ts");
        command.add("make_zero");
        
        if (reencode) {
            // BASİT VE GÜVENİLİR AYARLAR
            command.add("-c:v");
            command.add("libx264");
            command.add("-preset");
            command.add("medium"); // Güvenilir preset
            command.add("-crf");
            command.add("18"); // Yüksek kalite
            command.add("-pix_fmt");
            command.add("yuv420p");
            
            // Audio - AAC codec
            command.add("-c:a");
            command.add("aac");
            command.add("-b:a");
            command.add("128k");
            command.add("-ar");
            command.add("44100");
            
            // MP4 format
            command.add("-f");
            command.add("mp4");
            command.add("-movflags");
            command.add("+faststart");
        } else {
            // Copy modu - kullanılmıyor (güvenilirlik için her zaman re-encode)
            throw new IllegalArgumentException("Copy mode disabled for reliability");
        }
        
        // Metadata ekle
        command.add("-metadata");
        command.add("title=MediaShift Merged Video");
        command.add("-metadata");
        command.add("comment=Merged from " + segmentPaths.size() + " segments");
        command.add("-metadata");
        command.add("creation_time=" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Progress tracking için
        command.add("-progress");
        command.add("pipe:1");
        
        command.add(outputPath);
        
        return command;
    }
    
    /**
     * Birleştirme başarılı olduktan sonra segment dosyalarını sil
     */
    private void deleteSegmentFiles(List<String> segmentPaths) {
        logger.info("=== SEGMENT FİLE DELETE BAŞLADI ===");
        logMessage("📁 Birleştirme başarılı - segment dosyaları siliniyor...");
        
        int deletedCount = 0;
        int totalCount = segmentPaths.size();
        
        for (String segmentPath : segmentPaths) {
            try {
                File segmentFile = new File(segmentPath);
                if (segmentFile.exists()) {
                    String fileName = segmentFile.getName();
                    
                    // Dosyayı silmeyi 3 kez dene (dosya kilitli olabilir)
                    boolean deleted = false;
                    for (int attempt = 1; attempt <= 3; attempt++) {
                        if (segmentFile.delete()) {
                            deleted = true;
                            deletedCount++;
                            logger.info("Segment file deleted: {}", fileName);
                            logMessage("✅ Silindi: " + fileName);
                            break;
                        } else {
                            logger.warn("Delete attempt {} failed for: {}", attempt, fileName);
                            if (attempt < 3) {
                                try {
                                    Thread.sleep(500); // 0.5 saniye bekle
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                    
                    if (!deleted) {
                        logger.error("Failed to delete segment file after 3 attempts: {}", fileName);
                        logMessage("⚠️ Silinemedi (dosya kullanımda): " + fileName);
                    }
                    
                } else {
                    logger.warn("Segment file not found (already deleted?): {}", segmentPath);
                }
                
            } catch (Exception e) {
                logger.error("Error deleting segment file: {}", segmentPath, e);
                logMessage("❌ Silme hatası: " + new File(segmentPath).getName());
            }
        }
        
        logger.info("=== SEGMENT FİLE DELETE BİTTİ: {}/{} silindi ===", deletedCount, totalCount);
        logMessage(String.format("📁 Segment temizleme tamamlandı: %d/%d dosya silindi", deletedCount, totalCount));
        
        if (deletedCount == totalCount) {
            logMessage("🎉 Tüm segment dosyaları başarıyla silindi - sadece birleşmiş video kaldı!");
        }
    }
    
    private int runFfmpegAndWait(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        // Debug: FFmpeg komutunu ve çalışma dizinini logla
        logger.info("Full FFmpeg command: {}", String.join(" ", command));
        logger.info("Working directory: {}", System.getProperty("user.dir"));
        logMessage("FFmpeg komutu çalıştırılıyor...");
        
        ffmpegProcess = processBuilder.start();
        
        // FFmpeg çıktısını yakalayıp logla
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null && !isCancelled()) {
                logger.debug("FFmpeg output: {}", line);
                errorOutput.append(line).append("\n");
                
                // Hata mesajlarını özellikle logla
                if (line.toLowerCase().contains("error") || 
                    line.toLowerCase().contains("invalid") ||
                    line.toLowerCase().contains("failed") ||
                    line.toLowerCase().contains("no such file") ||
                    line.toLowerCase().contains("moov atom not found") ||
                    line.toLowerCase().contains("does not contain any stream")) {
                    logger.error("FFmpeg error detected: {}", line);
                    logMessage("FFmpeg hata: " + line);
                }
                
                // Progress güncelleme
                if (line.contains("out_time=") || line.contains("progress=")) {
                    parseProgressLine(line);
                }
            }
        }
        
        int exitCode = ffmpegProcess.waitFor();
        logger.info("FFmpeg process finished with exit code: {}", exitCode);
        
        if (exitCode != 0) {
            logger.error("FFmpeg failed! Full output:\n{}", errorOutput.toString());
            logMessage("FFmpeg başarısız oldu, tam çıktı log'a kaydedildi.");
        }
        
        return exitCode;
    }
    
    /**
     * Progress satırını parse eder
     */
    private void parseProgressLine(String line) {
        Pattern timePattern = Pattern.compile("out_time=(\\d+):(\\d+):(\\d+)\\.(\\d+)");
        Pattern progressPattern = Pattern.compile("progress=(\\w+)");
                
                // Progress güncelleme
                Matcher timeMatcher = timePattern.matcher(line);
                if (timeMatcher.find()) {
                    int hours = Integer.parseInt(timeMatcher.group(1));
                    int minutes = Integer.parseInt(timeMatcher.group(2));
                    int seconds = Integer.parseInt(timeMatcher.group(3));
                    int milliseconds = Integer.parseInt(timeMatcher.group(4));
                    
                    double currentTime = hours * 3600 + minutes * 60 + seconds + milliseconds / 1000.0;
            currentProgress = Math.min((currentTime / Math.max(totalDuration, 1)) * 100, 100);
                    
                    updateProgress(currentProgress, 100);
                    
                    if (callback != null) {
                        Platform.runLater(() -> callback.onMergeProgress(currentProgress));
                    }
                }
                
                // Progress durumu
                Matcher progressMatcher = progressPattern.matcher(line);
                if (progressMatcher.find()) {
                    String progress = progressMatcher.group(1);
                    if ("end".equals(progress)) {
                        currentProgress = 100;
                        updateProgress(100, 100);
                        
                        if (callback != null) {
                            Platform.runLater(() -> callback.onMergeProgress(100));
                        }
                    }
                }
            }

    /**
     * FFmpeg progressini izler (deprecated - artık runFfmpegAndWait içinde yapılıyor)
     */
    private void monitorFFmpegProgress() {
        // Bu metod artık kullanılmıyor - progress monitoring runFfmpegAndWait içinde yapılıyor
    }
    
    /**
     * Geçici dosyayı temizler
     */
    private void cleanupTempFile(String concatFilePath) {
        try {
            Path tempFile = Paths.get(concatFilePath);
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
                logger.debug("Cleaned up temp file: {}", concatFilePath);
            }
        } catch (IOException e) {
            logger.warn("Error cleaning up temp file: {}", concatFilePath, e);
        }
    }
    
    /**
     * Dosya uzantısını alır
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "mp4"; // varsayılan
    }
    
    /**
     * Dosya adının base kısmını alır (uzantı hariç)
     */
    private String getBaseName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }
    
    /**
     * Dosya boyutunu formatlar
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Log mesajı gönderir
     */
    private void logMessage(String message) {
        if (callback != null) {
            Platform.runLater(() -> callback.onMergeLog(message));
        }
    }
    
    /**
     * İptal işlemi
     */
    @Override
    protected void cancelled() {
        logger.info("Video merge cancelled");
        
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegProcess.destroyForcibly();
        }
        
        logMessage("Video birleştirme işlemi iptal edildi");
        
        if (callback != null) {
            Platform.runLater(() -> callback.onMergeError("Merge operation cancelled"));
        }
    }
    
    /**
     * Mevcut progressi döndürür
     */
    public double getCurrentProgress() {
        return currentProgress;
    }
    
    /**
     * Toplam süreyi döndürür
     */
    public double getTotalDuration() {
        return totalDuration;
    }
}

