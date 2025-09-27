package com.fjord.ide.project;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RecentFilesManager {
    private static final int MAX_RECENT_FILES = 10;
    private static final String RECENT_FILES_FILE = ".fjord-ide-recent";

    private final List<String> recentFiles = new ArrayList<>();
    private final Consumer<File> fileOpenCallback;
    private Menu recentFilesMenu;

    public RecentFilesManager(Consumer<File> fileOpenCallback) {
        this.fileOpenCallback = fileOpenCallback;
        loadRecentFiles();
    }

    public void setRecentFilesMenu(Menu menu) {
        this.recentFilesMenu = menu;
        updateMenu();
    }

    public void addRecentFile(File file) {
        if (file == null || !file.exists()) return;

        String filePath = file.getAbsolutePath();

        // Remove if already exists
        recentFiles.remove(filePath);

        // Add to front
        recentFiles.add(0, filePath);

        // Limit size
        while (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(recentFiles.size() - 1);
        }

        saveRecentFiles();
        updateMenu();
    }

    public void removeRecentFile(File file) {
        if (file == null) return;

        recentFiles.remove(file.getAbsolutePath());
        saveRecentFiles();
        updateMenu();
    }

    public List<String> getRecentFiles() {
        return new ArrayList<>(recentFiles);
    }

    public void clearRecentFiles() {
        recentFiles.clear();
        saveRecentFiles();
        updateMenu();
    }

    private void updateMenu() {
        if (recentFilesMenu == null) return;

        recentFilesMenu.getItems().clear();

        if (recentFiles.isEmpty()) {
            MenuItem noRecentItem = new MenuItem("No recent files");
            noRecentItem.setDisable(true);
            recentFilesMenu.getItems().add(noRecentItem);
        } else {
            // Add recent files
            for (int i = 0; i < recentFiles.size(); i++) {
                String filePath = recentFiles.get(i);
                File file = new File(filePath);

                MenuItem item = new MenuItem();
                item.setText((i + 1) + ". " + file.getName());

                // Show full path in tooltip
                item.setUserData(file);

                item.setOnAction(e -> {
                    File selectedFile = (File) item.getUserData();
                    if (selectedFile.exists()) {
                        fileOpenCallback.accept(selectedFile);
                    } else {
                        // File no longer exists, remove from recent
                        removeRecentFile(selectedFile);
                    }
                });

                recentFilesMenu.getItems().add(item);
            }

            // Add separator and clear option
            recentFilesMenu.getItems().add(new SeparatorMenuItem());

            MenuItem clearItem = new MenuItem("Clear Recent Files");
            clearItem.setOnAction(e -> clearRecentFiles());
            recentFilesMenu.getItems().add(clearItem);
        }
    }

    private void loadRecentFiles() {
        try {
            Path recentFilesPath = getRecentFilesPath();
            if (Files.exists(recentFilesPath)) {
                List<String> lines = Files.readAllLines(recentFilesPath);
                for (String line : lines) {
                    if (!line.trim().isEmpty() && new File(line).exists()) {
                        recentFiles.add(line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load recent files: " + e.getMessage());
        }
    }

    private void saveRecentFiles() {
        try {
            Path recentFilesPath = getRecentFilesPath();
            Files.write(recentFilesPath, recentFiles);
        } catch (IOException e) {
            System.err.println("Failed to save recent files: " + e.getMessage());
        }
    }

    private Path getRecentFilesPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, RECENT_FILES_FILE);
    }
}