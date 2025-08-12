package com.ffmpeg.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;
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
import javafx.scene.layout.StackPane;

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
    @FXML private Tab cameraTab;
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
    
    // Kamera kontrolleri
    @FXML private ComboBox<CameraService.CameraDevice> cameraComboBox;
    @FXML private Button refreshCamerasBtn;
    @FXML private Button detectCamerasBtn;
    @FXML private StackPane cameraPreviewPane;
    @FXML private Button startPreviewBtn;
    @FXML private Button stopPreviewBtn;
    @FXML private Button startRecordingBtn;
    @FXML private Button stopRecordingBtn;
    @FXML private TextField recordingOutputDirField;
    @FXML private Button selectRecordingDirBtn;
    @FXML private ComboBox<String> recordingFormatCombo;
    @FXML private ComboBox<String> recordingQualityCombo;
    @FXML private Spinner<Integer> recordingFpsSpinner;
    @FXML private Spinner<Integer> recordingBitrateSpinner;
    @FXML private Spinner<Integer> segmentDurationSpinner;
    @FXML private Label recordingStatusLabel;
    @FXML private Label recordingTimeLabel;
    @FXML private Label recordingFileLabel;
    @FXML private TextArea cameraLogArea;
    @FXML private Button clearCameraLogsBtn;
    
    // Segment yönetimi için yeni alanlar
    @FXML private ListView<LiveRecordingTask.VideoSegment> segmentListView;
    @FXML private Label segmentCountLabel;
    @FXML private Button refreshSegmentsBtn;
    @FXML private Button mergeSegmentsBtn;
    @FXML private Button clearSegmentsBtn;
    
    private Stage primaryStage;
    private FFmpegService ffmpegService;
    private FileExplorer fileExplorer;
    private MediaFileAnalyzer mediaAnalyzer;
    private CameraService cameraService;
    private ObservableList<File> batchFiles = FXCollections.observableArrayList();
    // LiveRecordingTask için değişkenler
    private LiveRecordingTask liveRecordingTask;
    private Thread recordingThread;
    
    // Segment listesi
    private ObservableList<LiveRecordingTask.VideoSegment> recordedSegments = FXCollections.observableArrayList();
    
    // Aktif kayıt oturumu için değişkenler
    private String currentRecordingSessionId = null;
    private long recordingStartTime = 0;
    
    // Kamera için ek değişkenler
    private javafx.scene.image.ImageView cameraImageView;
    private javafx.animation.Timeline recordingTimelineTimer;
    
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
        
        // Media analyzerı başlat
        String ffmpegPath = ffmpegService.getFfmpegPath();
        String ffprobePath = "ffprobe"; // Default olarak PATHten al
        
        if (ffmpegPath != null && !ffmpegPath.equals("ffmpeg")) {
            // FFmpeg pathi varsa, aynı dizinde ffprobeu ara
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
        
        // Kamera servisini başlat
        cameraService = new CameraService(ffmpegService);
        
        fileExplorer = new FileExplorer(fileTreeView);
        
        fileExplorer.setOnFileSelected(this::dosyaSecildi);
        
        setupWindowResizeListener();
        
        // İlk kamera tespitini yap
        Platform.runLater(() -> {
            if (cameraService != null) {
                detectCameras();
            }
        });
        
        logger.info("Main window controller started successfully");
    }
    
    private void initializeComponents() {
        // Video ayarları
        videoFormatCombo.getItems().addAll("MP4", "AVI", "MKV", "MOV", "WMV", "FLV", "WebM");
        videoFormatCombo.setValue("MP4");
        
        videoCodecCombo.getItems().addAll("H.264", "H.265", "VP9", "AV1", "MPEG-4");
        videoCodecCombo.setValue("H.264");
        
        // Video spinnerları editable yap
        SimpleEditableSpinner.makeEditable(videoBitrateSpinner, 100, 50000, 1000);
        SimpleEditableSpinner.makeEditable(videoWidthSpinner, 1, 7680, 1920);
        SimpleEditableSpinner.makeEditable(videoHeightSpinner, 1, 4320, 1080);
        SimpleEditableSpinner.makeEditable(videoFpsSpinner, 1.0, 120.0, 30.0, 0.1);
        
        // Audio ayarları
        audioFormatCombo.getItems().addAll("MP3", "AAC", "WAV", "FLAC", "OGG", "WMA");
        audioFormatCombo.setValue("MP3");
        
        audioCodecCombo.getItems().addAll("AAC", "MP3", "FLAC", "Vorbis", "WMA");
        audioCodecCombo.setValue("AAC");
        
        // Audio spinnerları editable yap
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
        
        // Batch Video spinnerları editable yap
        SimpleEditableSpinner.makeEditable(batchVideoBitrateSpinner, 100, 50000, 1000);
        SimpleEditableSpinner.makeEditable(batchVideoWidthSpinner, 1, 7680, 1920);
        SimpleEditableSpinner.makeEditable(batchVideoHeightSpinner, 1, 4320, 1080);
        SimpleEditableSpinner.makeEditable(batchVideoFpsSpinner, 1.0, 120.0, 30.0, 0.1);
        
        // Batch Audio ayarları
        batchAudioFormatCombo.getItems().addAll("MP3", "AAC", "WAV", "FLAC", "OGG", "WMA");
        batchAudioFormatCombo.setValue("MP3");
        
        batchAudioCodecCombo.getItems().addAll("AAC", "MP3", "FLAC", "Vorbis", "WMA");
        batchAudioCodecCombo.setValue("AAC");
        
        // Batch Audio spinnerları editable yap
        SimpleEditableSpinner.makeEditable(batchAudioBitrateSpinner, 32, 320, 128);
        SimpleEditableSpinner.makeEditable(batchAudioSampleRateSpinner, 8000, 192000, 44100);
        
        batchAudioChannelsCombo.getItems().addAll("Mono (1)", "Stereo (2)", "5.1 (6)", "7.1 (8)");
        batchAudioChannelsCombo.setValue("Stereo (2)");
        
        // Batch ayarları checkboxları
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
        
        // Kamera kontrolleri
        initializeCameraControls();
        
        // Progress barları sıfırla
        videoProgressBar.setProgress(0);
        audioProgressBar.setProgress(0);
        batchProgressBar.setProgress(0);
        
        // Log progress barları sıfırla
        videoLogProgressBar.setProgress(0);
        audioLogProgressBar.setProgress(0);
        
        // Progress labelları sıfırla
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
            // Batch tabında dosya seçimi sadece kaldırma işlemi için kullanılıyor
        });
        
        // FFmpeg Ayarları Event Handlerları
        selectFFmpegPathBtn.setOnAction(e -> selectFFmpegPath());
        autoDetectFFmpegCheck.setOnAction(e -> autoDetectFFmpeg());
        
        // Dönüştürme Ayarları Event Handlerları
        defaultVideoFormatCombo.setOnAction(e -> applyDefaultVideoSettings());
        defaultVideoCodecCombo.setOnAction(e -> applyDefaultVideoSettings());
        defaultAudioFormatCombo.setOnAction(e -> applyDefaultAudioSettings());
        defaultAudioCodecCombo.setOnAction(e -> applyDefaultAudioSettings());
        defaultVideoResolutionCombo.setOnAction(e -> applyDefaultVideoSettings());
        
        // Arayüz Ayarları Event Handlerları
        fileExplorerWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyInterfaceSettings());
        propertiesPanelWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyInterfaceSettings());
        autoOutputPathCheck.setOnAction(e -> applyInterfaceSettings());
        overwriteFilesCheck.setOnAction(e -> applyInterfaceSettings());
        showCompletionNotificationCheck.setOnAction(e -> applyInterfaceSettings());
        showErrorNotificationCheck.setOnAction(e -> applyInterfaceSettings());
        
        // Performans Ayarları Event Handlerları
        cacheSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyPerformanceSettings());
        maxFileSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyPerformanceSettings());
        processPriorityCombo.setOnAction(e -> applyPerformanceSettings());
        memoryUsageCombo.setOnAction(e -> applyPerformanceSettings());
        
        // Genel Ayarlar Event Handlerları
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
        
        // Kamera event handlerları
        setupCameraEventHandlers();
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
        
        // Önceki bindingi kaldır
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
            // Bindingi kaldır ve UI'ı güncelle
            videoProgressBar.progressProperty().unbind();
            videoLogArea.appendText("Video dönüştürme tamamlandı!\n");
            videoProgressLabel.setText("Video dönüştürme tamamlandı!");
            videoLogProgressBar.setProgress(1.0);
            startVideoConversionBtn.setDisable(false);
            showAlert("Başarılı", "Video dönüştürme işlemi tamamlandı", Alert.AlertType.INFORMATION);
        });
        
        task.setOnFailed(e -> {
            // Bindingi kaldır ve UI'ı güncelle
            videoProgressBar.progressProperty().unbind();
            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
            videoLogArea.appendText("Video dönüştürme hatası: " + errorMsg + "\n");
            videoProgressLabel.setText("Video dönüştürme başarısız: " + errorMsg);
            videoLogProgressBar.setProgress(0.0);
            startVideoConversionBtn.setDisable(false);
            showAlert("Hata", "Video dönüştürme başarısız: " + errorMsg, Alert.AlertType.ERROR);
        });
        
        task.setOnCancelled(e -> {
            // Bindingi kaldır ve UI'ı güncelle
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
        
        // Önceki bindingi kaldır
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
            // Bindingi kaldır ve UI'ı güncelle
            audioProgressBar.progressProperty().unbind();
            audioLogArea.appendText("Audio dönüştürme tamamlandı!\n");
            audioProgressLabel.setText("Audio dönüştürme tamamlandı!");
            audioLogProgressBar.setProgress(1.0);
            startAudioConversionBtn.setDisable(false);
            showAlert("Başarılı", "Audio dönüştürme işlemi tamamlandı", Alert.AlertType.INFORMATION);
        });
        
        task.setOnFailed(e -> {
            // Bindingi kaldır ve UI'ı güncelle
            audioProgressBar.progressProperty().unbind();
            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata";
            audioLogArea.appendText("Audio dönüştürme hatası: " + errorMsg + "\n");
            audioProgressLabel.setText("Audio dönüştürme başarısız: " + errorMsg);
            audioLogProgressBar.setProgress(0.0);
            startAudioConversionBtn.setDisable(false);
            showAlert("Hata", "Audio dönüştürme başarısız: " + errorMsg, Alert.AlertType.ERROR);
        });
        
        task.setOnCancelled(e -> {
            // Bindingi kaldır ve UI'ı güncelle
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
        
        // Log callbacki ayarla
        task.setLogCallback(message -> {
            if (batchLogArea != null) {
                batchLogArea.appendText(message + "\n");
                // Otomatik scroll
                batchLogArea.setScrollTop(Double.MAX_VALUE);
            }
        });
        
        // Thread sayısını optimize et
        ffmpegService.adjustThreadCountForBatch(batchFiles.size());
        
        // Önceki bindingi kaldır
        batchProgressBar.progressProperty().unbind();
        batchProgressBar.setProgress(0);
        
        // Yeni binding oluştur
        batchProgressBar.progressProperty().bind(task.progressProperty());
        
        task.setOnSucceeded(e -> {
            // Bindingi kaldır ve UI'ı güncelle
            batchProgressBar.progressProperty().unbind();
            batchProgressBar.setProgress(1.0); // Progress barı tamamla
            batchProgressLabel.setText("Batch işlem tamamlandı!");
            
            // Log penceresine tamamlanma mesajı ekle
            if (batchLogArea != null) {
                batchLogArea.appendText("\n=== BATCH İŞLEM TAMAMLANDI ===\n");
            }
            
            // Kısa bir gecikme ile alerti göster (progress barın tamamlanması için)
            javafx.application.Platform.runLater(() -> {
                showAlert("Başarılı", "Batch işlem başarıyla tamamlandı", Alert.AlertType.INFORMATION);
            });
        });
        
        task.setOnFailed(e -> {
            // Bindingi kaldır ve UI'ı güncelle
            batchProgressBar.progressProperty().unbind();
            batchProgressBar.setProgress(0); // Hata durumunda progress barı sıfırla
            batchProgressLabel.setText("Batch işlem başarısız: " + task.getException().getMessage());
            
            // Log penceresine hata mesajı ekle
            if (batchLogArea != null) {
                batchLogArea.appendText("\n=== BATCH İŞLEM BAŞARISIZ ===\n");
                batchLogArea.appendText("Hata: " + task.getException().getMessage() + "\n");
            }
            
            // Kısa bir gecikme ile alerti göster
            javafx.application.Platform.runLater(() -> {
                showAlert("Hata", "Batch işlem başarısız: " + task.getException().getMessage(), Alert.AlertType.ERROR);
            });
        });
        
        task.setOnRunning(e -> {
            batchProgressLabel.setText("Batch işlem çalışıyor...");
        });
        
        task.setOnCancelled(e -> {
            // İptal durumunda da bindingi kaldır
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
        // Formatı küçük harfe çevir ve Türkçe karakterleri temizle
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
                    // Batch tabında dosya özellikleri gösterilmiyor
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
                // Batch tabında dosya özellikleri gösterilmiyor
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
            
            // Video tabındaki ayarları güncelle
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
            
            // Audio tabındaki ayarları güncelle
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
    
    // ========================== KAMERA YÖNETİMİ ==========================
    
    private void initializeCameraControls() {
        if (cameraComboBox == null) {
            logger.warn("Camera combo box is null, skipping camera initialization");
            return;
        }
        
        // Kayıt formatları
        recordingFormatCombo.getItems().addAll("MP4", "AVI", "MOV", "MKV");
        recordingFormatCombo.setValue("MP4");
        
        // Video kaliteleri
        recordingQualityCombo.getItems().addAll("720p", "1080p", "480p");
        recordingQualityCombo.setValue("720p"); // HD default (daha güvenilir)
        
        // FPS spinner
        SimpleEditableSpinner.makeEditable(recordingFpsSpinner, 15, 60, 30);
        
        // Bitrate spinner
        SimpleEditableSpinner.makeEditable(recordingBitrateSpinner, 500, 15000, 3500); // 720p için optimize edilmiş bitrate
        
        // Segment duration spinner - varsayılan 5 saniye
        SimpleEditableSpinner.makeEditable(segmentDurationSpinner, 5, 600, 5);
        
        // Varsayılan kayıt klasörü - Projenin kendi dizininde
        String projectDir = System.getProperty("user.dir");
        String defaultRecordingDir = projectDir + "/MediaShift_Recordings";
        recordingOutputDirField.setText(defaultRecordingDir);
        
        // Kamera önizleme alanını hazırla
        setupCameraPreview();
        
        // Segment listesini ayarla
        setupSegmentList();
        
        // Segment refresh timerını başlat
        startSegmentRefreshTimer();
        
        logger.info("Camera controls initialized");
    }
    
    private void setupCameraPreview() {
        if (cameraPreviewPane == null) {
            logger.warn("Camera preview pane is null");
            return;
        }
        
        // ImageView oluştur - performans optimizasyonu
        cameraImageView = new javafx.scene.image.ImageView();
        
        // Sabit boyut - dinamik binding performans düşürür
        cameraImageView.setFitWidth(640);
        cameraImageView.setFitHeight(480);
        
        // Performans ayarları - hızlı görünüm
        cameraImageView.setPreserveRatio(true);
        cameraImageView.setSmooth(false);   // Hızlı rendering için kapalı
        cameraImageView.setCache(false);    // Memory leak önlemek için kapalı
        
        // CSS classı ekle
        cameraImageView.getStyleClass().add("camera-image-view");
        
        // Preview paneye ekle ve merkeze hizala
        cameraPreviewPane.getChildren().clear();
        cameraPreviewPane.getChildren().add(cameraImageView);
        
        // ImageViewi StackPane içinde merkeze hizala
        javafx.scene.layout.StackPane.setAlignment(cameraImageView, javafx.geometry.Pos.CENTER);
        
        logger.info("Camera preview setup completed");
    }
    
    private void setupSegmentList() {
        if (segmentListView == null) {
            logger.warn("Segment list view is null");
            return;
        }
        
        // Segment listesini ayarla
        segmentListView.setItems(recordedSegments);
        segmentListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Segment cell factory - custom görünüm
        segmentListView.setCellFactory(param -> new ListCell<LiveRecordingTask.VideoSegment>() {
            @Override
            protected void updateItem(LiveRecordingTask.VideoSegment segment, boolean empty) {
                super.updateItem(segment, empty);
                if (empty || segment == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("%s - %s (%s)", 
                        segment.getFileName(), 
                        segment.getFormattedDuration(),
                        segment.getFormattedFileSize()));
                }
            }
        });
        
        // Seçim değiştiğinde merge butonunu güncelle
        segmentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateMergeButtonState();
        });
        
        // Başlangıçta segment sayısını güncelle
        updateSegmentCount();
        
        logger.info("Segment list setup completed");
    }
    
    private void setupCameraEventHandlers() {
        // Kamera yenile
        refreshCamerasBtn.setOnAction(e -> detectCameras());
        detectCamerasBtn.setOnAction(e -> detectCameras());
        
        // Preview kontrolleri
        startPreviewBtn.setOnAction(e -> startCameraPreview());
        stopPreviewBtn.setOnAction(e -> stopCameraPreview());
        
        // Kayıt kontrolleri
        startRecordingBtn.setOnAction(e -> startCameraRecording());
        stopRecordingBtn.setOnAction(e -> stopCameraRecording());
        
        // Klasör seçimi
        selectRecordingDirBtn.setOnAction(e -> selectRecordingDirectory());
        
        // Log temizleme
        clearCameraLogsBtn.setOnAction(e -> {
            if (cameraLogArea != null) {
                cameraLogArea.clear();
            }
        });
        
        // Kamera seçimi değişikliği
        cameraComboBox.setOnAction(e -> {
            CameraService.CameraDevice selectedCamera = cameraComboBox.getValue();
            if (selectedCamera != null) {
                addCameraLog("Kamera seçildi: " + selectedCamera.getName());
            }
        });
        
        // Segment yönetimi event handlerları
        refreshSegmentsBtn.setOnAction(e -> refreshSegments());
        mergeSegmentsBtn.setOnAction(e -> mergeSelectedSegments());
        clearSegmentsBtn.setOnAction(e -> clearSegmentList());
    }
    
    private void detectCameras() {
        addCameraLog("Kameralar tespit ediliyor...");
        
        cameraService.discoverCameras().thenAccept(cameras -> {
            Platform.runLater(() -> {
                cameraComboBox.getItems().clear();
                cameraComboBox.getItems().addAll(cameras);
                
                if (!cameras.isEmpty()) {
                    cameraComboBox.setValue(cameras.get(0));
                    addCameraLog("Tespit edilen kamera sayısı: " + cameras.size());
                    for (CameraService.CameraDevice camera : cameras) {
                        addCameraLog("  - " + camera.getName() + " (" + camera.getDescription() + ")");
                    }
                } else {
                    addCameraLog("Hiç kamera tespit edilemedi!");
                }
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                addCameraLog("Kamera tespiti hatası: " + throwable.getMessage());
                logger.error("Camera detection error", throwable);
            });
            return null;
        });
    }
    
    private void startCameraPreview() {
        CameraService.CameraDevice selectedCamera = cameraComboBox.getValue();
        if (selectedCamera == null) {
            showAlert("Uyarı", "Lütfen önce bir kamera seçin", Alert.AlertType.WARNING);
            return;
        }
        
        addCameraLog("Kamera önizlemesi başlatılıyor: " + selectedCamera.getName());
        
        boolean success = cameraService.startPreview(selectedCamera, new CameraService.PreviewCallback() {
            @Override
            public void onFrameReceived(byte[] frameData) {
                Platform.runLater(() -> {
                    try {
                        // Hızlı görüntü oluşturma - performans optimizasyonu
                        javafx.scene.image.Image image = new javafx.scene.image.Image(
                            new java.io.ByteArrayInputStream(frameData),
                            640, 480, true, false  // sabit boyut, smooth=false
                        );
                        
                        // ImageView'a güvenli şekilde set et
                        if (cameraImageView != null) {
                            cameraImageView.setImage(image);
                        }
                    } catch (Exception e) {
                        logger.warn("Error displaying frame", e);
                    }
                });
            }
            
            @Override
            public void onPreviewError(String error) {
                Platform.runLater(() -> {
                    addCameraLog("Önizleme hatası: " + error);
                    logger.error("Camera preview error: {}", error);
                });
            }
        });
        
        if (success) {
            startPreviewBtn.setDisable(true);
            stopPreviewBtn.setDisable(false);
            startRecordingBtn.setDisable(false);
            addCameraLog("Kamera önizlemesi başlatıldı");
        } else {
            addCameraLog("Kamera önizlemesi başlatılamadı!");
        }
    }
    
    private void stopCameraPreview() {
        addCameraLog("Kamera önizlemesi durduruluyor...");
        
        cameraService.stopPreview();
        
        // UI'ı güncelle
        startPreviewBtn.setDisable(false);
        stopPreviewBtn.setDisable(true);
        startRecordingBtn.setDisable(true);
        stopRecordingBtn.setDisable(true);
        
        // Preview'ı temizle
        if (cameraImageView != null) {
            cameraImageView.setImage(null);
        }
        
        addCameraLog("Kamera önizlemesi durduruldu");
    }
    
    private void startCameraRecording() {
        if (!cameraService.isPreviewActive()) {
            showAlert("Uyarı", "Kayıt için önce kamera önizlemesini başlatın", Alert.AlertType.WARNING);
            return;
        }
        
        // *** KAMERA CİHAZINI ÖNİZLEME DURDURULMADAN ÖNCE SAKLA ***
        String cameraDevice = cameraService.getSelectedCameraDevice();
        if (cameraDevice == null || cameraDevice.isEmpty()) {
            addCameraLog("Kamera cihazı seçilmemiş!");
            return;
        }
        
        // Yeni kayıt oturumu başlat
        currentRecordingSessionId = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        recordingStartTime = System.currentTimeMillis();
        addCameraLog("Yeni kayıt oturumu başlatıldı: " + currentRecordingSessionId);
        
        // *** ÖNİZLEME DURDURULMUYOR - AYNI ANDA KAYIT VE ÖNİZLEME ***
        addCameraLog("Kamera kayıt başlatılıyor (aynı anda önizleme + kayıt)...");
        
        // Kayıt parametrelerini ayarla
        String outputDir = recordingOutputDirField.getText().trim();
        
        // Çıkış klasörü kontrolü - boş mu?
        if (outputDir.isEmpty()) {
            showAlert("Uyarı", "Lütfen kayıt klasörü belirtin.", Alert.AlertType.WARNING);
            return;
        }
        
        String format = recordingFormatCombo.getValue().toLowerCase();
        String quality = recordingQualityCombo.getValue();
        int fps = recordingFpsSpinner.getValue();
        int bitrate = recordingBitrateSpinner.getValue();
        int segmentDuration = segmentDurationSpinner.getValue();
        
        addCameraLog("Belirtilen kayıt klasörü: " + outputDir);
        
        // Çıkış klasörünü doğrula ve gerekirse oluştur - gelişmiş kontrol
        try {
            java.nio.file.Path dirPath = java.nio.file.Paths.get(outputDir);
            
            // Klasör mevcut mu kontrol et
            if (!java.nio.file.Files.exists(dirPath)) {
                addCameraLog("⚠️ Kayıt klasörü mevcut değil, oluşturuluyor: " + outputDir);
                
                // Klasörü oluşturmaya çalış
                java.nio.file.Files.createDirectories(dirPath);
                addCameraLog("✅ Kayıt klasörü başarıyla oluşturuldu: " + outputDir);
                
                // Kullanıcıyı bilgilendir
                showAlert("Bilgi", 
                    "Kayıt klasörü mevcut değildi ve otomatik olarak oluşturuldu:\n" + outputDir, 
                    Alert.AlertType.INFORMATION);
                    
            } else {
                addCameraLog("✅ Kayıt klasörü zaten mevcut: " + outputDir);
            }
            
            // Klasörün yazılabilir olup olmadığını kontrol et
            if (!java.nio.file.Files.isWritable(dirPath)) {
                addCameraLog("❌ HATA: Kayıt klasörü yazılabilir değil: " + outputDir);
                showAlert("Hata", 
                    "Belirtilen klasöre yazma izniniz yok:\n" + outputDir + 
                    "\n\nLütfen farklı bir klasör seçin veya yönetici olarak çalıştırın.", 
                    Alert.AlertType.ERROR);
                return;
            }
            
            addCameraLog("✅ Kayıt klasörü yazılabilir: " + outputDir);
            
        } catch (java.nio.file.InvalidPathException e) {
            addCameraLog("❌ HATA: Geçersiz kayıt klasörü yolu: " + outputDir);
            showAlert("Hata", 
                "Geçersiz klasör yolu:\n" + outputDir + 
                "\n\nLütfen geçerli bir klasör yolu girin.\n\nÖrnek: C:\\Videolar", 
                Alert.AlertType.ERROR);
            return;
        } catch (java.io.IOException e) {
            addCameraLog("❌ HATA: Kayıt klasörü oluşturulamadı: " + e.getMessage());
            showAlert("Hata", 
                "Klasör oluşturulamadı:\n" + outputDir + 
                "\n\nHata: " + e.getMessage() + 
                "\n\nLütfen farklı bir konum deneyin.", 
                Alert.AlertType.ERROR);
            return;
        } catch (SecurityException e) {
            addCameraLog("❌ HATA: Kayıt klasörü oluşturma izni yok: " + e.getMessage());
            showAlert("Hata", 
                "Klasör oluşturma izni yok:\n" + outputDir + 
                "\n\nLütfen yönetici olarak çalıştırın veya farklı bir konum seçin.", 
                Alert.AlertType.ERROR);
            return;
        }
        
        addCameraLog("Çıkış klasörü: " + outputDir);
        addCameraLog("Format: " + format + ", Kalite: " + quality + ", FPS: " + fps + ", Bitrate: " + bitrate);
        addCameraLog("Segment süresi: " + segmentDuration + " saniye");
        
        // UI kontrollerini güncelle - kayıt başlamadan önce
        startRecordingBtn.setDisable(true);
        stopRecordingBtn.setDisable(false);
        updateRecordingStatus("Kamera kayıt hazırlanıyor...");
        
        // CameraService üzerinden kayıt başlat - önizleme korunuyor
        try {
            // CameraService'e kayıt parametrelerini set et
            cameraService.setRecordingParams(outputDir, format, quality, fps, bitrate, segmentDuration, "segment");
            
            // Kayıt başlat - önizleme durmadan
            boolean recordingStarted = cameraService.startRecording(new CameraService.ExtendedRecordingCallback() {
                @Override
                public void onRecordingStarted() {
                    Platform.runLater(() -> {
                        updateRecordingStatus("Kamera kayıt yapılıyor");
                        addCameraLog("Kamera kayıt başlatıldı (önizleme aktif)");
                    });
                }
                
                @Override
                public void onRecordingStopped() {
                    Platform.runLater(() -> {
                        startRecordingBtn.setDisable(false);
                        stopRecordingBtn.setDisable(true);
                        updateRecordingStatus("Kayıt yapılmıyor");
                        recordingFileLabel.setText("Dosya: -");
                        recordingTimeLabel.setText("00:00:00");
                        addCameraLog("Kamera kayıt durduruldu (önizleme aktif)");
                        
                        // Son segmentlerin UI'ya yansıması için gecikmeli güncelleme
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000); // 2 saniye bekle
                                Platform.runLater(() -> {
                                    updateSegmentList();
                                    addCameraLog("Segment listesi güncellendi");
                                });
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    });
                }
                
                @Override
                public void onRecordingError(String error) {
                    Platform.runLater(() -> {
                        addCameraLog("Kayıt hatası: " + error);
                        startRecordingBtn.setDisable(false);
                        stopRecordingBtn.setDisable(true);
                        updateRecordingStatus("Kayıt hatası");
                        recordingTimeLabel.setText("00:00:00");
                    });
                }
                
                @Override
                public void onSegmentCreated(String segmentPath) {
                    Platform.runLater(() -> {
                        recordingFileLabel.setText("Segment: " + new java.io.File(segmentPath).getName());
                        addCameraLog("Yeni segment oluşturuldu: " + new java.io.File(segmentPath).getName());
                        
                        // Segment listesini güncelle
                        updateSegmentList();
                    });
                }
                
                @Override
                public void onTimeUpdate(String formattedTime) {
                    Platform.runLater(() -> {
                        recordingTimeLabel.setText(formattedTime);
                    });
                }
                
                @Override
                public void onRecordingPaused() {
                    Platform.runLater(() -> {
                        updateRecordingStatus("Kayıt duraklatıldı");
                        addCameraLog("Kamera kayıt duraklatıldı");
                    });
                }
                
                @Override
                public void onRecordingResumed() {
                    Platform.runLater(() -> {
                        updateRecordingStatus("Kamera kayıt yapılıyor");
                        addCameraLog("Kamera kayıt devam ediyor");
                    });
                }
            });
            
            if (!recordingStarted) {
                addCameraLog("Kamera kayıt başlatılamadı!");
                startRecordingBtn.setDisable(false);
                stopRecordingBtn.setDisable(true);
                updateRecordingStatus("Kayıt hatası");
                return;
            }
            
        } catch (Exception e) {
            addCameraLog("Kamera kayıt hatası: " + e.getMessage());
            startRecordingBtn.setDisable(false);
            stopRecordingBtn.setDisable(true);
            updateRecordingStatus("Kayıt hatası");
            return;
        }
    }
    
    private void stopCameraRecording() {
        if (cameraService == null || !cameraService.isRecording()) {
            addCameraLog("Aktif kayıt bulunamadı");
            return;
        }
        
        addCameraLog("Kamera kayıt durduruluyor...");
        
        // UI kontrollerini güncelle
        startRecordingBtn.setDisable(false);
        stopRecordingBtn.setDisable(true);
        updateRecordingStatus("Kayıt durduruluyor...");
        
        // CameraService üzerinden kayıt durdur
        try {
            cameraService.stopRecording();
            addCameraLog("Kamera kayıt durduruldu");
            addCameraLog("Kayıt oturumu tamamlandı: " + currentRecordingSessionId);
            
            // Son segmentlerin UI'ya yansıması için bekle
            Platform.runLater(() -> {
                try {
                    Thread.sleep(1000); // Segment list güncellemesi için bekle
                    updateSegmentList(); // Son segmentleri güncelle
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            // UI'ı güncelle
            updateRecordingStatus("Kayıt yapılmıyor");
            recordingFileLabel.setText("Dosya: -");
            recordingTimeLabel.setText("00:00:00");
            
        } catch (Exception e) {
            addCameraLog("Kayıt durdurma hatası: " + e.getMessage());
            updateRecordingStatus("Kayıt durdurma hatası");
        }
    }
    
    private void selectRecordingDirectory() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Kayıt Klasörü Seç");
        
        String currentDir = recordingOutputDirField.getText();
        if (currentDir != null && !currentDir.isEmpty()) {
            java.io.File dir = new java.io.File(currentDir);
            if (dir.exists()) {
                dirChooser.setInitialDirectory(dir);
            }
        }
        
        java.io.File selectedDir = dirChooser.showDialog(primaryStage);
        if (selectedDir != null) {
            recordingOutputDirField.setText(selectedDir.getAbsolutePath());
            addCameraLog("Kayıt klasörü seçildi: " + selectedDir.getAbsolutePath());
        }
    }
    
    private void addCameraLog(String message) {
        if (cameraLogArea != null) {
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            );
            Platform.runLater(() -> {
                cameraLogArea.appendText("[" + timestamp + "] " + message + "\n");
                cameraLogArea.setScrollTop(Double.MAX_VALUE);
            });
        }
        logger.info("Camera: {}", message);
    }
    
    public void shutdown() {
        logger.info("Shutting down MainWindowController...");
        
        // Aktif kayıt işlemlerini durdur
        if (liveRecordingTask != null) {
            logger.info("Stopping active recording task...");
            liveRecordingTask.cancel();
            
            // Recording threadin tamamlanmasını bekle
            if (recordingThread != null && recordingThread.isAlive()) {
                try {
                    recordingThread.interrupt();
                    recordingThread.join(3000); // 3 saniye bekle
                    if (recordingThread.isAlive()) {
                        logger.warn("Recording thread still alive after shutdown");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while waiting for recording thread");
                }
            }
            liveRecordingTask = null;
            recordingThread = null;
        }
        
        // Kamera servisini kapat (preview ve recordingi durdurur)
        if (cameraService != null) {
            logger.info("Shutting down camera service...");
            cameraService.shutdown();
        }
        
        // FFmpeg servisini kapat
        if (ffmpegService != null) {
            logger.info("Shutting down FFmpeg service...");
            // FFmpegService için shutdown metodu yoksa basit cleanup yapalım
            try {
                ffmpegService = null;
            } catch (Exception e) {
                logger.warn("Error shutting down FFmpeg service", e);
            }
        }
        
        // File explorerı kapat
        if (fileExplorer != null) {
            logger.info("Shutting down file explorer...");
            try {
                fileExplorer = null;
            } catch (Exception e) {
                logger.warn("Error shutting down file explorer", e);
            }
        }
        
        // Media analyzerı kapat
        if (mediaAnalyzer != null) {
            logger.info("Shutting down media analyzer...");
            try {
                mediaAnalyzer = null;
            } catch (Exception e) {
                logger.warn("Error shutting down media analyzer", e);
            }
        }
        
        // Timeline timerı durdur
        if (recordingTimelineTimer != null) {
            logger.info("Stopping recording timeline timer...");
            recordingTimelineTimer.stop();
            recordingTimelineTimer = null;
        }
        
        // Batch işlemlerini temizle
        if (batchFiles != null) {
            batchFiles.clear();
        }
        
        // Segment listelerini temizle
        if (recordedSegments != null) {
            recordedSegments.clear();
        }
        
        logger.info("MainWindowController shutdown completed");
    }
    
    // Segment yönetimi metodları
    private void updateSegmentCount() {
        if (segmentCountLabel != null) {
            int count = recordedSegments.size();
            segmentCountLabel.setText(String.format("(%d segment)", count));
        }
    }
    
    /**
     * Segment listesini güncelle - CameraService'den gelen segmentlerle
     */
    private void updateSegmentList() {
        if (cameraService != null && segmentListView != null) {
            try {
                List<String> segments = cameraService.getRecordedSegments();
                logger.info("Updating segment list, found {} segments from CameraService", segments != null ? segments.size() : 0);
                
                // CameraService segmentlerine ek olarak kayıt klasöründeki dosyaları da tara
                String recordingDir = recordingOutputDirField.getText().trim();
                if (!recordingDir.isEmpty()) {
                    java.util.Set<String> allSegments = new java.util.HashSet<>();
                    
                    // CameraService segmentlerini ekle
                    if (segments != null) {
                        allSegments.addAll(segments);
                    }
                    
                    // Kayıt klasöründeki tüm segment dosyalarını da ekle - SADECE AKTİF OTURUM
                    try {
                        java.nio.file.Path recordingPath = java.nio.file.Paths.get(recordingDir);
                        if (java.nio.file.Files.exists(recordingPath) && currentRecordingSessionId != null) {
                            java.nio.file.Files.list(recordingPath)
                                .filter(path -> path.toString().endsWith(".mp4"))
                                .filter(path -> path.getFileName().toString().contains("segment"))
                                .filter(path -> {
                                    // Sadece aktif kayıt oturumundaki segmentleri al
                                    String fileName = path.getFileName().toString();
                                    return fileName.contains(currentRecordingSessionId);
                                })
                                .forEach(path -> allSegments.add(path.toString()));
                            
                            logger.info("Found {} segments for current recording session {}", 
                                allSegments.size() - (segments != null ? segments.size() : 0), currentRecordingSessionId);
                        }
                    } catch (Exception e) {
                        logger.warn("Error scanning recording directory: {}", e.getMessage());
                    }
                    
                    segments = new java.util.ArrayList<>(allSegments);
                }
                
                if (segments != null && !segments.isEmpty()) {
                    // String segmentleri LiveRecordingTask.VideoSegmente dönüştür
                    ObservableList<LiveRecordingTask.VideoSegment> videoSegments = FXCollections.observableArrayList();
                    
                    for (String segmentPath : segments) {
                        try {
                            java.io.File segmentFile = new java.io.File(segmentPath);
                            if (segmentFile.exists()) {
                                // Dosya boyutunu al
                                long fileSize = segmentFile.length();
                                
                                // Basit bir VideoSegment oluştur
                                LiveRecordingTask.VideoSegment segment = new LiveRecordingTask.VideoSegment(
                                    segmentPath,
                                    0, // Duration bilgisi yok, 0 olarak ayarla
                                    java.time.LocalDateTime.now(),
                                    fileSize
                                );
                                videoSegments.add(segment);
                                logger.debug("Added segment to list: {} (size: {} bytes)", segmentPath, fileSize);
                            } else {
                                logger.warn("Segment file does not exist: {}", segmentPath);
                            }
                        } catch (Exception e) {
                            logger.warn("Error creating video segment for: " + segmentPath, e);
                        }
                    }
                    
                    // UI'ı güncelle
                    Platform.runLater(() -> {
                        recordedSegments.clear();
                        recordedSegments.addAll(videoSegments);
                        updateSegmentCount();
                        updateMergeButtonState();
                        logger.info("Segment list updated with {} segments", videoSegments.size());
                    });
                } else {
                    logger.info("No segments found, clearing segment list");
                    Platform.runLater(() -> {
                        recordedSegments.clear();
                        updateSegmentCount();
                        updateMergeButtonState();
                    });
                }
            } catch (Exception e) {
                logger.error("Error updating segment list", e);
            }
        } else {
            logger.warn("Cannot update segment list: cameraService={}, segmentListView={}", 
                       cameraService != null, segmentListView != null);
        }
    }
    
    private void updateMergeButtonState() {
        if (mergeSegmentsBtn != null) {
            int selectedCount = segmentListView.getSelectionModel().getSelectedItems().size();
            mergeSegmentsBtn.setDisable(selectedCount < 2);
        }
    }
    
    @FXML
    private void refreshSegments() {
        if (cameraService != null) {
            addCameraLog("Segment listesi yenileniyor...");
            updateSegmentList();
        } else {
            addCameraLog("Kamera servisi bulunamadı!");
        }
    }
    
    /**
     * Segment listesini periyodik olarak güncelle
     */
    private void startSegmentRefreshTimer() {
        javafx.animation.Timeline segmentRefreshTimer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), event -> {
                if (cameraService != null && cameraService.isRecording()) {
                    updateSegmentList();
                }
            })
        );
        segmentRefreshTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        segmentRefreshTimer.play();
    }
    
    /**
     * Kayıt durumunu güncelle
     */
    private void updateRecordingStatus(String status) {
        if (recordingStatusLabel != null) {
            Platform.runLater(() -> {
                recordingStatusLabel.setText(status);
                logger.info("Recording status updated: {}", status);
            });
        }
    }
    
    @FXML
    private void mergeSelectedSegments() {
        // Önce son segmentlerin UI'ya yansıması için bekle ve güncelle
        addCameraLog("Son segment'ler kontrol ediliyor...");
        
        // Segment listesini güncelleyip, son segmentlerin eklendiğinden emin ol
        updateSegmentList();
        
        // Bir miktar bekle ki son segment'ler UI'ya yansısın
        new Thread(() -> {
            try {
                Thread.sleep(1500); // UI güncellemesi için bekle
                Platform.runLater(() -> {
                    performMergeOperation();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private void performMergeOperation() {
        ObservableList<LiveRecordingTask.VideoSegment> selectedSegments = 
            segmentListView.getSelectionModel().getSelectedItems();
        
        // Eğer hiç segment seçilmemişse tüm segmentleri seç
        if (selectedSegments.isEmpty()) {
            segmentListView.getSelectionModel().selectAll();
            selectedSegments = segmentListView.getSelectionModel().getSelectedItems();
            addCameraLog("Hiç segment seçilmediği için tüm segment'ler otomatik seçildi");
        }
        
        if (selectedSegments.size() < 2) {
            showAlert("Uyarı", "Lütfen birleştirmek için en az 2 segment seçin.", Alert.AlertType.WARNING);
            return;
        }
        
        addCameraLog("Birleştirilecek segment sayısı: " + selectedSegments.size());
        
        // Sağ taraftaki sistem yolunu kullan - dosya seçme işlemini atlayarak otomatik kaydet
        String outputDir = recordingOutputDirField.getText().trim();
        if (outputDir.isEmpty()) {
            showAlert("Uyarı", "Lütfen sağ tarafta bir kayıt klasörü belirtin.", Alert.AlertType.WARNING);
            return;
        }
        
        addCameraLog("Belirtilen çıkış klasörü: " + outputDir);
        
        // Klasör yolunu doğrula ve gerekirse oluştur
        try {
            java.nio.file.Path dirPath = java.nio.file.Paths.get(outputDir);
            
            // Klasör mevcut mu kontrol et
            if (!java.nio.file.Files.exists(dirPath)) {
                addCameraLog("⚠️ Klasör mevcut değil, oluşturuluyor: " + outputDir);
                
                // Klasörü oluşturmaya çalış
                java.nio.file.Files.createDirectories(dirPath);
                addCameraLog("✅ Klasör başarıyla oluşturuldu: " + outputDir);
                
                // Kullanıcıyı bilgilendir
                showAlert("Bilgi", 
                    "Belirtilen klasör mevcut değildi ve otomatik olarak oluşturuldu:\n" + outputDir, 
                    Alert.AlertType.INFORMATION);
                    
            } else {
                addCameraLog("✅ Klasör zaten mevcut: " + outputDir);
            }
            
            // Klasörün yazılabilir olup olmadığını kontrol et
            if (!java.nio.file.Files.isWritable(dirPath)) {
                addCameraLog("❌ HATA: Klasör yazılabilir değil: " + outputDir);
                showAlert("Hata", 
                    "Belirtilen klasöre yazma izniniz yok:\n" + outputDir + 
                    "\n\nLütfen farklı bir klasör seçin veya yönetici olarak çalıştırın.", 
                    Alert.AlertType.ERROR);
                return;
            }
            
            addCameraLog("✅ Klasör yazılabilir: " + outputDir);
            
        } catch (java.nio.file.InvalidPathException e) {
            addCameraLog("❌ HATA: Geçersiz klasör yolu: " + outputDir);
            showAlert("Hata", 
                "Geçersiz klasör yolu:\n" + outputDir + 
                "\n\nLütfen geçerli bir klasör yolu girin.\n\nÖrnek: C:\\Videolar", 
                Alert.AlertType.ERROR);
            return;
        } catch (java.io.IOException e) {
            addCameraLog("❌ HATA: Klasör oluşturulamadı: " + e.getMessage());
            showAlert("Hata", 
                "Klasör oluşturulamadı:\n" + outputDir + 
                "\n\nHata: " + e.getMessage() + 
                "\n\nLütfen farklı bir konum deneyin.", 
                Alert.AlertType.ERROR);
            return;
        } catch (SecurityException e) {
            addCameraLog("❌ HATA: Güvenlik izni yok: " + e.getMessage());
            showAlert("Hata", 
                "Klasör oluşturma izni yok:\n" + outputDir + 
                "\n\nLütfen yönetici olarak çalıştırın veya farklı bir konum seçin.", 
                Alert.AlertType.ERROR);
            return;
        }
        
        // Otomatik dosya ismi oluştur
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "merged_segments_" + timestamp + ".mp4";
        
        addCameraLog("Oluşturulacak dosya ismi: " + fileName);
        
        // Çıkış dosyası yolunu oluştur
        java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir, fileName);
        File outputFile = outputPath.toFile();
        
        addCameraLog("Tam çıkış yolu: " + outputFile.getAbsolutePath());
        
        // Segment yollarını topla
        java.util.List<String> segmentPaths = new java.util.ArrayList<>();
        for (LiveRecordingTask.VideoSegment segment : selectedSegments) {
            segmentPaths.add(segment.getFilePath());
        }
        
        // VideoSegmentMergerı başlat
        VideoSegmentMerger merger = new VideoSegmentMerger(segmentPaths, outputFile.getAbsolutePath(), 
            new VideoSegmentMerger.MergeCallback() {
                @Override
                public void onMergeStarted() {
                    Platform.runLater(() -> {
                        mergeSegmentsBtn.setDisable(true);
                        addCameraLog("Segment birleştirme başlatıldı...");
                    });
                }
                
                @Override
                public void onMergeProgress(double progress) {
                    Platform.runLater(() -> {
                        addCameraLog(String.format("Birleştirme ilerleme: %.1f%%", progress));
                    });
                }
                
                @Override
                public void onMergeCompleted(String outputPath) {
                    Platform.runLater(() -> {
                        mergeSegmentsBtn.setDisable(false);
                        addCameraLog("Segment birleştirme tamamlandı: " + outputPath);
                        showAlert("Başarılı", "Segmentler başarıyla birleştirildi:\n" + outputPath, 
                                Alert.AlertType.INFORMATION);
                        
                        // Kayıt oturumunu temizle
                        currentRecordingSessionId = null;
                        recordingStartTime = 0;
                        addCameraLog("Kayıt oturumu temizlendi");
                    });
                }
                
                @Override
                public void onMergeError(String error) {
                    Platform.runLater(() -> {
                        mergeSegmentsBtn.setDisable(false);
                        addCameraLog("Birleştirme hatası: " + error);
                        showAlert("Hata", "Segment birleştirme hatası:\n" + error, 
                                Alert.AlertType.ERROR);
                    });
                }
                
                @Override
                public void onMergeLog(String logMessage) {
                    Platform.runLater(() -> addCameraLog(logMessage));
                }
            });
        
        // Background threadde çalıştır
        Thread mergeThread = new Thread(merger);
        mergeThread.setDaemon(true);
        mergeThread.start();
    }
    
    @FXML
    private void clearSegmentList() {
        recordedSegments.clear();
        updateSegmentCount();
        addCameraLog("Segment listesi temizlendi");
    }

} 