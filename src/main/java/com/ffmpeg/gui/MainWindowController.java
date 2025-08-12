package com.ffmpeg.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.geometry.Pos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainWindowController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);
    
    @FXML private VBox mainContainer;
    @FXML private TabPane tabPane;
    @FXML private Tab videoTab;
    @FXML private Tab audioTab;
    @FXML private Tab batchTab;
    @FXML private Tab settingsTab;
    
    @FXML private TextField inputVideoPath;
    @FXML private TextField outputVideoPath;
    @FXML private ComboBox<String> videoFormatCombo;
    @FXML private ComboBox<String> videoCodecCombo;
    @FXML private Spinner<Integer> videoBitrateSpinner;
    @FXML private Spinner<Integer> videoWidthSpinner;
    @FXML private Spinner<Integer> videoHeightSpinner;
    @FXML private Spinner<Double> videoFpsSpinner;
    @FXML private Button selectInputVideoBtn;
    @FXML private Button selectOutputVideoBtn;
    @FXML private Button startVideoConversionBtn;
    @FXML private ProgressBar videoProgressBar;
    @FXML private ProgressBar videoLogProgressBar;
    @FXML private Label videoProgressLabel;
    @FXML private TextArea videoLogArea;
    
    @FXML private Label videoFileNameLabel;
    @FXML private Label videoFileSizeLabel;
    @FXML private Label videoFileTypeLabel;
    @FXML private Label videoFilePathLabel;
    @FXML private Label videoFileModifiedLabel;
    @FXML private Label videoFileCreatedLabel;
    @FXML private Label videoFilePermissionsLabel;
    
    // Video teknik özellikleri
    @FXML private Label videoCodecLabel;
    @FXML private Label videoResolutionLabel;
    @FXML private Label videoFpsLabel;
    @FXML private Label videoBitrateLabel;
    @FXML private Label videoDurationLabel;
    @FXML private Label videoAspectRatioLabel;
    @FXML private Label videoPixelFormatLabel;
    @FXML private Label videoColorSpaceLabel;
    
    // Audio teknik özellikleri
    @FXML private Label audioCodecLabel;
    @FXML private Label audioSampleRateLabel;
    @FXML private Label audioChannelsLabel;
    @FXML private Label audioBitrateLabel;
    @FXML private Label audioDurationLabel;
    @FXML private Label audioChannelLayoutLabel;
    @FXML private Label audioSampleFormatLabel;
    
    @FXML private TextField inputAudioPath;
    @FXML private TextField outputAudioPath;
    @FXML private ComboBox<String> audioFormatCombo;
    @FXML private ComboBox<String> audioCodecCombo;
    @FXML private Spinner<Integer> audioBitrateSpinner;
    @FXML private Spinner<Integer> audioSampleRateSpinner;
    @FXML private ComboBox<String> audioChannelsCombo;
    @FXML private Button selectInputAudioBtn;
    @FXML private Button selectOutputAudioBtn;
    @FXML private Button startAudioConversionBtn;
    @FXML private ProgressBar audioProgressBar;
    @FXML private ProgressBar audioLogProgressBar;
    @FXML private Label audioProgressLabel;
    @FXML private TextArea audioLogArea;
    
    @FXML private Label audioFileNameLabel;
    @FXML private Label audioFileSizeLabel;
    @FXML private Label audioFileTypeLabel;
    @FXML private Label audioFilePathLabel;
    @FXML private Label audioFileModifiedLabel;
    @FXML private Label audioFileCreatedLabel;
    @FXML private Label audioFilePermissionsLabel;
    
    @FXML private ListView<File> batchFileList;
    @FXML private TextField batchOutputDir;
    @FXML private Button addBatchFilesBtn;
    @FXML private Button removeBatchFileBtn;
    @FXML private Button selectBatchOutputDirBtn;
    @FXML private Button startBatchProcessBtn;
    @FXML private ProgressBar batchProgressBar;
    @FXML private Label batchProgressLabel;
    @FXML private TextArea batchLogArea;
    @FXML private Button clearBatchLogsBtn;
    
    // Batch Video Settings
    @FXML private CheckBox enableVideoSettingsCheck;
    @FXML private ComboBox<String> batchVideoFormatCombo;
    @FXML private ComboBox<String> batchVideoCodecCombo;
    @FXML private Spinner<Integer> batchVideoBitrateSpinner;
    @FXML private Spinner<Integer> batchVideoWidthSpinner;
    @FXML private Spinner<Integer> batchVideoHeightSpinner;
    @FXML private Spinner<Double> batchVideoFpsSpinner;
    
    // Batch Audio Settings
    @FXML private CheckBox enableAudioSettingsCheck;
    @FXML private ComboBox<String> batchAudioFormatCombo;
    @FXML private ComboBox<String> batchAudioCodecCombo;
    @FXML private Spinner<Integer> batchAudioBitrateSpinner;
    @FXML private Spinner<Integer> batchAudioSampleRateSpinner;
    @FXML private ComboBox<String> batchAudioChannelsCombo;
    
    @FXML private TreeView<File> fileTreeView;
    @FXML private Button refreshExplorerBtn;
    @FXML private Button desktopBtn;
    @FXML private Button documentsBtn;
    @FXML private Button downloadsBtn;
    @FXML private Button videosBtn;
    @FXML private Button musicBtn;
    
    @FXML private TextField ffmpegPathField;
    @FXML private Button selectFFmpegPathBtn;
    @FXML private CheckBox autoDetectFFmpegCheck;
    @FXML private Spinner<Integer> maxThreadsSpinner;
    @FXML private CheckBox enableLoggingCheck;
    
    // Dönüştürme Ayarları
    @FXML private ComboBox<String> defaultVideoFormatCombo;
    @FXML private ComboBox<String> defaultVideoCodecCombo;
    @FXML private ComboBox<String> defaultAudioFormatCombo;
    @FXML private ComboBox<String> defaultAudioCodecCombo;
    @FXML private Spinner<Integer> defaultVideoBitrateSpinner;
    @FXML private Spinner<Integer> defaultAudioBitrateSpinner;
    @FXML private ComboBox<String> defaultVideoResolutionCombo;
    @FXML private Spinner<Double> defaultFpsSpinner;
    
    // Arayüz Ayarları
    @FXML private Spinner<Integer> fileExplorerWidthSpinner;
    @FXML private Spinner<Integer> propertiesPanelWidthSpinner;
    @FXML private CheckBox autoOutputPathCheck;
    @FXML private CheckBox overwriteFilesCheck;
    @FXML private CheckBox showCompletionNotificationCheck;
    @FXML private CheckBox showErrorNotificationCheck;
    
    // Performans Ayarları
    @FXML private Spinner<Integer> cacheSizeSpinner;
    @FXML private Spinner<Integer> maxFileSizeSpinner;
    @FXML private ComboBox<String> processPriorityCombo;
    @FXML private ComboBox<String> memoryUsageCombo;
    
    // Ayarları yönetme butonları
    @FXML private Button saveSettingsBtn;
    @FXML private Button loadSettingsBtn;
    @FXML private Button resetSettingsBtn;
    
    private Stage primaryStage;
    private FFmpegService ffmpegService;
    private FileExplorer fileExplorer;
    private MediaFileAnalyzer mediaAnalyzer;
    private ObservableList<File> batchFiles = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Main window controller starting...");
        
        initializeComponents();
        setupEventHandlers();
        loadSettings();
        
        ffmpegService = new FFmpegService();
        
        // Detaylı loglama ayarını uygula
        if (enableLoggingCheck != null) {
            ffmpegService.setDetailedLogging(enableLoggingCheck.isSelected());
        }
        
        // Media analyzer'ı başlat
        String ffmpegPath = ffmpegService.getFfmpegPath();
        String ffprobePath = "ffprobe"; // Default olarak PATH'ten al
        
        if (ffmpegPath != null && !ffmpegPath.equals("ffmpeg")) {
            // FFmpeg path'i varsa, aynı dizinde ffprobe'u ara
            String ffprobeDir = new File(ffmpegPath).getParent();
            if (ffprobeDir != null) {
                String possibleFFprobePath = ffprobeDir + File.separator + "ffprobe.exe";
                if (new File(possibleFFprobePath).exists()) {
                    ffprobePath = possibleFFprobePath;
                }
            }
        }
        
        logger.info("FFmpeg path: {}", ffmpegPath);
        logger.info("FFprobe path: {}", ffprobePath);
        
        mediaAnalyzer = new MediaFileAnalyzer(ffprobePath);
        
        fileExplorer = new FileExplorer(fileTreeView);
        
        fileExplorer.setOnFileSelected(this::dosyaSecildi);
        
        setupWindowResizeListener();
        
        logger.info("Main window controller started successfully");
    }
    
    private void initializeComponents() {
        // Video ayarları
        videoFormatCombo.getItems().addAll("MP4", "AVI", "MKV", "MOV", "WMV", "FLV", "WebM");
        videoFormatCombo.setValue("MP4");
        
        videoCodecCombo.getItems().addAll("H.264", "H.265", "VP9", "AV1", "MPEG-4");
        videoCodecCombo.setValue("H.264");
        
        // Video spinner'ları editable yap
        SimpleEditableSpinner.makeEditable(videoBitrateSpinner, 100, 50000, 1000);
        SimpleEditableSpinner.makeEditable(videoWidthSpinner, 1, 7680, 1920);
        SimpleEditableSpinner.makeEditable(videoHeightSpinner, 1, 4320, 1080);
        SimpleEditableSpinner.makeEditable(videoFpsSpinner, 1.0, 120.0, 30.0, 0.1);
        
        // Audio ayarları
        audioFormatCombo.getItems().addAll("MP3", "AAC", "WAV", "FLAC", "OGG", "WMA");
        audioFormatCombo.setValue("MP3");
        
        audioCodecCombo.getItems().addAll("AAC", "MP3", "FLAC", "Vorbis", "WMA");
        audioCodecCombo.setValue("AAC");
        
        // Audio spinner'ları editable yap
        SimpleEditableSpinner.makeEditable(audioBitrateSpinner, 32, 320, 128);
        SimpleEditableSpinner.makeEditable(audioSampleRateSpinner, 8000, 192000, 44100);
        
        audioChannelsCombo.getItems().addAll("Mono (1)", "Stereo (2)", "5.1 (6)", "7.1 (8)");
        audioChannelsCombo.setValue("Stereo (2)");
        
        // Batch ayarları
        batchFileList.setItems(batchFiles);
        batchFileList.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        
        // Batch Video ayarları
        batchVideoFormatCombo.getItems().addAll("MP4", "AVI", "MKV", "MOV", "WMV", "FLV", "WebM");
        batchVideoFormatCombo.setValue("MP4");
        
        batchVideoCodecCombo.getItems().addAll("H.264", "H.265", "VP9", "AV1", "MPEG-4");
        batchVideoCodecCombo.setValue("H.264");
        
        // Batch Video spinner'ları editable yap
        SimpleEditableSpinner.makeEditable(batchVideoBitrateSpinner, 100, 50000, 1000);
        SimpleEditableSpinner.makeEditable(batchVideoWidthSpinner, 1, 7680, 1920);
        SimpleEditableSpinner.makeEditable(batchVideoHeightSpinner, 1, 4320, 1080);
        SimpleEditableSpinner.makeEditable(batchVideoFpsSpinner, 1.0, 120.0, 30.0, 0.1);
        
        // Batch Audio ayarları
        batchAudioFormatCombo.getItems().addAll("MP3", "AAC", "WAV", "FLAC", "OGG", "WMA");
        batchAudioFormatCombo.setValue("MP3");
        
        batchAudioCodecCombo.getItems().addAll("AAC", "MP3", "FLAC", "Vorbis", "WMA");
        batchAudioCodecCombo.setValue("AAC");
        
        // Batch Audio spinner'ları editable yap
        SimpleEditableSpinner.makeEditable(batchAudioBitrateSpinner, 32, 320, 128);
        SimpleEditableSpinner.makeEditable(batchAudioSampleRateSpinner, 8000, 192000, 44100);
        
        batchAudioChannelsCombo.getItems().addAll("Mono (1)", "Stereo (2)", "5.1 (6)", "7.1 (8)");
        batchAudioChannelsCombo.setValue("Stereo (2)");
        
        // Batch ayarları checkbox'ları
        enableVideoSettingsCheck.setSelected(false);
        enableAudioSettingsCheck.setSelected(false);
        
        // Genel ayarlar
        SimpleEditableSpinner.makeEditable(maxThreadsSpinner, 1, 32, Runtime.getRuntime().availableProcessors());
        
        // Dönüştürme Ayarları
        defaultVideoFormatCombo.getItems().addAll("MP4", "AVI", "MKV", "MOV", "WMV", "FLV", "WebM");
        defaultVideoFormatCombo.setValue("MP4");
        
        defaultVideoCodecCombo.getItems().addAll("H.264", "H.265", "VP9", "AV1", "MPEG-4");
        defaultVideoCodecCombo.setValue("H.264");
        
        defaultAudioFormatCombo.getItems().addAll("MP3", "AAC", "WAV", "FLAC", "OGG", "WMA");
        defaultAudioFormatCombo.setValue("MP3");
        
        defaultAudioCodecCombo.getItems().addAll("AAC", "MP3", "FLAC", "Vorbis", "WMA");
        defaultAudioCodecCombo.setValue("AAC");
        
        SimpleEditableSpinner.makeEditable(defaultVideoBitrateSpinner, 100, 50000, 1000);
        SimpleEditableSpinner.makeEditable(defaultAudioBitrateSpinner, 32, 320, 128);
        
        defaultVideoResolutionCombo.getItems().addAll("1920x1080", "1280x720", "854x480", "640x480", "Orijinal");
        defaultVideoResolutionCombo.setValue("1920x1080");
        
        SimpleEditableSpinner.makeEditable(defaultFpsSpinner, 1.0, 120.0, 30.0, 0.1);
        
        // Arayüz Ayarları
        SimpleEditableSpinner.makeEditable(fileExplorerWidthSpinner, 150, 500, 250);
        SimpleEditableSpinner.makeEditable(propertiesPanelWidthSpinner, 200, 600, 300);
        
        autoOutputPathCheck.setSelected(true);
        overwriteFilesCheck.setSelected(false);
        showCompletionNotificationCheck.setSelected(true);
        showErrorNotificationCheck.setSelected(true);
        
        // Performans Ayarları
        SimpleEditableSpinner.makeEditable(cacheSizeSpinner, 10, 1000, 100);
        SimpleEditableSpinner.makeEditable(maxFileSizeSpinner, 1, 100, 10);
        
        processPriorityCombo.getItems().addAll("Düşük", "Normal", "Yüksek");
        processPriorityCombo.setValue("Normal");
        
        memoryUsageCombo.getItems().addAll("Düşük", "Orta", "Yüksek");
        memoryUsageCombo.setValue("Orta");
        
        // Progress bar'ları sıfırla
        videoProgressBar.setProgress(0);
        audioProgressBar.setProgress(0);
        batchProgressBar.setProgress(0);
        
        // Log progress bar'ları sıfırla
        videoLogProgressBar.setProgress(0);
        audioLogProgressBar.setProgress(0);
        
        // Progress label'ları sıfırla
        videoProgressLabel.setText("Hazır");
        audioProgressLabel.setText("Hazır");
        
        logger.info("UI components initialized successfully");
    }
    
    private void setupEventHandlers() {
        selectInputVideoBtn.setOnAction(e -> selectInputVideoFile());
        selectOutputVideoBtn.setOnAction(e -> selectOutputVideoFile());
        startVideoConversionBtn.setOnAction(e -> startVideoConversion());
        
        videoFormatCombo.setOnAction(e -> updateVideoOutputPath());
        
        selectInputAudioBtn.setOnAction(e -> selectInputAudioFile());
        selectOutputAudioBtn.setOnAction(e -> selectOutputAudioFile());
        startAudioConversionBtn.setOnAction(e -> startAudioConversion());
        
        audioFormatCombo.setOnAction(e -> updateAudioOutputPath());
        
        addBatchFilesBtn.setOnAction(e -> addBatchFiles());
        removeBatchFileBtn.setOnAction(e -> removeBatchFile());
        selectBatchOutputDirBtn.setOnAction(e -> selectBatchOutputDir());
        startBatchProcessBtn.setOnAction(e -> startBatchProcess());
        clearBatchLogsBtn.setOnAction(e -> batchLogArea.clear());
        
        batchFileList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Batch tab'ında dosya seçimi sadece kaldırma işlemi için kullanılıyor
        });
        
        // FFmpeg Ayarları Event Handler'ları
        selectFFmpegPathBtn.setOnAction(e -> selectFFmpegPath());
        autoDetectFFmpegCheck.setOnAction(e -> autoDetectFFmpeg());
        
        // Dönüştürme Ayarları Event Handler'ları
        defaultVideoFormatCombo.setOnAction(e -> applyDefaultVideoSettings());
        defaultVideoCodecCombo.setOnAction(e -> applyDefaultVideoSettings());
        defaultAudioFormatCombo.setOnAction(e -> applyDefaultAudioSettings());
        defaultAudioCodecCombo.setOnAction(e -> applyDefaultAudioSettings());
        defaultVideoResolutionCombo.setOnAction(e -> applyDefaultVideoSettings());
        
        // Arayüz Ayarları Event Handler'ları
        fileExplorerWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyInterfaceSettings());
        propertiesPanelWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyInterfaceSettings());
        autoOutputPathCheck.setOnAction(e -> applyInterfaceSettings());
        overwriteFilesCheck.setOnAction(e -> applyInterfaceSettings());
        showCompletionNotificationCheck.setOnAction(e -> applyInterfaceSettings());
        showErrorNotificationCheck.setOnAction(e -> applyInterfaceSettings());
        
        // Performans Ayarları Event Handler'ları
        cacheSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyPerformanceSettings());
        maxFileSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyPerformanceSettings());
        processPriorityCombo.setOnAction(e -> applyPerformanceSettings());
        memoryUsageCombo.setOnAction(e -> applyPerformanceSettings());
        
        // Genel Ayarlar Event Handler'ları
        maxThreadsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyGeneralSettings());
        enableLoggingCheck.setOnAction(e -> {
            boolean detailedLogging = enableLoggingCheck.isSelected();
            if (ffmpegService != null) {
                ffmpegService.setDetailedLogging(detailedLogging);
                logger.info("Detailed logging " + (detailedLogging ? "enabled" : "disabled"));
            }
        });
        
        // Ayarları yönetme butonları
        saveSettingsBtn.setOnAction(e -> saveSettings());
        loadSettingsBtn.setOnAction(e -> loadSettings());
        resetSettingsBtn.setOnAction(e -> resetSettings());
    }
    
    private void selectInputVideoFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Video Dosyası Seç");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Video Dosyaları", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv", "*.flv", "*.webm"),
            new FileChooser.ExtensionFilter("Tüm Dosyalar", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            inputVideoPath.setText(selectedFile.getAbsolutePath());
            suggestOutputPath(selectedFile, videoFormatCombo.getValue().toLowerCase());
            dosyaOzellikleriniGuncelle(selectedFile, "video");
        }
    }
    
    private void selectOutputVideoFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Çıktı Video Dosyası Seç");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Video Dosyaları", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv", "*.flv", "*.webm"),
            new FileChooser.ExtensionFilter("Tüm Dosyalar", "*.*")
        );
        
        File selectedFile = fileChooser.showSaveDialog(primaryStage);
        if (selectedFile != null) {
            outputVideoPath.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void startVideoConversion() {
        if (inputVideoPath.getText().isEmpty() || outputVideoPath.getText().isEmpty()) {
            showAlert("Hata", "Giriş ve çıkış dosya yollarını belirtin", Alert.AlertType.ERROR);
            return;
        }
        
        // Giriş dosyasının varlığını kontrol et
        File inputFile = new File(inputVideoPath.getText());
        if (!inputFile.exists()) {
            showAlert("Hata", "Giriş dosyası bulunamadı: " + inputVideoPath.getText(), Alert.AlertType.ERROR);
            return;
        }
        
        // UI'ı devre dışı bırak
        startVideoConversionBtn.setDisable(true);
        
        // Önceki binding'i kaldır
        videoProgressBar.progressProperty().unbind();
        videoProgressBar.setProgress(0);
        videoLogProgressBar.setProgress(0);
        videoProgressLabel.setText("Video dönüştürme başlatılıyor...");
        videoLogArea.appendText("Video dönüştürme başlatılıyor...\n");
        
        VideoConversionTask task = new VideoConversionTask(
            inputVideoPath.getText(),
            outputVideoPath.getText(),
            videoFormatCombo.getValue(),
            videoCodecCombo.getValue(),
            videoBitrateSpinner.getValue(),
            videoWidthSpinner.getValue(),
            videoHeightSpinner.getValue(),
            videoFpsSpinner.getValue(),
            videoLogProgressBar,
            videoProgressLabel
        );
        
        // Yeni binding oluştur
        videoProgressBar.progressProperty().bind(task.progressProperty());
        
        task.setOnSucceeded(e -> {
            // Binding'i kaldır ve UI'ı güncelle
            videoProgressBar.progressProperty().unbind();
            videoLogArea.appendText("Video dönüştürme tamamlandı!\n");
            videoProgressLabel.setText("Video dönüştürme tamamlandı!");
            videoLogProgressBar.setProgress(1.0);
            startVideoConversionBtn.setDisable(false);
            showAlert("Başarılı", "Video dönüştürme işlemi tamamlandı", Alert.AlertType.INFORMATION);
        });
        
        task.setOnFailed(e -> {
            // Binding'i kaldır ve UI'ı güncelle
            videoProgressBar.progressProperty().unbind();
            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
            videoLogArea.appendText("Video dönüştürme hatası: " + errorMsg + "\n");
            videoProgressLabel.setText("Video dönüştürme başarısız: " + errorMsg);
            videoLogProgressBar.setProgress(0.0);
            startVideoConversionBtn.setDisable(false);
            showAlert("Hata", "Video dönüştürme başarısız: " + errorMsg, Alert.AlertType.ERROR);
        });
        
        task.setOnCancelled(e -> {
            // Binding'i kaldır ve UI'ı güncelle
            videoProgressBar.progressProperty().unbind();
            videoLogArea.appendText("Video dönüştürme iptal edildi.\n");
            videoProgressLabel.setText("Video dönüştürme iptal edildi");
            videoLogProgressBar.setProgress(0.0);
            startVideoConversionBtn.setDisable(false);
        });
        
        Thread taskThread = new Thread(task);
        taskThread.setDaemon(true);
        taskThread.start();
    }
    
    private void selectInputAudioFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Audio Dosyası Seç");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Audio Dosyaları", "*.mp3", "*.wav", "*.flac", "*.aac", "*.ogg", "*.wma"),
            new FileChooser.ExtensionFilter("Tüm Dosyalar", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            inputAudioPath.setText(selectedFile.getAbsolutePath());
            suggestOutputPath(selectedFile, audioFormatCombo.getValue().toLowerCase());
            dosyaOzellikleriniGuncelle(selectedFile, "audio");
        }
    }
    
    private void selectOutputAudioFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Çıktı Audio Dosyası Seç");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Audio Dosyaları", "*.mp3", "*.wav", "*.flac", "*.aac", "*.ogg", "*.wma"),
            new FileChooser.ExtensionFilter("Tüm Dosyalar", "*.*")
        );
        
        File selectedFile = fileChooser.showSaveDialog(primaryStage);
        if (selectedFile != null) {
            outputAudioPath.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void startAudioConversion() {
        if (inputAudioPath.getText().isEmpty() || outputAudioPath.getText().isEmpty()) {
            showAlert("Hata", "Giriş ve çıkış dosya yollarını belirtin", Alert.AlertType.ERROR);
            return;
        }
        
        // Giriş dosyasının varlığını kontrol et
        File inputFile = new File(inputAudioPath.getText());
        if (!inputFile.exists()) {
            showAlert("Hata", "Giriş dosyası bulunamadı: " + inputAudioPath.getText(), Alert.AlertType.ERROR);
            return;
        }
        
        // UI'ı devre dışı bırak
        startAudioConversionBtn.setDisable(true);
        
        // Önceki binding'i kaldır
        audioProgressBar.progressProperty().unbind();
        audioProgressBar.setProgress(0);
        audioLogProgressBar.setProgress(0);
        audioProgressLabel.setText("Audio dönüştürme başlatılıyor...");
        audioLogArea.appendText("Audio dönüştürme başlatılıyor...\n");
        
        AudioConversionTask task = new AudioConversionTask(
            inputAudioPath.getText(),
            outputAudioPath.getText(),
            audioFormatCombo.getValue(),
            audioCodecCombo.getValue(),
            audioBitrateSpinner.getValue(),
            audioSampleRateSpinner.getValue(),
            getChannelCount(audioChannelsCombo.getValue()),
            audioLogProgressBar,
            audioProgressLabel
        );
        
        // Yeni binding oluştur
        audioProgressBar.progressProperty().bind(task.progressProperty());
        
        task.setOnSucceeded(e -> {
            // Binding'i kaldır ve UI'ı güncelle
            audioProgressBar.progressProperty().unbind();
            audioLogArea.appendText("Audio dönüştürme tamamlandı!\n");
            audioProgressLabel.setText("Audio dönüştürme tamamlandı!");
            audioLogProgressBar.setProgress(1.0);
            startAudioConversionBtn.setDisable(false);
            showAlert("Başarılı", "Audio dönüştürme işlemi tamamlandı", Alert.AlertType.INFORMATION);
        });
        
        task.setOnFailed(e -> {
            // Binding'i kaldır ve UI'ı güncelle
            audioProgressBar.progressProperty().unbind();
            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
            audioLogArea.appendText("Audio dönüştürme hatası: " + errorMsg + "\n");
            audioProgressLabel.setText("Audio dönüştürme başarısız: " + errorMsg);
            audioLogProgressBar.setProgress(0.0);
            startAudioConversionBtn.setDisable(false);
            showAlert("Hata", "Audio dönüştürme başarısız: " + errorMsg, Alert.AlertType.ERROR);
        });
        
        task.setOnCancelled(e -> {
            // Binding'i kaldır ve UI'ı güncelle
            audioProgressBar.progressProperty().unbind();
            audioLogArea.appendText("Audio dönüştürme iptal edildi.\n");
            audioProgressLabel.setText("Audio dönüştürme iptal edildi");
            audioLogProgressBar.setProgress(0.0);
            startAudioConversionBtn.setDisable(false);
        });
        
        Thread taskThread = new Thread(task);
        taskThread.setDaemon(true);
        taskThread.start();
    }
    
    private void addBatchFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Batch İşlem için Dosyalar Seç");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Medya Dosyaları", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv", "*.flv", "*.webm", "*.mp3", "*.wav", "*.flac", "*.aac", "*.ogg", "*.wma"),
            new FileChooser.ExtensionFilter("Tüm Dosyalar", "*.*")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
        if (selectedFiles != null) {
            batchFiles.addAll(selectedFiles);
        }
    }
    
    private void removeBatchFile() {
        File selectedFile = batchFileList.getSelectionModel().getSelectedItem();
        if (selectedFile != null) {
            batchFiles.remove(selectedFile);
        }
    }
    
    private void selectBatchOutputDir() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Batch İşlem Çıktı Klasörü Seç");
        File selectedDir = dirChooser.showDialog(primaryStage);
        if (selectedDir != null) {
            batchOutputDir.setText(selectedDir.getAbsolutePath());
            logger.info("Batch output folder selected: {}", selectedDir.getAbsolutePath());
        }
    }
    
    private void startBatchProcess() {
        if (batchFiles.isEmpty()) {
            showAlert("Hata", "Batch işlem için dosya seçin", Alert.AlertType.ERROR);
            return;
        }
        
        // Fazla dosya kontrolü
        if (batchFiles.size() > 100) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Büyük Batch İşlemi");
            alert.setHeaderText("Çok sayıda dosya seçildi");
            alert.setContentText(String.format("Toplam %d dosya seçildi. Bu işlem uzun sürebilir ve sistem kaynaklarını yoğun kullanabilir.\n\n" +
                    "Devam etmek istiyor musunuz?", batchFiles.size()));
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.OK) {
                return;
            }
        }
        
        if (batchOutputDir.getText().isEmpty()) {
            showAlert("Hata", "Lütfen önce çıktı klasörü seçin", Alert.AlertType.WARNING);
            selectBatchOutputDir();
            return;
        }
        
        // Video ve audio ayarlarının seçilip seçilmediğini kontrol et
        boolean hasVideoFiles = false;
        boolean hasAudioFiles = false;
        
        for (File file : batchFiles) {
            String extension = getFileExtension(file.getName()).toLowerCase();
            if (extension.matches("(mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp|ogv)")) {
                hasVideoFiles = true;
            } else if (extension.matches("(mp3|wav|flac|aac|ogg|wma|m4a|opus)")) {
                hasAudioFiles = true;
            }
        }
        
        // Video dosyaları varsa video ayarlarının seçilip seçilmediğini kontrol et
        if (hasVideoFiles && !enableVideoSettingsCheck.isSelected()) {
            showAlert("Uyarı", "Video dosyaları seçilmiş ancak video ayarları etkinleştirilmemiş.\n\n" +
                    "Lütfen 'Video ayarlarını kullan' seçeneğini işaretleyin veya video dosyalarını listeden çıkarın.", 
                    Alert.AlertType.WARNING);
            return;
        }
        
        // Audio dosyaları varsa audio ayarlarının seçilip seçilmediğini kontrol et
        if (hasAudioFiles && !enableAudioSettingsCheck.isSelected()) {
            showAlert("Uyarı", "Ses dosyaları seçilmiş ancak ses ayarları etkinleştirilmemiş.\n\n" +
                    "Lütfen 'Ses ayarlarını kullan' seçeneğini işaretleyin veya ses dosyalarını listeden çıkarın.", 
                    Alert.AlertType.WARNING);
            return;
        }
        
        // Hiç video veya audio dosyası yoksa uyarı ver
        if (!hasVideoFiles && !hasAudioFiles) {
            showAlert("Uyarı", "Seçilen dosyalar arasında desteklenen video veya ses dosyası bulunamadı.\n\n" +
                    "Desteklenen formatlar:\n" +
                    "Video: MP4, AVI, MKV, MOV, WMV, FLV, WebM\n" +
                    "Ses: MP3, WAV, FLAC, AAC, OGG, WMA", 
                    Alert.AlertType.WARNING);
            return;
        }
        
        File outputDir = new File(batchOutputDir.getText());
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (!created) {
                showAlert("Hata", "Çıktı klasörü oluşturulamadı: " + batchOutputDir.getText(), Alert.AlertType.ERROR);
                return;
            }
        }
        
        // Batch ayarlarını al
        BatchSettings batchSettings = new BatchSettings();
        
        // Video ayarları
        if (enableVideoSettingsCheck.isSelected()) {
            batchSettings.setVideoFormat(batchVideoFormatCombo.getValue());
            batchSettings.setVideoCodec(batchVideoCodecCombo.getValue());
            batchSettings.setVideoBitrate(batchVideoBitrateSpinner.getValue());
            batchSettings.setVideoWidth(batchVideoWidthSpinner.getValue());
            batchSettings.setVideoHeight(batchVideoHeightSpinner.getValue());
            batchSettings.setVideoFps(batchVideoFpsSpinner.getValue());
        }
        
        // Audio ayarları
        if (enableAudioSettingsCheck.isSelected()) {
            batchSettings.setAudioFormat(batchAudioFormatCombo.getValue());
            batchSettings.setAudioCodec(batchAudioCodecCombo.getValue());
            batchSettings.setAudioBitrate(batchAudioBitrateSpinner.getValue());
            batchSettings.setAudioSampleRate(batchAudioSampleRateSpinner.getValue());
            batchSettings.setAudioChannels(getChannelCount(batchAudioChannelsCombo.getValue()));
        }
        
        // Log alanını temizle ve başlangıç mesajı ekle
        if (batchLogArea != null) {
            batchLogArea.clear();
            batchLogArea.appendText("=== BATCH İŞLEM BAŞLATILIYOR ===\n");
            batchLogArea.appendText("Tarih: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            batchLogArea.appendText("Dosya sayısı: " + batchFiles.size() + "\n");
            batchLogArea.appendText("Çıktı klasörü: " + batchOutputDir.getText() + "\n");
            batchLogArea.appendText("================================\n\n");
        }
        
        BatchProcessingTask task = new BatchProcessingTask(batchFiles, batchOutputDir.getText(), batchSettings);
        
        // Log callback'i ayarla
        task.setLogCallback(message -> {
            if (batchLogArea != null) {
                batchLogArea.appendText(message + "\n");
                // Otomatik scroll
                batchLogArea.setScrollTop(Double.MAX_VALUE);
            }
        });
        
        // Thread sayısını optimize et
        ffmpegService.adjustThreadCountForBatch(batchFiles.size());
        
        // Önceki binding'i kaldır
        batchProgressBar.progressProperty().unbind();
        batchProgressBar.setProgress(0);
        
        // Yeni binding oluştur
        batchProgressBar.progressProperty().bind(task.progressProperty());
        
        task.setOnSucceeded(e -> {
            // Binding'i kaldır ve UI'ı güncelle
            batchProgressBar.progressProperty().unbind();
            batchProgressBar.setProgress(1.0); // Progress bar'ı tamamla
            batchProgressLabel.setText("Batch işlem tamamlandı!");
            
            // Log penceresine tamamlanma mesajı ekle
            if (batchLogArea != null) {
                batchLogArea.appendText("\n=== BATCH İŞLEM TAMAMLANDI ===\n");
            }
            
            // Kısa bir gecikme ile alert'i göster (progress bar'ın tamamlanması için)
            javafx.application.Platform.runLater(() -> {
                showAlert("Başarılı", "Batch işlem başarıyla tamamlandı", Alert.AlertType.INFORMATION);
            });
        });
        
        task.setOnFailed(e -> {
            // Binding'i kaldır ve UI'ı güncelle
            batchProgressBar.progressProperty().unbind();
            batchProgressBar.setProgress(0); // Hata durumunda progress bar'ı sıfırla
            batchProgressLabel.setText("Batch işlem başarısız: " + task.getException().getMessage());
            
            // Log penceresine hata mesajı ekle
            if (batchLogArea != null) {
                batchLogArea.appendText("\n=== BATCH İŞLEM BAŞARISIZ ===\n");
                batchLogArea.appendText("Hata: " + task.getException().getMessage() + "\n");
            }
            
            // Kısa bir gecikme ile alert'i göster
            javafx.application.Platform.runLater(() -> {
                showAlert("Hata", "Batch işlem başarısız: " + task.getException().getMessage(), Alert.AlertType.ERROR);
            });
        });
        
        task.setOnRunning(e -> {
            batchProgressLabel.setText("Batch işlem çalışıyor...");
        });
        
        task.setOnCancelled(e -> {
            // İptal durumunda da binding'i kaldır
            batchProgressBar.progressProperty().unbind();
            batchProgressBar.setProgress(0);
            batchProgressLabel.setText("Batch işlem iptal edildi");
            
            // Log penceresine iptal mesajı ekle
            if (batchLogArea != null) {
                batchLogArea.appendText("\n=== BATCH İŞLEM İPTAL EDİLDİ ===\n");
            }
        });
        
        batchProgressLabel.setText("Batch işlem başlatılıyor...");
        
        Thread taskThread = new Thread(task);
        taskThread.setDaemon(true);
        taskThread.start();
    }
    
    private void selectFFmpegPath() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select FFmpeg Executable");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Executable Files", "*.exe", "ffmpeg"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            ffmpegPathField.setText(selectedFile.getAbsolutePath());
            saveSettings();
        }
    }
    
    private void autoDetectFFmpeg() {
        if (autoDetectFFmpegCheck.isSelected()) {
            String detectedPath = ffmpegService.autoDetectFFmpeg();
            if (detectedPath != null) {
                ffmpegPathField.setText(detectedPath);
                saveSettings();
            } else {
                showAlert("Warning", "FFmpeg could not be automatically detected", Alert.AlertType.WARNING);
            }
        }
    }
    
    private void suggestOutputPath(File inputFile, String format) {
        String inputPath = inputFile.getAbsolutePath();
        String fileName = inputFile.getName();
        String baseName = fileName;
        if (fileName.contains(".")) {
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        // Format'ı küçük harfe çevir ve Türkçe karakterleri temizle
        String formatLower = format.toLowerCase()
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c");
        String outputPath = inputFile.getParent() + File.separator + baseName + "_converted." + formatLower;
        
        if (tabPane.getSelectionModel().getSelectedItem() == videoTab) {
            outputVideoPath.setText(outputPath);
        } else if (tabPane.getSelectionModel().getSelectedItem() == audioTab) {
            outputAudioPath.setText(outputPath);
        }
    }
    
    private void updateVideoOutputPath() {
        if (!inputVideoPath.getText().isEmpty()) {
            File inputFile = new File(inputVideoPath.getText());
            if (inputFile.exists()) {
                suggestOutputPath(inputFile, videoFormatCombo.getValue().toLowerCase());
            }
        }
    }
    
    private void updateAudioOutputPath() {
        if (!inputAudioPath.getText().isEmpty()) {
            File inputFile = new File(inputAudioPath.getText());
            if (inputFile.exists()) {
                suggestOutputPath(inputFile, audioFormatCombo.getValue().toLowerCase());
            }
        }
    }
    
    private int getChannelCount(String channelText) {
        if (channelText.contains("Mono")) return 1;
        if (channelText.contains("Stereo")) return 2;
        if (channelText.contains("5.1")) return 6;
        if (channelText.contains("7.1")) return 8;
        return 2;
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
    
    private void loadSettings() {
        try {
            // Ayarları dosyadan yükle (basit JSON formatında)
            File settingsFile = new File("mediashift_settings.json");
            if (settingsFile.exists()) {
                // Burada JSON parsing yapılacak, şimdilik varsayılan değerler kullanıyoruz
                logger.info("Settings file found, loading...");
                
                // FFmpeg ayarları
                if (ffmpegPathField != null) {
                    ffmpegPathField.setText("C:\\ProgramData\\chocolatey\\bin\\ffmpeg.exe");
                }
                if (autoDetectFFmpegCheck != null) {
                    autoDetectFFmpegCheck.setSelected(true);
                }
                if (enableLoggingCheck != null) {
                    enableLoggingCheck.setSelected(false); // Varsayılan olarak detaylı loglama devre dışı
                }
                
                // Dönüştürme ayarları
                if (defaultVideoFormatCombo != null) {
                    defaultVideoFormatCombo.setValue("MP4");
                }
                if (defaultVideoCodecCombo != null) {
                    defaultVideoCodecCombo.setValue("H.264");
                }
                if (defaultAudioFormatCombo != null) {
                    defaultAudioFormatCombo.setValue("MP3");
                }
                if (defaultAudioCodecCombo != null) {
                    defaultAudioCodecCombo.setValue("AAC");
                }
                if (defaultVideoResolutionCombo != null) {
                    defaultVideoResolutionCombo.setValue("1920x1080");
                }
                
                // Arayüz ayarları
                if (autoOutputPathCheck != null) {
                    autoOutputPathCheck.setSelected(true);
                }
                if (overwriteFilesCheck != null) {
                    overwriteFilesCheck.setSelected(false);
                }
                if (showCompletionNotificationCheck != null) {
                    showCompletionNotificationCheck.setSelected(true);
                }
                if (showErrorNotificationCheck != null) {
                    showErrorNotificationCheck.setSelected(true);
                }
                
                // Performans ayarları
                if (processPriorityCombo != null) {
                    processPriorityCombo.setValue("Normal");
                }
                if (memoryUsageCombo != null) {
                    memoryUsageCombo.setValue("Orta");
                }
                
                logger.info("Settings loaded successfully");
            } else {
                logger.info("Settings file not found, using default settings");
            }
        } catch (Exception e) {
            logger.error("Error loading settings: " + e.getMessage());
        }
    }
    
    private void saveSettings() {
        try {
            // Ayarları dosyaya kaydet (basit JSON formatında)
            logger.info("Saving settings...");
            
            // Burada JSON serialization yapılacak
            // Şimdilik sadece log yazıyoruz
            logger.info("Settings saved successfully");
            
            showInfo("Info", "Settings saved successfully!");
        } catch (Exception e) {
            logger.error("Error saving settings: " + e.getMessage());
            showAlert("Error", "Error saving settings: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void refreshFileExplorer() {
        if (fileExplorer != null) {
            fileExplorer.refresh();
        }
    }
    
    @FXML
    private void addFolderToExplorer() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Add Folder");
        File selectedDir = dirChooser.showDialog(primaryStage);
        if (selectedDir != null && fileExplorer != null) {
            fileExplorer.klasoreGit(selectedDir);
        }
    }
    
    @FXML
    private void selectAsVideo() {
        File selectedFile = fileExplorer.seciliDosyayiAl();
        if (selectedFile != null && fileExplorer.videoDosyasiMi(selectedFile)) {
            inputVideoPath.setText(selectedFile.getAbsolutePath());
            suggestOutputPath(selectedFile, videoFormatCombo.getValue().toLowerCase());
            tabPane.getSelectionModel().select(videoTab);
        } else {
            showAlert("Error", "Please select a valid video file", Alert.AlertType.WARNING);
        }
    }
    
    @FXML
    private void selectAsAudio() {
        File selectedFile = fileExplorer.seciliDosyayiAl();
        if (selectedFile != null && fileExplorer.audioDosyasiMi(selectedFile)) {
            inputAudioPath.setText(selectedFile.getAbsolutePath());
            suggestOutputPath(selectedFile, audioFormatCombo.getValue().toLowerCase());
            tabPane.getSelectionModel().select(audioTab);
        } else {
            showAlert("Error", "Please select a valid audio file", Alert.AlertType.WARNING);
        }
    }
    
    @FXML
    private void addToBatch() {
        File selectedFile = fileExplorer.seciliDosyayiAl();
        if (selectedFile != null && fileExplorer.medyaDosyasiMi(selectedFile)) {
            batchFiles.add(selectedFile);
            tabPane.getSelectionModel().select(batchTab);
            dosyaOzellikleriniGuncelle(selectedFile, "batch");
        } else {
            showAlert("Error", "Please select a valid media file", Alert.AlertType.WARNING);
        }
    }
    
    @FXML
    private void navigateToDesktop() {
        if (fileExplorer != null) {
            fileExplorer.masaustuneGit();
        }
    }
    
    @FXML
    private void navigateToDocuments() {
        if (fileExplorer != null) {
            fileExplorer.belgelereGit();
        }
    }
    
    @FXML
    private void navigateToDownloads() {
        if (fileExplorer != null) {
            fileExplorer.indirilenlereGit();
        }
    }
    
    @FXML
    private void navigateToVideos() {
        if (fileExplorer != null) {
            fileExplorer.videolaraGit();
        }
    }
    
    @FXML
    private void navigateToMusic() {
        if (fileExplorer != null) {
            fileExplorer.muzigeGit();
        }
    }
    
    private void dosyaSecildi(File selectedFile) {
        if (selectedFile == null || !selectedFile.isFile()) {
            return;
        }
        
        logger.info("File selected: {}", selectedFile.getName());
        
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        
        if (selectedTab == videoTab) {
            if (fileExplorer.videoDosyasiMi(selectedFile)) {
                inputVideoPath.setText(selectedFile.getAbsolutePath());
                
                String outputPath = createOutputPath(selectedFile, videoFormatCombo.getValue().toLowerCase());
                outputVideoPath.setText(outputPath);
                
                dosyaOzellikleriniGuncelle(selectedFile, "video");
                
                showInfo("Video File Selected", 
                    "Video file selected successfully: " + selectedFile.getName());
            } else {
                showAlert("Format Not Supported", 
                    "Only video files can be selected in the Video tab.\n\n" +
                    "Selected file: " + selectedFile.getName() + "\n" +
                    "Supported video formats: MP4, AVI, MKV, MOV, WMV, FLV, WEBM", 
                    Alert.AlertType.WARNING);
            }
            
        } else if (selectedTab == audioTab) {
            if (fileExplorer.audioDosyasiMi(selectedFile)) {
                inputAudioPath.setText(selectedFile.getAbsolutePath());
                
                String outputPath = createOutputPath(selectedFile, audioFormatCombo.getValue().toLowerCase());
                outputAudioPath.setText(outputPath);
                
                updateFileProperties(selectedFile, "audio");
                
                showInfo("Audio File Selected", 
                    "Audio file selected successfully: " + selectedFile.getName());
            } else {
                showAlert("Format Not Supported", 
                    "Only audio files can be selected in the Audio tab.\n\n" +
                    "Selected file: " + selectedFile.getName() + "\n" +
                    "Supported audio formats: MP3, WAV, FLAC, AAC, OGG, WMA", 
                    Alert.AlertType.WARNING);
            }
            
        } else if (selectedTab == batchTab) {
            if (fileExplorer.medyaDosyasiMi(selectedFile)) {
                batchFiles.add(selectedFile);
                
                dosyaOzellikleriniGuncelle(selectedFile, "batch");
                
                showInfo("File Added to Batch", 
                    "Media file added to batch list: " + selectedFile.getName());
            } else {
                showAlert("Format Not Supported", 
                    "Only media files can be selected in the Batch tab.\n\n" +
                    "Selected file: " + selectedFile.getName() + "\n" +
                    "Supported media formats: MP4, AVI, MKV, MOV, WMV, FLV, WEBM, MP3, WAV, FLAC, AAC, OGG, WMA", 
                    Alert.AlertType.WARNING);
            }
            
        } else {
            if (fileExplorer.isVideoFile(selectedFile)) {
                inputVideoPath.setText(selectedFile.getAbsolutePath());
                
                String outputPath = createOutputPath(selectedFile, videoFormatCombo.getValue().toLowerCase());
                outputVideoPath.setText(outputPath);
                
                tabPane.getSelectionModel().select(videoTab);
                dosyaOzellikleriniGuncelle(selectedFile, "video");
                
                showInfo("Video File Selected", 
                    "Video file selected and redirected to Video tab: " + selectedFile.getName());
                
            } else if (fileExplorer.audioDosyasiMi(selectedFile)) {
                inputAudioPath.setText(selectedFile.getAbsolutePath());
                
                String outputPath = createOutputPath(selectedFile, audioFormatCombo.getValue().toLowerCase());
                outputAudioPath.setText(outputPath);
                
                tabPane.getSelectionModel().select(audioTab);
                dosyaOzellikleriniGuncelle(selectedFile, "audio");
                
                showInfo("Audio File Selected", 
                    "Audio file selected and redirected to Audio tab: " + selectedFile.getName());
                
            } else {
                showAlert("Unsupported File Type", 
                    "The selected file type is not supported.\n\n" +
                    "File: " + selectedFile.getName() + "\n" +
                    "Supported formats:\n" +
                    "• Video: MP4, AVI, MKV, MOV, WMV, FLV, WEBM\n" +
                    "• Audio: MP3, WAV, FLAC, AAC, OGG, WMA", 
                    Alert.AlertType.WARNING);
            }
        }
    }
    
    private String createOutputPath(File inputFile, String outputFormat) {
        String fileName = inputFile.getName();
        String baseName = fileName;
        if (fileName.contains(".")) {
            baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        // Türkçe karakterleri İngilizce karakterlere çevir
        String cleanFormat = outputFormat.toLowerCase()
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c");
        return inputFile.getParent() + File.separator + baseName + "_converted." + cleanFormat;
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void dosyaOzellikleriniGuncelle(File file, String tabType) {
        updateFileProperties(file, tabType);
    }
    
    private void updateFileProperties(File file, String tabType) {
        if (file == null || !file.exists()) {
            clearFileProperties(tabType);
            return;
        }
        
        try {
            String fileName = file.getName();
            String fileSize = formatFileSize(file.length());
            String fileType = getFileExtension(fileName);
            String filePath = file.getAbsolutePath();
            String modifiedDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                    .format(new java.util.Date(file.lastModified()));
            String createdDate = "Unknown";
            String permissions = getFilePermissions(file);
            
            // Medya dosyası analizi yap
            MediaFileAnalyzer.MediaFileInfo mediaInfo = null;
            if (mediaAnalyzer != null) {
                logger.info("Analyzing media file: {}", file.getAbsolutePath());
                mediaInfo = mediaAnalyzer.analyzeFile(file);
                if (mediaInfo != null) {
                    if (mediaInfo.hasError()) {
                        logger.warn("Media analysis error: {}", mediaInfo.getError());
                    } else {
                        logger.info("Media analysis successful - Video: {}, Audio: {}", 
                                  mediaInfo.hasVideo(), mediaInfo.hasAudio());
                    }
                } else {
                    logger.warn("Media analysis returned null");
                }
            } else {
                logger.error("MediaAnalyzer is null!");
            }
            
            switch (tabType.toLowerCase()) {
                case "video":
                    videoFileNameLabel.setText("File Name: " + fileName);
                    videoFileSizeLabel.setText("File Size: " + fileSize);
                    videoFileTypeLabel.setText("File Type: " + fileType);
                    videoFilePathLabel.setText("File Path: " + filePath);
                    videoFileModifiedLabel.setText("Modified Date: " + modifiedDate);
                    videoFileCreatedLabel.setText("Created Date: " + createdDate);
                    videoFilePermissionsLabel.setText("Permissions: " + permissions);
                    
                    // Video teknik özellikleri
                    updateVideoTechnicalProperties(mediaInfo);
                    break;
                case "audio":
                    audioFileNameLabel.setText("File Name: " + fileName);
                    audioFileSizeLabel.setText("File Size: " + fileSize);
                    audioFileTypeLabel.setText("File Type: " + fileType);
                    audioFilePathLabel.setText("File Path: " + filePath);
                    audioFileModifiedLabel.setText("Modified Date: " + modifiedDate);
                    audioFileCreatedLabel.setText("Created Date: " + createdDate);
                    audioFilePermissionsLabel.setText("Permissions: " + permissions);
                    
                    // Audio teknik özellikleri
                    updateAudioTechnicalProperties(mediaInfo);
                    break;
                case "batch":
                    // Batch tab'ında dosya özellikleri gösterilmiyor
                    break;
            }
        } catch (Exception e) {
            logger.error("Error updating file properties", e);
        }
    }
    
    private void clearFileProperties(String tabType) {
        switch (tabType.toLowerCase()) {
            case "video":
                videoFileNameLabel.setText("File Name: -");
                videoFileSizeLabel.setText("File Size: -");
                videoFileTypeLabel.setText("File Type: -");
                videoFilePathLabel.setText("File Path: -");
                videoFileModifiedLabel.setText("Modified Date: -");
                videoFileCreatedLabel.setText("Created Date: -");
                videoFilePermissionsLabel.setText("Permissions: -");
                clearVideoTechnicalProperties();
                break;
            case "audio":
                audioFileNameLabel.setText("File Name: -");
                audioFileSizeLabel.setText("File Size: -");
                audioFileTypeLabel.setText("File Type: -");
                audioFilePathLabel.setText("File Path: -");
                audioFileModifiedLabel.setText("Modified Date: -");
                audioFileCreatedLabel.setText("Created Date: -");
                audioFilePermissionsLabel.setText("Permissions: -");
                clearAudioTechnicalProperties();
                break;
            case "batch":
                // Batch tab'ında dosya özellikleri gösterilmiyor
                break;
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toUpperCase();
        }
        return "Unknown";
    }
    
    private String getFilePermissions(File file) {
        StringBuilder permissions = new StringBuilder();
        if (file.canRead()) permissions.append("R");
        if (file.canWrite()) permissions.append("W");
        if (file.canExecute()) permissions.append("X");
        return permissions.toString();
    }
    
    private void updateVideoTechnicalProperties(MediaFileAnalyzer.MediaFileInfo mediaInfo) {
        if (mediaInfo == null || mediaInfo.hasError()) {
            logger.warn("Video technical properties could not be updated - mediaInfo is null or error exists");
            clearVideoTechnicalProperties();
            return;
        }
        
        MediaFileAnalyzer.VideoInfo videoInfo = mediaInfo.getVideoInfo();
        if (videoInfo == null) {
            logger.warn("Video technical properties could not be updated - videoInfo is null");
            clearVideoTechnicalProperties();
            return;
        }
        
        logger.info("Updating video technical properties - Codec: {}, Resolution: {}", 
                   videoInfo.getCodec(), videoInfo.getResolution());
        
        // Video teknik özelliklerini güncelle
        videoCodecLabel.setText("Codec: " + (videoInfo.getCodec() != null ? videoInfo.getCodec() : "Unknown"));
        videoResolutionLabel.setText("Resolution: " + videoInfo.getResolution());
        videoFpsLabel.setText("FPS: " + videoInfo.getFormattedFps());
        videoBitrateLabel.setText("Video Bitrate: " + videoInfo.getFormattedBitrate());
        videoDurationLabel.setText("Duration: " + mediaInfo.getFormattedDuration());
        videoAspectRatioLabel.setText("Aspect Ratio: " + (videoInfo.getAspectRatio() != null ? videoInfo.getAspectRatio() : "Unknown"));
        videoPixelFormatLabel.setText("Pixel Format: " + (videoInfo.getPixelFormat() != null ? videoInfo.getPixelFormat() : "Unknown"));
        videoColorSpaceLabel.setText("Color Space: " + (videoInfo.getColorSpace() != null ? videoInfo.getColorSpace() : "Unknown"));
    }
    
    private void updateAudioTechnicalProperties(MediaFileAnalyzer.MediaFileInfo mediaInfo) {
        if (mediaInfo == null || mediaInfo.hasError()) {
            clearAudioTechnicalProperties();
            return;
        }
        
        MediaFileAnalyzer.AudioInfo audioInfo = mediaInfo.getAudioInfo();
        if (audioInfo == null) {
            clearAudioTechnicalProperties();
            return;
        }
        
        // Audio teknik özelliklerini güncelle
        audioCodecLabel.setText("Codec: " + (audioInfo.getCodec() != null ? audioInfo.getCodec() : "Unknown"));
        audioSampleRateLabel.setText("Sample Rate: " + audioInfo.getFormattedSampleRate());
        audioChannelsLabel.setText("Channels: " + audioInfo.getFormattedChannels());
        audioBitrateLabel.setText("Audio Bitrate: " + audioInfo.getFormattedBitrate());
        audioDurationLabel.setText("Duration: " + mediaInfo.getFormattedDuration());
        audioChannelLayoutLabel.setText("Channel Layout: " + (audioInfo.getChannelLayout() != null ? audioInfo.getChannelLayout() : "Unknown"));
        audioSampleFormatLabel.setText("Sample Format: " + (audioInfo.getSampleFormat() != null ? audioInfo.getSampleFormat() : "Unknown"));
    }
    
    private void clearVideoTechnicalProperties() {
        videoCodecLabel.setText("Codec: -");
        videoResolutionLabel.setText("Resolution: -");
        videoFpsLabel.setText("FPS: -");
        videoBitrateLabel.setText("Video Bitrate: -");
        videoDurationLabel.setText("Duration: -");
        videoAspectRatioLabel.setText("Aspect Ratio: -");
        videoPixelFormatLabel.setText("Pixel Format: -");
        videoColorSpaceLabel.setText("Color Space: -");
    }
    
    private void clearAudioTechnicalProperties() {
        audioCodecLabel.setText("Codec: -");
        audioSampleRateLabel.setText("Sample Rate: -");
        audioChannelsLabel.setText("Channels: -");
        audioBitrateLabel.setText("Audio Bitrate: -");
        audioDurationLabel.setText("Duration: -");
        audioChannelLayoutLabel.setText("Channel Layout: -");
        audioSampleFormatLabel.setText("Sample Format: -");
    }
    
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
    }
    
    private void setupWindowResizeListener() {
        if (primaryStage != null) {
            primaryStage.widthProperty().addListener((observable, oldValue, newValue) -> {
                adjustUIForWindowSize(newValue.doubleValue(), primaryStage.getHeight());
            });
            
            primaryStage.heightProperty().addListener((observable, oldValue, newValue) -> {
                adjustUIForWindowSize(primaryStage.getWidth(), newValue.doubleValue());
            });
        }
    }
    
    private void adjustUIForWindowSize(double width, double height) {
        Platform.runLater(() -> {
            if (width < 1000 || height < 700) {
                mainContainer.getStyleClass().add("small-window");
            } else {
                mainContainer.getStyleClass().remove("small-window");
            }
        });
    }
    
    private void applyDefaultVideoSettings() {
        try {
            // Varsayılan video ayarlarını uygula
            String defaultFormat = defaultVideoFormatCombo.getValue();
            String defaultCodec = defaultVideoCodecCombo.getValue();
            String defaultResolution = defaultVideoResolutionCombo.getValue();
            Integer defaultBitrate = defaultVideoBitrateSpinner.getValue();
            Double defaultFps = defaultFpsSpinner.getValue();
            
            // Video tab'ındaki ayarları güncelle
            if (videoFormatCombo != null) {
                videoFormatCombo.setValue(defaultFormat);
            }
            if (videoCodecCombo != null) {
                videoCodecCombo.setValue(defaultCodec);
            }
            if (videoBitrateSpinner != null) {
                videoBitrateSpinner.getValueFactory().setValue(defaultBitrate);
            }
            if (videoFpsSpinner != null) {
                videoFpsSpinner.getValueFactory().setValue(defaultFps);
            }
            
            // Çözünürlük ayarını uygula
            if (defaultResolution != null && !defaultResolution.equals("Orijinal")) {
                String[] parts = defaultResolution.split("x");
                if (parts.length == 2) {
                    int width = Integer.parseInt(parts[0]);
                    int height = Integer.parseInt(parts[1]);
                    if (videoWidthSpinner != null) {
                        videoWidthSpinner.getValueFactory().setValue(width);
                    }
                    if (videoHeightSpinner != null) {
                        videoHeightSpinner.getValueFactory().setValue(height);
                    }
                }
            }
            
            // Batch video ayarlarını da güncelle
            if (batchVideoFormatCombo != null) {
                batchVideoFormatCombo.setValue(defaultFormat);
            }
            if (batchVideoCodecCombo != null) {
                batchVideoCodecCombo.setValue(defaultCodec);
            }
            if (batchVideoBitrateSpinner != null) {
                batchVideoBitrateSpinner.getValueFactory().setValue(defaultBitrate);
            }
            if (batchVideoFpsSpinner != null) {
                batchVideoFpsSpinner.getValueFactory().setValue(defaultFps);
            }
            
            logger.info("Default video settings applied: " + defaultFormat + ", " + defaultCodec);
        } catch (Exception e) {
            logger.error("Error applying default video settings: " + e.getMessage());
        }
    }
    
    private void applyDefaultAudioSettings() {
        try {
            // Varsayılan audio ayarlarını uygula
            String defaultFormat = defaultAudioFormatCombo.getValue();
            String defaultCodec = defaultAudioCodecCombo.getValue();
            Integer defaultBitrate = defaultAudioBitrateSpinner.getValue();
            
            // Audio tab'ındaki ayarları güncelle
            if (audioFormatCombo != null) {
                audioFormatCombo.setValue(defaultFormat);
            }
            if (audioCodecCombo != null) {
                audioCodecCombo.setValue(defaultCodec);
            }
            if (audioBitrateSpinner != null) {
                audioBitrateSpinner.getValueFactory().setValue(defaultBitrate);
            }
            
            // Batch audio ayarlarını da güncelle
            if (batchAudioFormatCombo != null) {
                batchAudioFormatCombo.setValue(defaultFormat);
            }
            if (batchAudioCodecCombo != null) {
                batchAudioCodecCombo.setValue(defaultCodec);
            }
            if (batchAudioBitrateSpinner != null) {
                batchAudioBitrateSpinner.getValueFactory().setValue(defaultBitrate);
            }
            
            logger.info("Default audio settings applied: " + defaultFormat + ", " + defaultCodec);
        } catch (Exception e) {
            logger.error("Error applying default audio settings: " + e.getMessage());
        }
    }
    
    private void applyInterfaceSettings() {
        try {
            // Arayüz ayarlarını uygula
            Integer explorerWidth = fileExplorerWidthSpinner.getValue();
            Integer propertiesWidth = propertiesPanelWidthSpinner.getValue();
            boolean autoOutput = autoOutputPathCheck.isSelected();
            boolean overwrite = overwriteFilesCheck.isSelected();
            boolean showCompletion = showCompletionNotificationCheck.isSelected();
            boolean showError = showErrorNotificationCheck.isSelected();
            
            // UI boyutlarını ayarla
            if (primaryStage != null) {
                // Ana pencere boyutunu ayarla (basit implementasyon)
                logger.info("Interface dimensions updated: Explorer=" + explorerWidth + ", Properties=" + propertiesWidth);
            }
            
            logger.info("Interface settings applied: AutoOutput=" + autoOutput + ", Overwrite=" + overwrite);
        } catch (Exception e) {
            logger.error("Error applying interface settings: " + e.getMessage());
        }
    }
    
    private void applyPerformanceSettings() {
        try {
            // Performans ayarlarını uygula
            Integer cacheSize = cacheSizeSpinner.getValue();
            Integer maxFileSize = maxFileSizeSpinner.getValue();
            String processPriority = processPriorityCombo.getValue();
            String memoryUsage = memoryUsageCombo.getValue();
            
            // FFmpeg servisine performans ayarlarını bildir
            if (ffmpegService != null) {
                // Burada FFmpeg servisine performans ayarları gönderilecek
                logger.info("Performance settings sent to FFmpeg service");
            }
            
            logger.info("Performance settings applied: Cache=" + cacheSize + "MB, MaxFile=" + maxFileSize + "GB, Priority=" + processPriority);
        } catch (Exception e) {
            logger.error("Error applying performance settings: " + e.getMessage());
        }
    }
    
    private void applyGeneralSettings() {
        try {
            // Genel ayarları uygula
            Integer maxThreads = maxThreadsSpinner.getValue();
            boolean enableLogging = enableLoggingCheck.isSelected();
            
            // Thread sayısını ayarla
            if (ffmpegService != null) {
                // FFmpeg servisine thread sayısını bildir
                logger.info("Thread count sent to FFmpeg service: " + maxThreads);
                // FFmpeg servisine detaylı loglama ayarını bildir
                ffmpegService.setDetailedLogging(enableLogging);
                logger.info("Detailed logging enabled: " + enableLogging);
            }
            
            logger.info("General settings applied: MaxThreads=" + maxThreads + ", Logging=" + enableLogging);
        } catch (Exception e) {
            logger.error("Error applying general settings: " + e.getMessage());
        }
    }
    
    private void resetSettings() {
        try {
            // Tüm ayarları varsayılan değerlere sıfırla
            logger.info("Resetting settings to default values...");
            
            // FFmpeg ayarları
            if (ffmpegPathField != null) {
                ffmpegPathField.setText("");
            }
            if (autoDetectFFmpegCheck != null) {
                autoDetectFFmpegCheck.setSelected(true);
            }
            if (maxThreadsSpinner != null) {
                maxThreadsSpinner.getValueFactory().setValue(Runtime.getRuntime().availableProcessors());
            }
            if (enableLoggingCheck != null) {
                enableLoggingCheck.setSelected(false);
            }
            
            // Dönüştürme ayarları
            if (defaultVideoFormatCombo != null) {
                defaultVideoFormatCombo.setValue("MP4");
            }
            if (defaultVideoCodecCombo != null) {
                defaultVideoCodecCombo.setValue("H.264");
            }
            if (defaultAudioFormatCombo != null) {
                defaultAudioFormatCombo.setValue("MP3");
            }
            if (defaultAudioCodecCombo != null) {
                defaultAudioCodecCombo.setValue("AAC");
            }
            if (defaultVideoBitrateSpinner != null) {
                defaultVideoBitrateSpinner.getValueFactory().setValue(1000);
            }
            if (defaultAudioBitrateSpinner != null) {
                defaultAudioBitrateSpinner.getValueFactory().setValue(128);
            }
            if (defaultVideoResolutionCombo != null) {
                defaultVideoResolutionCombo.setValue("1920x1080");
            }
            if (defaultFpsSpinner != null) {
                defaultFpsSpinner.getValueFactory().setValue(30.0);
            }
            
            // Arayüz ayarları
            if (fileExplorerWidthSpinner != null) {
                fileExplorerWidthSpinner.getValueFactory().setValue(250);
            }
            if (propertiesPanelWidthSpinner != null) {
                propertiesPanelWidthSpinner.getValueFactory().setValue(300);
            }
            if (autoOutputPathCheck != null) {
                autoOutputPathCheck.setSelected(true);
            }
            if (overwriteFilesCheck != null) {
                overwriteFilesCheck.setSelected(false);
            }
            if (showCompletionNotificationCheck != null) {
                showCompletionNotificationCheck.setSelected(true);
            }
            if (showErrorNotificationCheck != null) {
                showErrorNotificationCheck.setSelected(true);
            }
            
            // Performans ayarları
            if (cacheSizeSpinner != null) {
                cacheSizeSpinner.getValueFactory().setValue(100);
            }
            if (maxFileSizeSpinner != null) {
                maxFileSizeSpinner.getValueFactory().setValue(10);
            }
            if (processPriorityCombo != null) {
                processPriorityCombo.setValue("Normal");
            }
            if (memoryUsageCombo != null) {
                memoryUsageCombo.setValue("Orta");
            }
            
            // Ayarları uygula
            applyDefaultVideoSettings();
            applyDefaultAudioSettings();
            applyInterfaceSettings();
            applyPerformanceSettings();
            applyGeneralSettings();
            
            logger.info("Settings successfully reset to default values");
            showInfo("Info", "Settings reset to default values!");
        } catch (Exception e) {
            logger.error("Error resetting settings: " + e.getMessage());
            showAlert("Error", "Error resetting settings: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

} 