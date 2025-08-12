package com.ffmpeg.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestVideoAudioSettings {
    
    @Test
    public void testVideoConversionParams() {
        VideoConversionParams params = new VideoConversionParams(
            "input.mp4", "output.mp4", "MP4", "H.264", 1000, 1920, 1080, 30.0
        );
        
        assertEquals("input.mp4", params.getInputPath());
        assertEquals("output.mp4", params.getOutputPath());
        assertEquals("MP4", params.getFormat());
        assertEquals("H.264", params.getCodec());
        assertEquals(1000, params.getBitrate());
        assertEquals(1920, params.getWidth());
        assertEquals(1080, params.getHeight());
        assertEquals(30.0, params.getFps());
    }
    
    @Test
    public void testAudioConversionParams() {
        AudioConversionParams params = new AudioConversionParams(
            "input.mp3", "output.aac", "AAC", "AAC", 128, 44100, 2
        );
        
        assertEquals("input.mp3", params.getInputPath());
        assertEquals("output.aac", params.getOutputPath());
        assertEquals("AAC", params.getFormat());
        assertEquals("AAC", params.getCodec());
        assertEquals(128, params.getBitrate());
        assertEquals(44100, params.getSampleRate());
        assertEquals(2, params.getChannels());
    }
    
    @Test
    public void testFFmpegServiceInitialization() {
        FFmpegService service = new FFmpegService();
        assertNotNull(service);
        
        // FFmpeg path'ini kontrol et
        String ffmpegPath = service.getFfmpegPath();
        assertNotNull(ffmpegPath);
        System.out.println("FFmpeg path: " + ffmpegPath);
    }
} 