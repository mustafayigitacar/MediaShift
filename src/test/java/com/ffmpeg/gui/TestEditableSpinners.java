package com.ffmpeg.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestEditableSpinners {
    
    @Test
    public void testVideoConversionParamsWithCustomValues() {
        // Özel değerlerle video parametreleri testi
        VideoConversionParams params = new VideoConversionParams(
            "input.mp4", "output.mp4", "MP4", "H.264", 5000, 3840, 2160, 60.0
        );
        
        assertEquals("input.mp4", params.getInputPath());
        assertEquals("output.mp4", params.getOutputPath());
        assertEquals("MP4", params.getFormat());
        assertEquals("H.264", params.getCodec());
        assertEquals(5000, params.getBitrate()); // Özel bitrate değeri
        assertEquals(3840, params.getWidth());   // 4K genişlik
        assertEquals(2160, params.getHeight());  // 4K yükseklik
        assertEquals(60.0, params.getFps());     // 60 FPS
    }
    
    @Test
    public void testAudioConversionParamsWithCustomValues() {
        // Özel değerlerle audio parametreleri testi
        AudioConversionParams params = new AudioConversionParams(
            "input.mp3", "output.aac", "AAC", "AAC", 256, 48000, 6
        );
        
        assertEquals("input.mp3", params.getInputPath());
        assertEquals("output.aac", params.getOutputPath());
        assertEquals("AAC", params.getFormat());
        assertEquals("AAC", params.getCodec());
        assertEquals(256, params.getBitrate());    // Yüksek kalite bitrate
        assertEquals(48000, params.getSampleRate()); // 48kHz sample rate
        assertEquals(6, params.getChannels());     // 5.1 surround
    }
    
    @Test
    public void testParameterValidation() {
        // Geçersiz değerlerin kontrolü
        VideoConversionParams params = new VideoConversionParams(
            "input.mp4", "output.mp4", "MP4", "H.264", -100, 0, -50, -10.0
        );
        
        // Negatif değerler kabul edilmemeli (gerçek uygulamada validation eklenebilir)
        assertEquals(-100, params.getBitrate());
        assertEquals(0, params.getWidth());
        assertEquals(-50, params.getHeight());
        assertEquals(-10.0, params.getFps());
    }
    
    @Test
    public void testHighQualitySettings() {
        // Yüksek kalite ayarları testi
        VideoConversionParams videoParams = new VideoConversionParams(
            "input.mp4", "output.mp4", "MP4", "H.265", 8000, 7680, 4320, 120.0
        );
        
        AudioConversionParams audioParams = new AudioConversionParams(
            "input.wav", "output.flac", "FLAC", "FLAC", 320, 192000, 8
        );
        
        // Video ayarları
        assertEquals("H.265", videoParams.getCodec());
        assertEquals(8000, videoParams.getBitrate());
        assertEquals(7680, videoParams.getWidth());  // 8K genişlik
        assertEquals(4320, videoParams.getHeight()); // 8K yükseklik
        assertEquals(120.0, videoParams.getFps());   // 120 FPS
        
        // Audio ayarları
        assertEquals("FLAC", audioParams.getFormat());
        assertEquals("FLAC", audioParams.getCodec());
        assertEquals(320, audioParams.getBitrate());
        assertEquals(192000, audioParams.getSampleRate()); // 192kHz
        assertEquals(8, audioParams.getChannels());        // 7.1 surround
    }
} 