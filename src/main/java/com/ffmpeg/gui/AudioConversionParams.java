package com.ffmpeg.gui;

public class AudioConversionParams {
    private String inputPath;
    private String outputPath;
    private String format;
    private String codec;
    private int bitrate;
    private int sampleRate;
    private int channels;
    
    public AudioConversionParams(String inputPath, String outputPath, String format, 
                               String codec, int bitrate, int sampleRate, int channels) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.format = format;
        this.codec = codec;
        this.bitrate = bitrate;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public String getInputPath() { return inputPath; }
    public String getOutputPath() { return outputPath; }
    public String getFormat() { return format; }
    public String getCodec() { return codec; }
    public int getBitrate() { return bitrate; }
    public int getSampleRate() { return sampleRate; }
    public int getChannels() { return channels; }
    
    
    public void setInputPath(String inputPath) { this.inputPath = inputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    public void setFormat(String format) { this.format = format; }
    public void setCodec(String codec) { this.codec = codec; }
    public void setBitrate(int bitrate) { this.bitrate = bitrate; }
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
    public void setChannels(int channels) { this.channels = channels; }
    
    @Override
    public String toString() {
        return String.format("AudioConversionParams{input='%s', output='%s', format='%s', codec='%s', bitrate=%d, sampleRate=%d, channels=%d}",
            inputPath, outputPath, format, codec, bitrate, sampleRate, channels);
    }
} 