package com.ffmpeg.gui;

import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MediaFileAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaFileAnalyzer.class);
    
    private FFprobe ffprobe;
    
    public MediaFileAnalyzer(String ffprobePath) {
        try {
            logger.info("MediaFileAnalyzer starting, ffprobe path: {}", ffprobePath);
            this.ffprobe = new FFprobe(ffprobePath);
            logger.info("MediaFileAnalyzer started successfully: {}", ffprobePath);
        } catch (IOException e) {
            logger.error("FFprobe başlatılamadı: {}", ffprobePath, e);
            this.ffprobe = null;
        } catch (Exception e) {
            logger.error("MediaFileAnalyzer başlatılırken beklenmeyen hata: {}", e.getMessage(), e);
            this.ffprobe = null;
        }
    }
    
    public MediaFileInfo analyzeFile(File file) {
        if (file == null || !file.exists()) {
            logger.warn("Dosya bulunamadı veya null: {}", file);
            return null;
        }
        
        try {
            if (ffprobe == null) {
                logger.error("FFprobe başlatılmamış");
                return new MediaFileInfo(file, "FFprobe başlatılamadı");
            }
            
            logger.info("Analyzing file with FFprobe: {}", file.getAbsolutePath());
            FFmpegProbeResult result = ffprobe.probe(file.getAbsolutePath());
            logger.info("FFprobe analysis completed, stream count: {}", result.getStreams().size());
            return parseMediaInfo(file, result);
            
        } catch (IOException e) {
            logger.error("Dosya analiz edilemedi: {}", file.getAbsolutePath(), e);
            return new MediaFileInfo(file, "Dosya analiz edilemedi: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Dosya analiz edilirken beklenmeyen hata: {}", file.getAbsolutePath(), e);
            return new MediaFileInfo(file, "Beklenmeyen hata: " + e.getMessage());
        }
    }
    
    private MediaFileInfo parseMediaInfo(File file, FFmpegProbeResult result) {
        MediaFileInfo info = new MediaFileInfo(file);
        
        // Genel dosya bilgileri
        info.setDuration(result.getFormat().duration);
        info.setBitrate(result.getFormat().bit_rate);
        info.setSize(result.getFormat().size);
        info.setFormat(result.getFormat().format_name);
        
        // Stream bilgilerini analiz et
        for (FFmpegStream stream : result.getStreams()) {
            if (stream.codec_type == FFmpegStream.CodecType.VIDEO) {
                parseVideoStream(info, stream);
            } else if (stream.codec_type == FFmpegStream.CodecType.AUDIO) {
                parseAudioStream(info, stream);
            }
        }
        
        return info;
    }
    
    private void parseVideoStream(MediaFileInfo info, FFmpegStream stream) {
        VideoInfo videoInfo = new VideoInfo();
        
        // Temel video özellikleri
        videoInfo.setCodec(stream.codec_name);
        videoInfo.setCodecLongName(stream.codec_long_name);
        videoInfo.setWidth(stream.width);
        videoInfo.setHeight(stream.height);
        videoInfo.setBitrate(stream.bit_rate);
        
        // FPS bilgisi
        if (stream.r_frame_rate != null) {
            try {
                double fps = stream.r_frame_rate.doubleValue();
                videoInfo.setFps(fps);
            } catch (Exception e) {
                logger.warn("FPS parse edilemedi: {}", stream.r_frame_rate);
            }
        }
        
        // Pixel format
        videoInfo.setPixelFormat(stream.pix_fmt);
        
        // Aspect ratio
        if (stream.display_aspect_ratio != null) {
            videoInfo.setAspectRatio(stream.display_aspect_ratio);
        }
        
        // Color space bilgileri - FFmpeg kütüphanesinde bu alanlar mevcut değil
        // Bu bilgileri tags'dan almaya çalışacağız
        
        // Profile ve level bilgileri
        if (stream.profile != null) {
            videoInfo.setProfile(stream.profile);
        }
        
        // Level bilgisi int olarak geliyor, 0'dan büyükse String'e çevir
        if (stream.level > 0) {
            videoInfo.setLevel(String.valueOf(stream.level));
        }
        
        // Tags'dan ek bilgiler
        if (stream.tags != null) {
            videoInfo.setLanguage(stream.tags.get("language"));
            videoInfo.setTitle(stream.tags.get("title"));
        }
        
        info.setVideoInfo(videoInfo);
    }
    
    private void parseAudioStream(MediaFileInfo info, FFmpegStream stream) {
        AudioInfo audioInfo = new AudioInfo();
        
        // Temel audio özellikleri
        audioInfo.setCodec(stream.codec_name);
        audioInfo.setCodecLongName(stream.codec_long_name);
        audioInfo.setBitrate(stream.bit_rate);
        audioInfo.setSampleRate(stream.sample_rate);
        audioInfo.setChannels(stream.channels);
        
        // Channel layout
        if (stream.channel_layout != null) {
            audioInfo.setChannelLayout(stream.channel_layout);
        }
        
        // Sample format
        if (stream.sample_fmt != null) {
            audioInfo.setSampleFormat(stream.sample_fmt);
        }
        
        // Profile bilgisi
        if (stream.profile != null) {
            audioInfo.setProfile(stream.profile);
        }
        
        // Tags'dan ek bilgiler
        if (stream.tags != null) {
            audioInfo.setLanguage(stream.tags.get("language"));
            audioInfo.setTitle(stream.tags.get("title"));
        }
        
        info.setAudioInfo(audioInfo);
    }
    
    public static class MediaFileInfo {
        private File file;
        private String error;
        
        // Genel dosya bilgileri
        private Double duration;
        private Long bitrate;
        private Long size;
        private String format;
        
        // Video ve audio bilgileri
        private VideoInfo videoInfo;
        private AudioInfo audioInfo;
        
        public MediaFileInfo(File file) {
            this.file = file;
        }
        
        public MediaFileInfo(File file, String error) {
            this.file = file;
            this.error = error;
        }
        
        // Getter ve setter metodları
        public File getFile() { return file; }
        public String getError() { return error; }
        public Double getDuration() { return duration; }
        public Long getBitrate() { return bitrate; }
        public Long getSize() { return size; }
        public String getFormat() { return format; }
        public VideoInfo getVideoInfo() { return videoInfo; }
        public AudioInfo getAudioInfo() { return audioInfo; }
        
        public void setDuration(Double duration) { this.duration = duration; }
        public void setBitrate(Long bitrate) { this.bitrate = bitrate; }
        public void setSize(Long size) { this.size = size; }
        public void setFormat(String format) { this.format = format; }
        public void setVideoInfo(VideoInfo videoInfo) { this.videoInfo = videoInfo; }
        public void setAudioInfo(AudioInfo audioInfo) { this.audioInfo = audioInfo; }
        
        public boolean hasError() {
            return error != null && !error.isEmpty();
        }
        
        public boolean hasVideo() {
            return videoInfo != null;
        }
        
        public boolean hasAudio() {
            return audioInfo != null;
        }
        
        public String getFormattedDuration() {
            if (duration == null) return "Bilinmiyor";
            
            int hours = (int) (duration / 3600);
            int minutes = (int) ((duration % 3600) / 60);
            int seconds = (int) (duration % 60);
            
            if (hours > 0) {
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                return String.format("%02d:%02d", minutes, seconds);
            }
        }
        
        public String getFormattedBitrate() {
            if (bitrate == null) return "Bilinmiyor";
            
            if (bitrate < 1024) {
                return bitrate + " bps";
            } else if (bitrate < 1024 * 1024) {
                return String.format("%.1f kbps", bitrate / 1024.0);
            } else {
                return String.format("%.1f Mbps", bitrate / (1024.0 * 1024.0));
            }
        }
        
        public String getFormattedSize() {
            if (size == null) return "Bilinmiyor";
            
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            } else if (size < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", size / (1024.0 * 1024.0));
            } else {
                return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
    
    public static class VideoInfo {
        private String codec;
        private String codecLongName;
        private Integer width;
        private Integer height;
        private Long bitrate;
        private Double fps;
        private String pixelFormat;
        private String aspectRatio;
        private String colorSpace;
        private String colorTransfer;
        private String colorPrimaries;
        private String profile;
        private String level;
        private String language;
        private String title;
        
        // Getter ve setter metodları
        public String getCodec() { return codec; }
        public String getCodecLongName() { return codecLongName; }
        public Integer getWidth() { return width; }
        public Integer getHeight() { return height; }
        public Long getBitrate() { return bitrate; }
        public Double getFps() { return fps; }
        public String getPixelFormat() { return pixelFormat; }
        public String getAspectRatio() { return aspectRatio; }
        public String getColorSpace() { return colorSpace; }
        public String getColorTransfer() { return colorTransfer; }
        public String getColorPrimaries() { return colorPrimaries; }
        public String getProfile() { return profile; }
        public String getLevel() { return level; }
        public String getLanguage() { return language; }
        public String getTitle() { return title; }
        
        public void setCodec(String codec) { this.codec = codec; }
        public void setCodecLongName(String codecLongName) { this.codecLongName = codecLongName; }
        public void setWidth(Integer width) { this.width = width; }
        public void setHeight(Integer height) { this.height = height; }
        public void setBitrate(Long bitrate) { this.bitrate = bitrate; }
        public void setFps(Double fps) { this.fps = fps; }
        public void setPixelFormat(String pixelFormat) { this.pixelFormat = pixelFormat; }
        public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }
        public void setColorSpace(String colorSpace) { this.colorSpace = colorSpace; }
        public void setColorTransfer(String colorTransfer) { this.colorTransfer = colorTransfer; }
        public void setColorPrimaries(String colorPrimaries) { this.colorPrimaries = colorPrimaries; }
        public void setProfile(String profile) { this.profile = profile; }
        public void setLevel(String level) { this.level = level; }
        public void setLanguage(String language) { this.language = language; }
        public void setTitle(String title) { this.title = title; }
        
        public String getResolution() {
            if (width != null && height != null) {
                return width + "x" + height;
            }
            return "Bilinmiyor";
        }
        
        public String getFormattedBitrate() {
            if (bitrate == null) return "Bilinmiyor";
            
            if (bitrate < 1024) {
                return bitrate + " bps";
            } else if (bitrate < 1024 * 1024) {
                return String.format("%.1f kbps", bitrate / 1024.0);
            } else {
                return String.format("%.1f Mbps", bitrate / (1024.0 * 1024.0));
            }
        }
        
        public String getFormattedFps() {
            if (fps == null) return "Bilinmiyor";
            return String.format("%.2f fps", fps);
        }
    }
    
    public static class AudioInfo {
        private String codec;
        private String codecLongName;
        private Long bitrate;
        private Integer sampleRate;
        private Integer channels;
        private String channelLayout;
        private String sampleFormat;
        private String profile;
        private String language;
        private String title;
        
        // Getter ve setter metodları
        public String getCodec() { return codec; }
        public String getCodecLongName() { return codecLongName; }
        public Long getBitrate() { return bitrate; }
        public Integer getSampleRate() { return sampleRate; }
        public Integer getChannels() { return channels; }
        public String getChannelLayout() { return channelLayout; }
        public String getSampleFormat() { return sampleFormat; }
        public String getProfile() { return profile; }
        public String getLanguage() { return language; }
        public String getTitle() { return title; }
        
        public void setCodec(String codec) { this.codec = codec; }
        public void setCodecLongName(String codecLongName) { this.codecLongName = codecLongName; }
        public void setBitrate(Long bitrate) { this.bitrate = bitrate; }
        public void setSampleRate(Integer sampleRate) { this.sampleRate = sampleRate; }
        public void setChannels(Integer channels) { this.channels = channels; }
        public void setChannelLayout(String channelLayout) { this.channelLayout = channelLayout; }
        public void setSampleFormat(String sampleFormat) { this.sampleFormat = sampleFormat; }
        public void setProfile(String profile) { this.profile = profile; }
        public void setLanguage(String language) { this.language = language; }
        public void setTitle(String title) { this.title = title; }
        
        public String getFormattedBitrate() {
            if (bitrate == null) return "Bilinmiyor";
            
            if (bitrate < 1024) {
                return bitrate + " bps";
            } else if (bitrate < 1024 * 1024) {
                return String.format("%.1f kbps", bitrate / 1024.0);
            } else {
                return String.format("%.1f Mbps", bitrate / (1024.0 * 1024.0));
            }
        }
        
        public String getFormattedSampleRate() {
            if (sampleRate == null) return "Bilinmiyor";
            
            if (sampleRate < 1000) {
                return sampleRate + " Hz";
            } else {
                return String.format("%.1f kHz", sampleRate / 1000.0);
            }
        }
        
        public String getFormattedChannels() {
            if (channels == null) return "Bilinmiyor";
            
            switch (channels) {
                case 1: return "Mono (1)";
                case 2: return "Stereo (2)";
                case 6: return "5.1 Surround (6)";
                case 8: return "7.1 Surround (8)";
                default: return channels + " kanal";
            }
        }
    }
} 