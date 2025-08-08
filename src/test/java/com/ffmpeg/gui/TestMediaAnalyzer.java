package com.ffmpeg.gui;

import java.io.File;

public class TestMediaAnalyzer {
    
    public static void main(String[] args) {
        System.out.println("Testing MediaFileAnalyzer...");
        
        // Test with the video file
        File videoFile = new File("C:\\Users\\musta\\Downloads\\aaaa.mp4");
        
        if (!videoFile.exists()) {
            System.out.println("Video file not found: " + videoFile.getAbsolutePath());
            return;
        }
        
        System.out.println("Video file found: " + videoFile.getAbsolutePath());
        System.out.println("File size: " + videoFile.length() + " bytes");
        
        // Create MediaFileAnalyzer
        System.out.println("Creating MediaFileAnalyzer with ffprobe...");
        MediaFileAnalyzer analyzer = new MediaFileAnalyzer("ffprobe");
        
        // Analyze the file
        System.out.println("Analyzing file...");
        MediaFileAnalyzer.MediaFileInfo info = analyzer.analyzeFile(videoFile);
        
        if (info == null) {
            System.out.println("Analysis returned null");
            return;
        }
        
        if (info.hasError()) {
            System.out.println("Analysis error: " + info.getError());
            return;
        }
        
        System.out.println("Analysis successful!");
        System.out.println("Duration: " + info.getFormattedDuration());
        System.out.println("Bitrate: " + info.getFormattedBitrate());
        System.out.println("Size: " + info.getFormattedSize());
        System.out.println("Format: " + info.getFormat());
        
        if (info.hasVideo()) {
            MediaFileAnalyzer.VideoInfo videoInfo = info.getVideoInfo();
            System.out.println("\nVideo Info:");
            System.out.println("Codec: " + videoInfo.getCodec());
            System.out.println("Resolution: " + videoInfo.getResolution());
            System.out.println("FPS: " + videoInfo.getFormattedFps());
            System.out.println("Bitrate: " + videoInfo.getFormattedBitrate());
            System.out.println("Aspect Ratio: " + videoInfo.getAspectRatio());
            System.out.println("Pixel Format: " + videoInfo.getPixelFormat());
        } else {
            System.out.println("No video stream found");
        }
        
        if (info.hasAudio()) {
            MediaFileAnalyzer.AudioInfo audioInfo = info.getAudioInfo();
            System.out.println("\nAudio Info:");
            System.out.println("Codec: " + audioInfo.getCodec());
            System.out.println("Sample Rate: " + audioInfo.getFormattedSampleRate());
            System.out.println("Channels: " + audioInfo.getFormattedChannels());
            System.out.println("Bitrate: " + audioInfo.getFormattedBitrate());
            System.out.println("Channel Layout: " + audioInfo.getChannelLayout());
            System.out.println("Sample Format: " + audioInfo.getSampleFormat());
        } else {
            System.out.println("No audio stream found");
        }
        
        System.out.println("\nTest completed successfully!");
    }
} 