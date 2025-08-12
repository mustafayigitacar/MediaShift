package com.ffmpeg.gui;

public class VideoConversionParams {
    private String inputPath;
    private String outputPath;
    private String format;
    private String codec;
    private int bitrate;
    private int width;
    private int height;
    private double fps;
    
    public VideoConversionParams(String inputPath, String outputPath, String format, 
                               String codec, int bitrate, int width, int height, double fps) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.format = format;
        this.codec = codec;
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
        this.fps = fps;
    }
    
    public String getInputPath() { return inputPath; }
    public String getOutputPath() { return outputPath; }
    public String getFormat() { return format; }
    public String getCodec() { return codec; }
    public int getBitrate() { return bitrate; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public double getFps() { return fps; }
    
    public void setInputPath(String inputPath) { this.inputPath = inputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    public void setFormat(String format) { this.format = format; }
    public void setCodec(String codec) { this.codec = codec; }
    public void setBitrate(int bitrate) { this.bitrate = bitrate; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setFps(double fps) { this.fps = fps; }
    
    @Override
    public String toString() {
        return String.format("VideoConversionParams{input='%s', output='%s', format='%s', codec='%s', bitrate=%d, resolution=%dx%d, fps=%.1f}",
            inputPath, outputPath, format, codec, bitrate, width, height, fps);
    }
} 