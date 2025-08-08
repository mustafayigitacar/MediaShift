package com.ffmpeg.gui;

/**
 * Toplu işlem ayarlarını tutan sınıf
 */
public class BatchSettings {
    private String videoFormat = "MP4";
    private String videoCodec = "H.264";
    private int videoBitrate = 2000;
    private int videoWidth = 1920;
    private int videoHeight = 1080;
    private double videoFps = 30.0;
    
    private String audioFormat = "MP3";
    private String audioCodec = "AAC";
    private int audioBitrate = 128;
    private int audioSampleRate = 44100;
    private int audioChannels = 2;
    
    // Video ayarları getter/setter
    public String getVideoFormat() { return videoFormat; }
    public void setVideoFormat(String videoFormat) { this.videoFormat = videoFormat; }
    
    public String getVideoCodec() { return videoCodec; }
    public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }
    
    public int getVideoBitrate() { return videoBitrate; }
    public void setVideoBitrate(int videoBitrate) { this.videoBitrate = videoBitrate; }
    
    public int getVideoWidth() { return videoWidth; }
    public void setVideoWidth(int videoWidth) { this.videoWidth = videoWidth; }
    
    public int getVideoHeight() { return videoHeight; }
    public void setVideoHeight(int videoHeight) { this.videoHeight = videoHeight; }
    
    public double getVideoFps() { return videoFps; }
    public void setVideoFps(double videoFps) { this.videoFps = videoFps; }
    
    // Audio ayarları getter/setter
    public String getAudioFormat() { return audioFormat; }
    public void setAudioFormat(String audioFormat) { this.audioFormat = audioFormat; }
    
    public String getAudioCodec() { return audioCodec; }
    public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }
    
    public int getAudioBitrate() { return audioBitrate; }
    public void setAudioBitrate(int audioBitrate) { this.audioBitrate = audioBitrate; }
    
    public int getAudioSampleRate() { return audioSampleRate; }
    public void setAudioSampleRate(int audioSampleRate) { this.audioSampleRate = audioSampleRate; }
    
    public int getAudioChannels() { return audioChannels; }
    public void setAudioChannels(int audioChannels) { this.audioChannels = audioChannels; }
} 