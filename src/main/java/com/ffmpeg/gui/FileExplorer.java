package com.ffmpeg.gui;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;

public class FileExplorer {
    
    private static final Logger logger = LoggerFactory.getLogger(FileExplorer.class);
    
    private TreeView<File> treeView;
    private TreeItem<File> rootItem;
    private Consumer<File> onFileSelectedCallback;
    
    public FileExplorer(TreeView<File> treeView) {
        this.treeView = treeView;
        initializeTreeView();
    }
    
    public void setOnFileSelected(Consumer<File> callback) {
        this.onFileSelectedCallback = callback;
    }
    
    private void initializeTreeView() {
        logger.info("File explorer starting...");
        
        rootItem = new TreeItem<>(new File("Bilgisayar"));
        rootItem.setExpanded(true);
        
        addDrives();
        
        treeView.setRoot(rootItem);
        
        treeView.setCellFactory(param -> new TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (item.getPath().length() == 3 && item.getPath().endsWith("\\")) {
                        String driveName = item.getPath().substring(0, 2);
                        
                        HBox hbox = new HBox(5);
                        Label arrowLabel = new Label("▶");
                        arrowLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 12px; -fx-cursor: hand;");
                        arrowLabel.setOnMouseClicked(e -> {
                            TreeItem<File> treeItem = getTreeItem();
                            if (treeItem != null) {
                                treeItem.setExpanded(!treeItem.isExpanded());
                                if (treeItem.isExpanded() && treeItem.getChildren().isEmpty()) {
                                    loadDirectoryContents(treeItem);
                                }
                            }
                            e.consume();
                        });
                        
                        Label nameLabel = new Label(driveName);
                        nameLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");
                        hbox.getChildren().addAll(arrowLabel, nameLabel);
                        setGraphic(hbox);
                        setText(null);
                    }
                    else if (item.isDirectory()) {
                        HBox hbox = new HBox(5);
                        Label arrowLabel = new Label("▶");
                        arrowLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 12px; -fx-cursor: hand;");
                        arrowLabel.setOnMouseClicked(e -> {
                            TreeItem<File> treeItem = getTreeItem();
                            if (treeItem != null) {
                                treeItem.setExpanded(!treeItem.isExpanded());
                                if (treeItem.isExpanded() && treeItem.getChildren().isEmpty()) {
                                    loadDirectoryContents(treeItem);
                                }
                            }
                            e.consume();
                        });
                        
                        Label nameLabel = new Label(item.getName());
                        nameLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");
                        hbox.getChildren().addAll(arrowLabel, nameLabel);
                        setGraphic(hbox);
                        setText(null);
                    } else {
                        setText(item.getName());
                        setGraphic(null);
                    }
                }
            }
        });
        
        setupContextMenu();
        
        treeView.setOnMouseClicked(event -> {
            TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                File selectedFile = selectedItem.getValue();
                
                if (event.getClickCount() == 2) {
                    if (selectedFile.isFile()) {
                        logger.debug("Dosya seçildi: {}", selectedFile.getName());
                        if (onFileSelectedCallback != null) {
                            onFileSelectedCallback.accept(selectedFile);
                        }
                    }
                }
            }
        });
        
        logger.info("File explorer started");
    }
    
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem refreshItem = new MenuItem("Yenile");
        refreshItem.setOnAction(e -> refresh());
        
        MenuItem expandItem = new MenuItem("Genişlet");
        expandItem.setOnAction(e -> {
            TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getValue().isDirectory()) {
                selectedItem.setExpanded(true);
                if (selectedItem.getChildren().isEmpty()) {
                    loadDirectoryContents(selectedItem);
                }
            }
        });
        
        MenuItem collapseItem = new MenuItem("Daralt");
        collapseItem.setOnAction(e -> {
            TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                selectedItem.setExpanded(false);
            }
        });
        
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        
        MenuItem selectAsVideoItem = new MenuItem("Video Olarak Seç");
        selectAsVideoItem.setOnAction(e -> {
            File selectedFile = getSelectedFile();
            if (selectedFile != null && isVideoFile(selectedFile)) {
                logger.info("Video file selected: {}", selectedFile.getName());
                if (onFileSelectedCallback != null) {
                    onFileSelectedCallback.accept(selectedFile);
                }
            }
        });
        
        MenuItem selectAsAudioItem = new MenuItem("Audio Olarak Seç");
        selectAsAudioItem.setOnAction(e -> {
            File selectedFile = getSelectedFile();
            if (selectedFile != null && isAudioFile(selectedFile)) {
                logger.info("Audio file selected: {}", selectedFile.getName());
                if (onFileSelectedCallback != null) {
                    onFileSelectedCallback.accept(selectedFile);
                }
            }
        });
        
        MenuItem addToBatchItem = new MenuItem("Batch'e Ekle");
        addToBatchItem.setOnAction(e -> {
            File selectedFile = getSelectedFile();
            if (selectedFile != null && isMediaFile(selectedFile)) {
                logger.info("File added to batch: {}", selectedFile.getName());
                if (onFileSelectedCallback != null) {
                    onFileSelectedCallback.accept(selectedFile);
                }
            }
        });
        
        contextMenu.getItems().addAll(
            refreshItem,
            expandItem,
            collapseItem,
            separator1,
            selectAsVideoItem,
            selectAsAudioItem,
            addToBatchItem
        );
        
        treeView.setContextMenu(contextMenu);
        
        contextMenu.setOnShowing(e -> {
            TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
            File selectedFile = selectedItem != null ? selectedItem.getValue() : null;
            
            if (selectedFile != null) {
                expandItem.setDisable(!selectedFile.isDirectory());
                collapseItem.setDisable(!selectedFile.isDirectory());
                
                selectAsVideoItem.setDisable(!isVideoFile(selectedFile));
                selectAsAudioItem.setDisable(!isAudioFile(selectedFile));
                addToBatchItem.setDisable(!isMediaFile(selectedFile));
            } else {
                expandItem.setDisable(true);
                collapseItem.setDisable(true);
                selectAsVideoItem.setDisable(true);
                selectAsAudioItem.setDisable(true);
                addToBatchItem.setDisable(true);
            }
        });
    }
    
    private void addDrives() {
        File[] drives = File.listRoots();
        if (drives != null) {
            for (File drive : drives) {
                TreeItem<File> driveItem = new TreeItem<>(drive);
                driveItem.setExpanded(false);
                rootItem.getChildren().add(driveItem);
                
                logger.info("Drive added: {}", drive.getPath());
            }
        }
        logger.info("Total {} drives added", drives != null ? drives.length : 0);
    }
    
    private void loadDirectoryContents(TreeItem<File> parentItem) {
        File parentFile = parentItem.getValue();
        
        logger.info("Loading folder contents: {}", parentFile.getPath());
        
        try {
            File[] children = parentFile.listFiles();
            if (children != null) {
                logger.info("{} files/folders found", children.length);
                
                Arrays.sort(children, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });
                
                for (File child : children) {
                    if (child.isHidden()) continue;
                    
                    TreeItem<File> childItem = new TreeItem<>(child);
                    parentItem.getChildren().add(childItem);
                }
                
                logger.info("Folder contents loaded successfully: {} items", parentItem.getChildren().size());
            } else {
                logger.warn("Folder contents null returned: {}", parentFile.getPath());
            }
        } catch (Exception e) {
            logger.error("Error loading folder contents: {}", parentFile.getPath(), e);
        }
    }
    
    public void refresh() {
        rootItem.getChildren().clear();
        addDrives();
    }
    
    public void klasoreGit(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("Invalid folder: {}", directory.getPath());
            return;
        }
        
        logger.info("Navigating to folder: {}", directory.getPath());
        
        String driveLetter = directory.getPath().substring(0, 2);
        TreeItem<File> driveItem = null;
        
        for (TreeItem<File> child : rootItem.getChildren()) {
            File childFile = child.getValue();
            if (childFile.getPath().startsWith(driveLetter)) {
                driveItem = child;
                break;
            }
        }
        
        if (driveItem != null) {
            driveItem.setExpanded(true);
            
            if (driveItem.getChildren().isEmpty()) {
                loadDirectoryContents(driveItem);
            }
            
            klasorBulVeSec(driveItem, directory);
        } else {
            logger.warn("Drive not found: {}", driveLetter);
        }
    }
    
    private void klasorBulVeSec(TreeItem<File> parent, File target) {
        for (TreeItem<File> child : parent.getChildren()) {
            File childFile = child.getValue();
            
            if (childFile.getPath().equals(target.getPath())) {
                child.setExpanded(true);
                treeView.getSelectionModel().select(child);
                treeView.scrollTo(treeView.getRow(child));
                logger.info("Folder found and selected: {}", target.getPath());
                return;
            }
            
            if (childFile.isDirectory()) {
                child.setExpanded(true);
                if (child.getChildren().isEmpty()) {
                    loadDirectoryContents(child);
                }
                if (target.getPath().startsWith(childFile.getPath())) {
                    klasorBulVeSec(child, target);
                }
            }
        }
    }
    
    public File seciliDosyayiAl() {
        TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
        return selectedItem != null ? selectedItem.getValue() : null;
    }
    
    public File getSelectedFile() {
        return seciliDosyayiAl();
    }
    
    public File[] seciliDosyalariAl() {
        return treeView.getSelectionModel().getSelectedItems().stream()
            .map(TreeItem::getValue)
            .toArray(File[]::new);
    }
    
    public void masaustuneGit() {
        String desktopPath = System.getProperty("user.home") + "\\Desktop";
        logger.info("Navigating to Desktop: {}", desktopPath);
        File desktop = new File(desktopPath);
        if (desktop.exists()) {
            klasoreGit(desktop);
        } else {
            logger.warn("Desktop folder not found: {}", desktopPath);
        }
    }
    
    public void belgelereGit() {
        String documentsPath = System.getProperty("user.home") + "\\Documents";
        logger.info("Navigating to Documents: {}", documentsPath);
        File documents = new File(documentsPath);
        if (documents.exists()) {
            klasoreGit(documents);
        } else {
            logger.warn("Documents folder not found: {}", documentsPath);
        }
    }
    
    public void indirilenlereGit() {
        String downloadsPath = System.getProperty("user.home") + "\\Downloads";
        logger.info("Navigating to Downloads: {}", downloadsPath);
        File downloads = new File(downloadsPath);
        if (downloads.exists()) {
            klasoreGit(downloads);
        } else {
            logger.warn("Downloads folder not found: {}", downloadsPath);
        }
    }
    
    public void videolaraGit() {
        String videosPath = System.getProperty("user.home") + "\\Videos";
        logger.info("Navigating to Videos: {}", videosPath);
        File videos = new File(videosPath);
        if (videos.exists()) {
            klasoreGit(videos);
        } else {
            logger.warn("Videos folder not found: {}", videosPath);
        }
    }
    
    public void muzigeGit() {
        String musicPath = System.getProperty("user.home") + "\\Music";
        logger.info("Navigating to Music: {}", musicPath);
        File music = new File(musicPath);
        if (music.exists()) {
            klasoreGit(music);
        } else {
            logger.warn("Music folder not found: {}", musicPath);
        }
    }
    
    public boolean videoDosyasiMi(File file) {
        if (file == null || !file.isFile()) return false;
        
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".mp4") || fileName.endsWith(".avi") || 
               fileName.endsWith(".mkv") || fileName.endsWith(".mov") || 
               fileName.endsWith(".wmv") || fileName.endsWith(".flv") || 
               fileName.endsWith(".webm");
    }
    
    public boolean isVideoFile(File file) {
        return videoDosyasiMi(file);
    }
    
    public boolean audioDosyasiMi(File file) {
        if (file == null || !file.isFile()) return false;
        
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".mp3") || fileName.endsWith(".wav") || 
               fileName.endsWith(".flac") || fileName.endsWith(".aac") || 
               fileName.endsWith(".ogg") || fileName.endsWith(".wma");
    }
    
    public boolean isAudioFile(File file) {
        return audioDosyasiMi(file);
    }
    
    public boolean medyaDosyasiMi(File file) {
        return videoDosyasiMi(file) || audioDosyasiMi(file);
    }
    
    public boolean isMediaFile(File file) {
        return medyaDosyasiMi(file);
    }
} 