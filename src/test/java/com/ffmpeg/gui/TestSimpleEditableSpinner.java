package com.ffmpeg.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestSimpleEditableSpinner {
    
    @Test
    public void testSimpleEditableSpinnerClassExists() {
        // Sınıfın var olduğunu test et
        assertNotNull(SimpleEditableSpinner.class);
    }
    
    @Test
    public void testVideoConversionParamsWithCustomValues() {
        // Video parametreleri testi
        VideoConversionParams params = new VideoConversionParams(
            "input.mp4", "output.mp4", "MP4", "H.264", 8000, 3840, 2160, 60.0
        );
        
        assertEquals("input.mp4", params.getInputPath());
        assertEquals("output.mp4", params.getOutputPath());
        assertEquals("MP4", params.getFormat());
        assertEquals("H.264", params.getCodec());
        assertEquals(8000, params.getBitrate()); // Yüksek bitrate
        assertEquals(3840, params.getWidth());   // 4K genişlik
        assertEquals(2160, params.getHeight());  // 4K yükseklik
        assertEquals(60.0, params.getFps());     // 60 FPS
    }
    
    @Test
    public void testAudioConversionParamsWithCustomValues() {
        // Audio parametreleri testi
        AudioConversionParams params = new AudioConversionParams(
            "input.mp3", "output.aac", "AAC", "AAC", 320, 48000, 6
        );
        
        assertEquals("input.mp3", params.getInputPath());
        assertEquals("output.aac", params.getOutputPath());
        assertEquals("AAC", params.getFormat());
        assertEquals("AAC", params.getCodec());
        assertEquals(320, params.getBitrate());     // Yüksek kalite bitrate
        assertEquals(48000, params.getSampleRate()); // 48kHz sample rate
        assertEquals(6, params.getChannels());      // 5.1 surround
    }
    
    @Test
    public void testHighQualityVideoSettings() {
        // Yüksek kalite video ayarları testi
        VideoConversionParams params = new VideoConversionParams(
            "input.mp4", "output.mp4", "MP4", "H.265", 15000, 7680, 4320, 120.0
        );
        
        assertEquals("H.265", params.getCodec());
        assertEquals(15000, params.getBitrate());
        assertEquals(7680, params.getWidth());  // 8K genişlik
        assertEquals(4320, params.getHeight()); // 8K yükseklik
        assertEquals(120.0, params.getFps());   // 120 FPS
    }
    
    @Test
    public void testHighQualityAudioSettings() {
        // Yüksek kalite audio ayarları testi
        AudioConversionParams params = new AudioConversionParams(
            "input.wav", "output.flac", "FLAC", "FLAC", 320, 192000, 8
        );
        
        assertEquals("FLAC", params.getFormat());
        assertEquals("FLAC", params.getCodec());
        assertEquals(320, params.getBitrate());
        assertEquals(192000, params.getSampleRate()); // 192kHz
        assertEquals(8, params.getChannels());        // 7.1 surround
    }
} 