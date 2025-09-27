package com.fjord.ide.ui;

import com.fjord.ide.navigation.NavigationTarget;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;

public class FindReferencesPanel {
    private final VBox root;
    private final TableView<ReferenceItem> referencesTable;
    private final ObservableList<ReferenceItem> references = FXCollections.observableArrayList();
    private final Label titleLabel;
    private BiConsumer<File, Integer> navigationCallback;

    public FindReferencesPanel() {
        root = new VBox();
        titleLabel = new Label("Find References");
        referencesTable = new TableView<>(references);
        setupUI();
    }

    private void setupUI() {
        titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");

        // Setup table columns
        TableColumn<ReferenceItem, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setPrefWidth(80);

        TableColumn<ReferenceItem, String> fileColumn = new TableColumn<>("File");
        fileColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileColumn.setPrefWidth(150);

        TableColumn<ReferenceItem, String> lineColumn = new TableColumn<>("Line");
        lineColumn.setCellValueFactory(new PropertyValueFactory<>("line"));
        lineColumn.setPrefWidth(60);

        TableColumn<ReferenceItem, String> contextColumn = new TableColumn<>("Context");
        contextColumn.setCellValueFactory(new PropertyValueFactory<>("context"));
        contextColumn.setPrefWidth(300);

        referencesTable.getColumns().addAll(typeColumn, fileColumn, lineColumn, contextColumn);

        // Custom row factory for navigation
        referencesTable.setRowFactory(tv -> {
            TableRow<ReferenceItem> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && row.getItem() != null) {
                    navigateToReference(row.getItem());
                }
            });

            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setStyle("");
                } else {
                    switch (newItem.getType().toLowerCase()) {
                        case "definition" -> row.setStyle("-fx-font-weight: bold; -fx-text-fill: #0066cc;");
                        case "reference" -> row.setStyle("-fx-text-fill: #333333;");
                        default -> row.setStyle("");
                    }
                }
            });

            return row;
        });

        referencesTable.setPrefHeight(200);

        // Context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem goToItem = new MenuItem("Go to Location");
        MenuItem copyItem = new MenuItem("Copy Context");

        goToItem.setOnAction(e -> {
            ReferenceItem selected = referencesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                navigateToReference(selected);
            }
        });

        copyItem.setOnAction(e -> {
            ReferenceItem selected = referencesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(
                    java.util.Map.of(javafx.scene.input.DataFormat.PLAIN_TEXT, selected.getContext())
                );
            }
        });

        contextMenu.getItems().addAll(goToItem, copyItem);
        referencesTable.setContextMenu(contextMenu);

        root.getChildren().addAll(titleLabel, referencesTable);
    }

    public void setNavigationCallback(BiConsumer<File, Integer> callback) {
        this.navigationCallback = callback;
    }

    public void showReferences(String symbolName, List<NavigationTarget> targets) {
        references.clear();

        titleLabel.setText("Find References - " + symbolName + " (" + targets.size() + " found)");

        for (NavigationTarget target : targets) {
            ReferenceItem item = new ReferenceItem(
                target.getType().toString(),
                target.getFile() != null ? target.getFile().getName() : "Unknown",
                String.valueOf(target.getLine()),
                target.getContext() != null ? target.getContext().trim() : "",
                target.getFile(),
                target.getLine()
            );
            references.add(item);
        }

        // Sort by file, then by line
        references.sort((a, b) -> {
            int fileCompare = a.getFileName().compareToIgnoreCase(b.getFileName());
            if (fileCompare != 0) return fileCompare;
            return Integer.compare(a.getLineNumber(), b.getLineNumber());
        });
    }

    public void clear() {
        references.clear();
        titleLabel.setText("Find References");
    }

    private void navigateToReference(ReferenceItem item) {
        if (navigationCallback != null && item.getFile() != null) {
            navigationCallback.accept(item.getFile(), item.getLineNumber());
        }
    }

    public VBox getRoot() {
        return root;
    }

    public static class ReferenceItem {
        private final String type;
        private final String fileName;
        private final String line;
        private final String context;
        private final File file;
        private final int lineNumber;

        public ReferenceItem(String type, String fileName, String line, String context, File file, int lineNumber) {
            this.type = type;
            this.fileName = fileName;
            this.line = line;
            this.context = context;
            this.file = file;
            this.lineNumber = lineNumber;
        }

        public String getType() { return type; }
        public String getFileName() { return fileName; }
        public String getLine() { return line; }
        public String getContext() { return context; }
        public File getFile() { return file; }
        public int getLineNumber() { return lineNumber; }
    }
}