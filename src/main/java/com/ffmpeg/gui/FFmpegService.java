package com.ffmpeg.gui;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class FFmpegService {
    
    private static final Logger logger = LoggerFactory.getLogger(FFmpegService.class);
    
    private FFmpeg ffmpeg;
    private FFprobe ffprobe;
    private FFmpegExecutor executor;
    private ExecutorService executorService;
    
    private String ffmpegPath;
    private String ffprobePath;
    private int maxThreads;
    private boolean detailedLogging = false; // Detaylı loglama özelliği
    
    public FFmpegService() {
        this.maxThreads = Math.min(Runtime.getRuntime().availableProcessors(), 8); // Maksimum 8 thread
        this.executorService = Executors.newFixedThreadPool(maxThreads);
        initializeFFmpeg();
    }
    
    private void initializeFFmpeg() {
        try {
            ffmpegPath = ffmpegOtomatikBul();
            if (ffmpegPath == null) {
                logger.warn("FFmpeg otomatik olarak bulunamadı, varsayılan yollar deneniyor...");
                ffmpegPath = "ffmpeg";
            }
            
            // FFprobe pathini düzgün şekilde ayarla
            if (ffmpegPath.equals("ffmpeg")) {
                ffprobePath = "ffprobe";
            } else {
                ffprobePath = ffmpegPath.replace("ffmpeg", "ffprobe");
                // Eğer ffprobe bulunamazsa, PATH'ten dene
                if (!new File(ffprobePath).exists()) {
                    ffprobePath = "ffprobe";
                    logger.warn("FFprobe bulunamadı, PATH'ten deneniyor: {}", ffprobePath);
                }
            }
            
            // FFmpeg ve FFprobe'un çalışıp çalışmadığını test et
            if (!ffmpegKullanilabilirMi(ffmpegPath)) {
                throw new IOException("FFmpeg çalıştırılamıyor: " + ffmpegPath);
            }
            
            if (!ffmpegKullanilabilirMi(ffprobePath)) {
                throw new IOException("FFprobe çalıştırılamıyor: " + ffprobePath);
            }
            
            ffmpeg = new FFmpeg(ffmpegPath);
            ffprobe = new FFprobe(ffprobePath);
            executor = new FFmpegExecutor(ffmpeg, ffprobe);
            
            logger.info("FFmpeg started successfully: {}", ffmpegPath);
            logger.info("FFprobe started successfully: {}", ffprobePath);
            
        } catch (IOException e) {
            logger.error("FFmpeg başlatılırken hata oluştu", e);
            ffmpeg = null;
            ffprobe = null;
            executor = null;
        }
    }
    
    public String ffmpegOtomatikBul() {
        String[] possiblePaths = {
            // Winget ile kurulan FFmpeg
            System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\\ffmpeg-7.1.1-full_build\\bin\\ffmpeg.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\\ffmpeg-7.0-full_build\\bin\\ffmpeg.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\\ffmpeg-6.1-full_build\\bin\\ffmpeg.exe",
            
            // Chocolatey ile kurulan FFmpeg
            "C:\\ProgramData\\chocolatey\\bin\\ffmpeg.exe",
            
            // Standart kurulum yolları
            "C:\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\Program Files (x86)\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\Program Files\\FFmpeg\\bin\\ffmpeg.exe",
            "C:\\Program Files (x86)\\FFmpeg\\bin\\ffmpeg.exe",
            
            // Kullanıcı klasörü
            System.getProperty("user.home") + "\\ffmpeg\\bin\\ffmpeg.exe",
            System.getProperty("user.home") + "\\Desktop\\ffmpeg\\bin\\ffmpeg.exe",
            System.getProperty("user.home") + "\\Downloads\\ffmpeg\\bin\\ffmpeg.exe",
            
            // PATH'ten bul
            "ffmpeg"
        };
        
        for (String path : possiblePaths) {
            if (ffmpegKullanilabilirMi(path)) {
                logger.info("FFmpeg found: {}", path);
                return path;
            }
        }
        
        return null;
    }
    
    public String autoDetectFFmpeg() {
        return ffmpegOtomatikBul();
    }
    
    private String temizleDosyaAdi(String dosyaAdi) {
        if (dosyaAdi == null) return "";
        
        // Dosya yolunu ve adını ayır
        File file = new File(dosyaAdi);
        String parentPath = file.getParent();
        String fileName = file.getName();
        
        // Sadece dosya adındaki Türkçe karakterleri temizle
        String temizlenmisAd = fileName
            .replace("ç", "c").replace("Ç", "C")
            .replace("ğ", "g").replace("Ğ", "G")
            .replace("ı", "i").replace("I", "I")
            .replace("ö", "o").replace("Ö", "O")
            .replace("ş", "s").replace("Ş", "S")
            .replace("ü", "u").replace("Ü", "U")
            .replace("İ", "I");
        
        // Dosya adındaki özel karakterleri temizle (sadece dosya adı)
        temizlenmisAd = temizlenmisAd.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Birden fazla alt çizgiyi tek alt çizgiye çevir
        temizlenmisAd = temizlenmisAd.replaceAll("_+", "_");
        
        // Başındaki ve sonundaki alt çizgileri kaldır
        temizlenmisAd = temizlenmisAd.replaceAll("^_+|_+$", "");
        
        // Temizlenmiş dosya adını orijinal yol ile birleştir
        if (parentPath != null) {
            return new File(parentPath, temizlenmisAd).getPath();
        } else {
            return temizlenmisAd;
        }
    }
    
    private boolean ffmpegKullanilabilirMi(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    public FFmpegProbeResult videoBilgisiAl(String inputPath) throws IOException {
        if (ffprobe == null) {
            throw new IOException("FFprobe başlatılmamış");
        }
        
        return ffprobe.probe(inputPath);
    }
    
    public CompletableFuture<Void> videoDonustur(VideoConversionParams params, ProgressCallback callback) {
        return convertVideo(params, callback);
    }
    
    public CompletableFuture<Void> convertVideo(VideoConversionParams params, ProgressCallback callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (executor == null) {
                    throw new RuntimeException("FFmpeg executor başlatılmamış. FFmpeg kurulu olduğundan emin olun.");
                }
                
                // Giriş dosyasının varlığını kontrol et
                File inputFile = new File(params.getInputPath());
                if (!inputFile.exists()) {
                    throw new IOException("Giriş dosyası bulunamadı: " + params.getInputPath());
                }
                
                String outputPath = params.getOutputPath();
                // Formatı küçük harfe çevir ve Türkçe karakterleri temizle
                String format = params.getFormat().toLowerCase()
                    .replace("ı", "i")
                    .replace("ğ", "g")
                    .replace("ü", "u")
                    .replace("ş", "s")
                    .replace("ö", "o")
                    .replace("ç", "c");
                
                // Dosya adındaki Türkçe karakterleri temizle
                String cleanOutputPath = temizleDosyaAdi(outputPath);
                
                // Temizlenmiş dosya yolunu kullan
                outputPath = cleanOutputPath;
                
                // Eğer dosya uzantısı yoksa veya yanlışsa, doğru uzantıyı ekle
                if (!outputPath.toLowerCase().endsWith("." + format)) {
                    String basePath = outputPath;
                    if (basePath.contains(".")) {
                        basePath = basePath.substring(0, basePath.lastIndexOf('.'));
                    }
                    outputPath = basePath + "." + format;
                }
                
                // Çıkış klasörünün varlığını kontrol et ve oluştur
                File outputFile = new File(outputPath);
                File outputDir = outputFile.getParentFile();
                if (outputDir != null && !outputDir.exists()) {
                    outputDir.mkdirs();
                }
                
                String videoCodec = formatIcinVideoCodecBul(format, params.getCodec());
                String audioCodec = formatIcinAudioCodecBul(format);
                
                // FFmpeg builderı oluştur
                FFmpegBuilder builder = new FFmpegBuilder();
                builder.setInput(params.getInputPath());
                builder.overrideOutputFiles(true);
                
                // Output builderı oluştur ve ayarları ekle
                FFmpegOutputBuilder outputBuilder = builder.addOutput(outputPath);
                outputBuilder.setVideoCodec(videoCodec);
                outputBuilder.setAudioCodec(audioCodec);
                outputBuilder.setVideoResolution(params.getWidth(), params.getHeight());
                outputBuilder.setVideoFrameRate(params.getFps());
                
                // Codece göre sıkıştırma ayarları
                if (videoCodec.equals("libx264")) {
                    // H.264 için CRF (Constant Rate Factor) kullan - daha iyi kalite/sıkıştırma oranı
                    int crf = Math.max(18, Math.min(28, 23)); // 18-28 arası, 23 varsayılan (düşük = daha iyi kalite)
                    outputBuilder.addExtraArgs("-crf", String.valueOf(crf));
                    outputBuilder.addExtraArgs("-preset", "medium"); // Sıkıştırma hızı: ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow
                    outputBuilder.addExtraArgs("-tune", "film"); // Optimizasyon: film, animation, grain, stillimage, fastdecode, zerolatency
                    
                    // Maksimum bitrate sınırı (bitrate kontrolü için)
                    if (params.getBitrate() > 0) {
                        outputBuilder.addExtraArgs("-maxrate", params.getBitrate() + "k");
                        outputBuilder.addExtraArgs("-bufsize", (params.getBitrate() * 2) + "k");
                    }
                } else if (videoCodec.equals("libx265")) {
                    // H.265 için CRF kullan
                    int crf = Math.max(20, Math.min(30, 25)); // H.265 için 20-30 arası, 25 varsayılan
                    outputBuilder.addExtraArgs("-crf", String.valueOf(crf));
                    outputBuilder.addExtraArgs("-preset", "medium");
                    
                    // Maksimum bitrate sınırı
                    if (params.getBitrate() > 0) {
                        outputBuilder.addExtraArgs("-maxrate", params.getBitrate() + "k");
                        outputBuilder.addExtraArgs("-bufsize", (params.getBitrate() * 2) + "k");
                    }
                } else if (videoCodec.equals("libvpx-vp9")) {
                    // VP9 için CRF kullan
                    int crf = Math.max(20, Math.min(32, 25));
                    outputBuilder.addExtraArgs("-crf", String.valueOf(crf));
                    outputBuilder.addExtraArgs("-b:v", "0"); // VP9 için bitrate 0 olmalı CRF ile kullanılırken
                    outputBuilder.addExtraArgs("-deadline", "good"); // Sıkıştırma kalitesi: best, good, realtime
                    outputBuilder.addExtraArgs("-cpu-used", "2"); // CPU kullanımı: 0-5 arası, düşük = daha iyi kalite
                } else {
                    // Diğer codecler için bitrate kullan
                    if (params.getBitrate() > 0) {
                        outputBuilder.addExtraArgs("-b:v", params.getBitrate() + "k");
                    }
                }
                
                // Audio bitrate ayarları
                if (params.getBitrate() > 0) {
                    // Video bitrateinin %10'u kadar audio bitrate
                    int audioBitrate = Math.max(64, params.getBitrate() / 10);
                    outputBuilder.addExtraArgs("-b:a", audioBitrate + "k");
                }
                
                // Genel sıkıştırma optimizasyonları
                outputBuilder.addExtraArgs("-movflags", "+faststart");             // Web streaming için optimize
                outputBuilder.addExtraArgs("-g", "30");                           // GOP (Group of Pictures) boyutu
                outputBuilder.addExtraArgs("-keyint_min", "25");                 // Minimum keyframe aralığı
                outputBuilder.addExtraArgs("-sc_threshold", "0");               // Scene change detectionı kapat
                outputBuilder.addExtraArgs("-avoid_negative_ts", "make_zero"); // Timestamp sorunlarını önle
                
                logger.info("FFmpeg command created: {}", builder.toString());
                logger.info("Input file: {}", params.getInputPath());
                logger.info("Output file: {}", outputPath);
                logger.info("Selected format: {}", format);
                logger.info("Video codec: {}", videoCodec);
                logger.info("Audio codec: {}", audioCodec);
                logger.info("Bitrate: {} kbps", params.getBitrate());
                
                // Detaylı loglama için FFmpeg listener ekle
                if (detailedLogging) {
                    logger.info("Detailed logging enabled - FFmpeg terminal output will be logged");
                    logger.info("FFmpeg command: {}", builder.toString());
                }
                
                executor.createJob(builder, progress -> {
                    // out_time_ns nanosecond cinsinden geliyor, saniyeye çevir
                    double currentTime = progress.out_time_ns / 1000000000.0;
                    if (currentTime > 0) {
                        callback.onProgress(currentTime);
                    }
                }).run();
                
                logger.info("Video conversion completed: {}", outputPath);
                
            } catch (Exception e) {
                logger.error("Video dönüştürme hatası", e);
                throw new RuntimeException("Video dönüştürme başarısız: " + e.getMessage(), e);
            }
        }, executorService);
    }
    
    public CompletableFuture<Void> audioDonustur(AudioConversionParams params, ProgressCallback callback) {
        return convertAudio(params, callback);
    }
    
    public CompletableFuture<Void> convertAudio(AudioConversionParams params, ProgressCallback callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (executor == null) {
                    throw new RuntimeException("FFmpeg executor başlatılmamış. FFmpeg kurulu olduğundan emin olun.");
                }
                
                // Giriş dosyasının varlığını kontrol et
                File inputFile = new File(params.getInputPath());
                if (!inputFile.exists()) {
                    throw new IOException("Giriş dosyası bulunamadı: " + params.getInputPath());
                }
                
                String outputPath = params.getOutputPath();
                // Formatı küçük harfe çevir ve Türkçe karakterleri temizle
                String format = params.getFormat().toLowerCase()
                    .replace("ı", "i")
                    .replace("ğ", "g")
                    .replace("ü", "u")
                    .replace("ş", "s")
                    .replace("ö", "o")
                    .replace("ç", "c");
                
                // Dosya adındaki Türkçe karakterleri temizle
                String cleanOutputPath = temizleDosyaAdi(outputPath);
                
                // Temizlenmiş dosya yolunu kullan
                outputPath = cleanOutputPath;
                
                // Eğer dosya uzantısı yoksa veya yanlışsa, doğru uzantıyı ekle
                if (!outputPath.toLowerCase().endsWith("." + format)) {
                    String basePath = outputPath;
                    if (basePath.contains(".")) {
                        basePath = basePath.substring(0, basePath.lastIndexOf('.'));
                    }
                    outputPath = basePath + "." + format;
                }
                
                // Çıkış klasörünün varlığını kontrol et ve oluştur
                File outputFile = new File(outputPath);
                File outputDir = outputFile.getParentFile();
                if (outputDir != null && !outputDir.exists()) {
                    outputDir.mkdirs();
                }
                
                String audioCodec = formatIcinAudioCodecBul(format);
                
                // FFmpeg builderı oluştur
                FFmpegBuilder builder = new FFmpegBuilder();
                builder.setInput(params.getInputPath());
                builder.overrideOutputFiles(true);
                
                // Output builderı oluştur ve ayarları ekle
                FFmpegOutputBuilder outputBuilder = builder.addOutput(outputPath);
                outputBuilder.setAudioCodec(audioCodec);
                outputBuilder.setAudioSampleRate(params.getSampleRate());
                outputBuilder.setAudioChannels(params.getChannels());
                
                // Codece göre sıkıştırma ayarları
                if (audioCodec.equals("libmp3lame")) {
                    // MP3 için VBR (Variable Bit Rate) kullan - daha iyi kalite/sıkıştırma oranı
                    int quality = Math.max(0, Math.min(9, 5)); // 0-9 arası, 0 = en iyi kalite, 9 = en kötü kalite
                    outputBuilder.addExtraArgs("-q:a", String.valueOf(quality));
                    
                    // Maksimum bitrate sınırı
                    if (params.getBitrate() > 0) {
                        outputBuilder.addExtraArgs("-b:a", params.getBitrate() + "k");
                    }
                } else if (audioCodec.equals("aac")) {
                    // AAC için VBR kullan
                    int quality = Math.max(1, Math.min(5, 3)); // 1-5 arası, 1 = en iyi kalite, 5 = en kötü kalite
                    outputBuilder.addExtraArgs("-q:a", String.valueOf(quality));
                    
                    // Maksimum bitrate sınırı
                    if (params.getBitrate() > 0) {
                        outputBuilder.addExtraArgs("-b:a", params.getBitrate() + "k");
                    }
                } else if (audioCodec.equals("libvorbis")) {
                    // OGG Vorbis için VBR kullan
                    int quality = Math.max(-1, Math.min(10, 5)); // -1-10 arası, -1 = en iyi kalite, 10 = en kötü kalite
                    outputBuilder.addExtraArgs("-q:a", String.valueOf(quality));
                    
                    // Maksimum bitrate sınırı
                    if (params.getBitrate() > 0) {
                        outputBuilder.addExtraArgs("-b:a", params.getBitrate() + "k");
                    }
                } else if (audioCodec.equals("flac")) {
                    // FLAC için lossless sıkıştırma seviyesi
                    int compression = Math.max(0, Math.min(8, 5)); // 0-8 arası, 0 = hızlı, 8 = en iyi sıkıştırma
                    outputBuilder.addExtraArgs("-compression_level", String.valueOf(compression));
                } else {
                    // Diğer codecler için bitrate kullan
                    if (params.getBitrate() > 0) {
                        outputBuilder.addExtraArgs("-b:a", params.getBitrate() + "k");
                    }
                }
                
                // Genel audio optimizasyonları
                outputBuilder.addExtraArgs("-ar", String.valueOf(params.getSampleRate())); // Sample rate
                outputBuilder.addExtraArgs("-ac", String.valueOf(params.getChannels())); // Channel sayısı
                outputBuilder.addExtraArgs("-avoid_negative_ts", "make_zero"); // Timestamp sorunlarını önle
                
                logger.info("FFmpeg command created: {}", builder.toString());
                logger.info("Input file: {}", params.getInputPath());
                logger.info("Output file: {}", outputPath);
                logger.info("Selected format: {}", format);
                logger.info("Audio codec: {}", audioCodec);
                logger.info("Bitrate: {} kbps", params.getBitrate());
                logger.info("Sample Rate: {} Hz", params.getSampleRate());
                logger.info("Channels: {}", params.getChannels());
                
                // Detaylı loglama için FFmpeg listener ekle
                if (detailedLogging) {
                    logger.info("Detailed logging enabled - FFmpeg terminal output will be logged");
                    logger.info("FFmpeg command: {}", builder.toString());
                }
                
                executor.createJob(builder, progress -> {
                    // out_time_ns nanosecond cinsinden geliyor, saniyeye çevir
                    double currentTime = progress.out_time_ns / 1000000000.0;
                    if (currentTime > 0) {
                        callback.onProgress(currentTime);
                    }
                }).run();
                
                logger.info("Audio conversion completed: {}", outputPath);
                
            } catch (Exception e) {
                logger.error("Audio dönüştürme hatası", e);
                throw new RuntimeException("Audio dönüştürme başarısız: " + e.getMessage(), e);
            }
        }, executorService);
    }
    
    private String formatIcinVideoCodecBul(String format, String requestedCodec) {
        String formatLower = format.toLowerCase();
        String codecLower = requestedCodec != null ? requestedCodec.toLowerCase() : "";
        
        if (codecLower.contains("h.265") || codecLower.contains("hevc")) {
            switch (formatLower) {
                case "mp4":
                case "mkv":
                case "mov":
                    return "libx265";
                case "avi":
                case "wmv":
                case "flv":
                case "webm":
                    logger.warn("H.265 codec'i {} formatında desteklenmiyor, H.264 kullanılıyor", format);
                    return "libx264";
                default:
                    return "libx264";
            }
        }
        
        switch (formatLower) {
            case "mp4":
                return "libx264";
            case "avi":
                return "libx264";
            case "mkv":
                return "libx264";
            case "mov":
                return "libx264";
            case "wmv":
                return "wmv2";
            case "flv":
                return "flv";
            case "webm":
                return "libvpx-vp9";
            default:
                return "libx264";
        }
    }
    
    private String formatIcinAudioCodecBul(String format) {
        switch (format.toLowerCase()) {
            case "mp3":
                return "libmp3lame";
            case "aac":
                return "aac";
            case "wav":
                return "pcm_s16le";
            case "flac":
                return "flac";
            case "ogg":
                return "libvorbis";
            case "wma":
                return "wmav2";
            default:
                return "aac";
        }
    }
    
    public String dosyaUzantisiAl(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0) {
            return filePath.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
    
    public FileType dosyaTuruBelirle(String filePath) {
        String extension = dosyaUzantisiAl(filePath).toLowerCase();
        
        if (extension.matches("(mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp|ogv)")) {
            return FileType.VIDEO;
        }
        
        if (extension.matches("(mp3|wav|flac|aac|ogg|wma|m4a|opus)")) {
            return FileType.AUDIO;
        }
        
        try {
            FFmpegProbeResult result = videoBilgisiAl(filePath);
            
            boolean hasVideo = result.getStreams().stream()
                .anyMatch(stream -> stream.codec_type == FFmpegStream.CodecType.VIDEO);
            
            boolean hasAudio = result.getStreams().stream()
                .anyMatch(stream -> stream.codec_type == FFmpegStream.CodecType.AUDIO);
            
            if (hasVideo) {
                return FileType.VIDEO;
            } else if (hasAudio) {
                return FileType.AUDIO;
            } else {
                return FileType.UNKNOWN;
            }
            
        } catch (Exception e) {
            logger.warn("FFprobe ile dosya türü belirlenemedi, uzantiya göre tespit edildi: {}", filePath);
            if (extension.matches("(mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp|ogv)")) {
                return FileType.VIDEO;
            } else if (extension.matches("(mp3|wav|flac|aac|ogg|wma|m4a|opus)")) {
                return FileType.AUDIO;
            }
            return FileType.UNKNOWN;
        }
    }
    
    public CompletableFuture<Void> topluDosyaIsle(java.util.List<File> files, String outputDir, 
                                                   BatchProcessingCallback callback) {
        // Varsayılan ayarlarla çağır
        BatchSettings defaultSettings = new BatchSettings();
        return processBatchFiles(files, outputDir, defaultSettings, callback);
    }
    
    public CompletableFuture<Void> processBatchFiles(java.util.List<File> files, String outputDir, 
                                                   BatchSettings batchSettings, BatchProcessingCallback callback) {
        return CompletableFuture.runAsync(() -> {
            int totalFiles = files.size();
            final int[] processedFiles = {0};
            final int[] failedFiles = {0};
            
            logger.info("Batch processing starting: {} files, output directory: {}", totalFiles, outputDir);
            
            // Büyük dosya listeleri için batch boyutu belirle
            int batchSize = Math.min(10, Math.max(1, totalFiles / 4)); // Maksimum 10 dosya, minimum 1
            logger.info("Batch size: {} files per batch", batchSize);
            
            // Dosyaları batchlere böl
            for (int i = 0; i < totalFiles; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalFiles);
                List<File> batch = files.subList(i, endIndex);
                
                logger.info("Processing batch {}/{}: {} files", (i / batchSize) + 1, 
                           (totalFiles + batchSize - 1) / batchSize, batch.size());
                
                // Her batch için CompletableFuture listesi oluştur
                List<CompletableFuture<Void>> batchFutures = new java.util.ArrayList<>();
                
                for (File file : batch) {
                    CompletableFuture<Void> fileFuture = CompletableFuture.runAsync(() -> {
                        try {
                            logger.info("Processing file: {} ({}/{})", file.getName(), processedFiles[0] + 1, totalFiles);
                            
                            FileType fileType = dosyaTuruBelirle(file.getAbsolutePath());
                            logger.info("File type detected: {} -> {}", file.getName(), fileType);
                            
                            String outputPath = cikisYoluOlustur(file, outputDir, fileType, batchSettings);
                            logger.info("Output path: {}", outputPath);
                            
                            if (fileType == FileType.VIDEO) {
                                logger.info("Video conversion starting: {}", file.getName());
                                VideoConversionParams params = new VideoConversionParams(
                                    file.getAbsolutePath(), outputPath, 
                                    batchSettings.getVideoFormat().toLowerCase(), 
                                    batchSettings.getVideoCodec(), 
                                    batchSettings.getVideoBitrate(), 
                                    batchSettings.getVideoWidth(), 
                                    batchSettings.getVideoHeight(), 
                                    batchSettings.getVideoFps()
                                );
                                
                                videoDonustur(params, new ProgressCallback() {
                                    @Override
                                    public void onProgress(double progress) {
                                        callback.onFileProgress(processedFiles[0], totalFiles, progress);
                                    }
                                }).get(30, java.util.concurrent.TimeUnit.MINUTES); // 30 dakika timeout
                                
                                logger.info("Video conversion completed: {}", file.getName());
                                
                            } else if (fileType == FileType.AUDIO) {
                                logger.info("Audio conversion starting: {}", file.getName());
                                AudioConversionParams params = new AudioConversionParams(
                                    file.getAbsolutePath(), outputPath, 
                                    batchSettings.getAudioFormat().toLowerCase(), 
                                    batchSettings.getAudioCodec(), 
                                    batchSettings.getAudioBitrate(), 
                                    batchSettings.getAudioSampleRate(), 
                                    batchSettings.getAudioChannels()
                                );
                                
                                audioDonustur(params, new ProgressCallback() {
                                    @Override
                                    public void onProgress(double progress) {
                                        callback.onFileProgress(processedFiles[0], totalFiles, progress);
                                    }
                                }).get(30, java.util.concurrent.TimeUnit.MINUTES); // 30 dakika timeout
                                
                                logger.info("Audio conversion completed: {}", file.getName());
                                
                            } else {
                                logger.warn("Unsupported file type: {}", file.getName());
                                callback.onFileError(file.getName(), "Unsupported file type");
                                failedFiles[0]++;
                                return;
                            }
                            
                            synchronized (processedFiles) {
                                processedFiles[0]++;
                            }
                            callback.onFileCompleted(file.getName());
                            logger.info("File processed successfully: {} ({}/{})", file.getName(), processedFiles[0], totalFiles);
                            
                        } catch (java.util.concurrent.TimeoutException e) {
                            logger.error("File processing timeout: {}", file.getName(), e);
                            callback.onFileError(file.getName(), "Processing timeout after 30 minutes");
                            synchronized (failedFiles) {
                                failedFiles[0]++;
                            }
                            synchronized (processedFiles) {
                                processedFiles[0]++;
                            }
                        } catch (Exception e) {
                            logger.error("Batch processing error: {}", file.getName(), e);
                            callback.onFileError(file.getName(), e.getMessage());
                            synchronized (failedFiles) {
                                failedFiles[0]++;
                            }
                            synchronized (processedFiles) {
                                processedFiles[0]++;
                            }
                        }
                    }, executorService);
                    
                    batchFutures.add(fileFuture);
                }
                
                // Batchteki tüm dosyaların tamamlanmasını bekle (timeout ile)
                try {
                    CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                        .get(60, java.util.concurrent.TimeUnit.MINUTES); // 60 dakika timeout
                    logger.info("Batch completed: {}/{} files processed", processedFiles[0], totalFiles);
                } catch (java.util.concurrent.TimeoutException e) {
                    logger.error("Batch processing timeout", e);
                    // Timeout durumunda kalan futureları iptal et
                    for (CompletableFuture<Void> future : batchFutures) {
                        if (!future.isDone()) {
                            future.cancel(true);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Batch processing failed", e);
                }
                
                // Bellek temizliği için kısa bir bekleme
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            logger.info("Batch processing completed: {} files processed, {} files failed", processedFiles[0], failedFiles[0]);
        }, executorService);
    }
    
    private String cikisYoluOlustur(File inputFile, String outputDir, FileType fileType, BatchSettings batchSettings) {
        String fileName = inputFile.getName();
        String baseName = fileName;
        if (fileName.contains(".")) {
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        
        // Dosya adındaki Türkçe karakterleri temizle
        baseName = temizleDosyaAdi(baseName);
        
        String extension = fileType == FileType.VIDEO ? 
            batchSettings.getVideoFormat().toLowerCase() : 
            batchSettings.getAudioFormat().toLowerCase();
        String outputFileName = baseName + "_converted." + extension;
        
        return Paths.get(outputDir, outputFileName).toString();
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            logger.info("Shutting down FFmpegService executor...");
            executorService.shutdown();
            
            try {
                // 30 saniye bekle, sonra force shutdown
                if (!executorService.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate in 30 seconds, forcing shutdown...");
                    executorService.shutdownNow();
                    
                    // 10 saniye daha bekle
                    if (!executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                        logger.error("Executor could not be terminated");
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("Shutdown interrupted, forcing shutdown...");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            logger.info("FFmpegService executor shutdown completed");
        }
    }
    
    public String getFfmpegPath() {
        return ffmpegPath;
    }
    
    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
        initializeFFmpeg();
    }
    
    public int getMaxThreads() {
        return maxThreads;
    }
    
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = Math.min(maxThreads, 8); // Maksimum 8 thread
        
        // Mevcut executorı düzgün şekilde kapat
        if (executorService != null && !executorService.isShutdown()) {
            logger.info("Shutting down existing executor to update thread count...");
            executorService.shutdown();
            
            try {
                // 10 saniye bekle
                if (!executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate in 10 seconds, forcing shutdown...");
                    executorService.shutdownNow();
                    
                    // 5 saniye daha bekle
                    if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        logger.error("Executor could not be terminated during thread count update");
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("Thread count update interrupted, forcing shutdown...");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Yeni executor oluştur
        this.executorService = Executors.newFixedThreadPool(this.maxThreads);
        logger.info("Thread count updated to: {}", this.maxThreads);
    }
    
    public void adjustThreadCountForBatch(int fileCount) {
        // Dosya sayısına göre thread sayısını optimize et
        int optimalThreads;
        if (fileCount <= 10) {
            optimalThreads = Math.min(4, maxThreads);
        } else if (fileCount <= 50) {
            optimalThreads = Math.min(6, maxThreads);
        } else {
            optimalThreads = maxThreads;
        }
        
        if (optimalThreads != maxThreads) {
            setMaxThreads(optimalThreads);
        }
    }
    
    public boolean isDetailedLogging() {
        return detailedLogging;
    }

    public void setDetailedLogging(boolean detailedLogging) {
        this.detailedLogging = detailedLogging;
    }
    
    public enum FileType {
        VIDEO, AUDIO, UNKNOWN
    }
    
    public interface ProgressCallback {
        void onProgress(double progress);
    }
    
    public interface BatchProcessingCallback {
        void onFileProgress(int currentFile, int totalFiles, double progress);
        void onFileCompleted(String fileName);
        void onFileError(String fileName, String error);
    }
}