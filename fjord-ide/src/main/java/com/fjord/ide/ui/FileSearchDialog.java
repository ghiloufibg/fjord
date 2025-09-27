package com.fjord.ide.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FileSearchDialog {
    private final Stage stage;
    private final TextField searchField;
    private final ListView<FileItem> resultsList;
    private final Label statusLabel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FileSearch");
        t.setDaemon(true);
        return t;
    });

    private File projectRoot;
    private Consumer<File> fileOpenCallback;
    private Task<List<FileItem>> currentSearchTask;

    public FileSearchDialog(Stage parentStage) {
        stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.initOwner(parentStage);
        stage.setTitle("Go to File");
        stage.setWidth(600);
        stage.setHeight(400);

        searchField = new TextField();
        searchField.setPromptText("Type file name...");

        resultsList = new ListView<>();
        resultsList.setCellFactory(listView -> new FileListCell());

        statusLabel = new Label("Type to search for files");

        setupLayout();
        setupEventHandlers();
    }

    private void setupLayout() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: search field
        HBox searchBox = new HBox(10);
        searchBox.getChildren().addAll(new Label("File name:"), searchField);
        root.setTop(searchBox);

        // Center: results list
        root.setCenter(resultsList);

        // Bottom: status
        root.setBottom(statusLabel);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private void setupEventHandlers() {
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.trim().isEmpty()) {
                resultsList.getItems().clear();
                statusLabel.setText("Type to search for files");
            } else {
                performSearch(newText.trim());
            }
        });

        searchField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DOWN -> {
                    if (!resultsList.getItems().isEmpty()) {
                        resultsList.requestFocus();
                        resultsList.getSelectionModel().selectFirst();
                    }
                    event.consume();
                }
                case ENTER -> {
                    openSelectedFile();
                    event.consume();
                }
                case ESCAPE -> {
                    stage.close();
                    event.consume();
                }
            }
        });

        resultsList.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    openSelectedFile();
                    event.consume();
                }
                case ESCAPE -> {
                    searchField.requestFocus();
                    event.consume();
                }
                case UP -> {
                    if (resultsList.getSelectionModel().getSelectedIndex() == 0) {
                        searchField.requestFocus();
                        event.consume();
                    }
                }
            }
        });

        resultsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openSelectedFile();
            }
        });
    }

    public void show(File projectRoot, Consumer<File> fileOpenCallback) {
        this.projectRoot = projectRoot;
        this.fileOpenCallback = fileOpenCallback;

        resultsList.getItems().clear();
        searchField.clear();
        statusLabel.setText("Type to search for files");

        stage.show();
        searchField.requestFocus();
    }

    private void performSearch(String query) {
        // Cancel previous search
        if (currentSearchTask != null && !currentSearchTask.isDone()) {
            currentSearchTask.cancel(true);
        }

        statusLabel.setText("Searching...");

        currentSearchTask = new Task<List<FileItem>>() {
            @Override
            protected List<FileItem> call() throws Exception {
                return searchFiles(query);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<FileItem> results = getValue();
                    resultsList.getItems().clear();
                    resultsList.getItems().addAll(results);

                    if (results.isEmpty()) {
                        statusLabel.setText("No files found");
                    } else {
                        statusLabel.setText(results.size() + " file(s) found");
                        resultsList.getSelectionModel().selectFirst();
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    statusLabel.setText("Search failed");
                });
            }
        };

        executor.submit(currentSearchTask);
    }

    private List<FileItem> searchFiles(String query) {
        List<FileItem> results = new ArrayList<>();

        if (projectRoot == null || !projectRoot.exists()) {
            return results;
        }

        String lowerQuery = query.toLowerCase();

        try (Stream<Path> paths = Files.walk(projectRoot.toPath())) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".fj"))
                 .filter(path -> {
                     String fileName = path.getFileName().toString().toLowerCase();
                     return fileName.contains(lowerQuery);
                 })
                 .limit(100) // Limit results to avoid UI overload
                 .forEach(path -> {
                     File file = path.toFile();
                     String relativePath = projectRoot.toPath().relativize(path).toString();
                     results.add(new FileItem(file, relativePath));
                 });
        } catch (Exception e) {
            // Handle search errors gracefully
        }

        // Sort by relevance (exact name matches first)
        results.sort((a, b) -> {
            boolean aExact = a.fileName.toLowerCase().equals(lowerQuery);
            boolean bExact = b.fileName.toLowerCase().equals(lowerQuery);
            if (aExact && !bExact) return -1;
            if (!aExact && bExact) return 1;

            boolean aStarts = a.fileName.toLowerCase().startsWith(lowerQuery);
            boolean bStarts = b.fileName.toLowerCase().startsWith(lowerQuery);
            if (aStarts && !bStarts) return -1;
            if (!aStarts && bStarts) return 1;

            return a.fileName.compareToIgnoreCase(b.fileName);
        });

        return results;
    }

    private void openSelectedFile() {
        FileItem selected = resultsList.getSelectionModel().getSelectedItem();
        if (selected != null && fileOpenCallback != null) {
            fileOpenCallback.accept(selected.file);
            stage.close();
        }
    }

    public void shutdown() {
        if (currentSearchTask != null) {
            currentSearchTask.cancel(true);
        }
        executor.shutdown();
    }

    private static class FileItem {
        final File file;
        final String fileName;
        final String relativePath;

        FileItem(File file, String relativePath) {
            this.file = file;
            this.fileName = file.getName();
            this.relativePath = relativePath;
        }
    }

    private static class FileListCell extends ListCell<FileItem> {
        @Override
        protected void updateItem(FileItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.fileName);
                setTooltip(new Tooltip(item.relativePath));

                // Highlight matching part (simplified)
                setStyle("-fx-font-family: monospace;");
            }
        }
    }
}