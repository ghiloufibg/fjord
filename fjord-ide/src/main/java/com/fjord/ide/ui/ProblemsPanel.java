package com.fjord.ide.ui;

import com.fjord.error.FjordError;
import com.fjord.error.SourcePosition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;

public class ProblemsPanel {
    private final VBox root;
    private final TableView<ProblemItem> problemTable;
    private final ObservableList<ProblemItem> problems = FXCollections.observableArrayList();

    public ProblemsPanel() {
        root = new VBox();
        problemTable = new TableView<>(problems);
        setupUI();
    }

    private void setupUI() {
        Label titleLabel = new Label("Problems");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");

        // Setup table columns
        TableColumn<ProblemItem, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setPrefWidth(60);

        TableColumn<ProblemItem, String> messageColumn = new TableColumn<>("Message");
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        messageColumn.setPrefWidth(300);

        TableColumn<ProblemItem, String> fileColumn = new TableColumn<>("File");
        fileColumn.setCellValueFactory(new PropertyValueFactory<>("file"));
        fileColumn.setPrefWidth(150);

        TableColumn<ProblemItem, String> lineColumn = new TableColumn<>("Line");
        lineColumn.setCellValueFactory(new PropertyValueFactory<>("line"));
        lineColumn.setPrefWidth(60);

        TableColumn<ProblemItem, String> columnColumn = new TableColumn<>("Column");
        columnColumn.setCellValueFactory(new PropertyValueFactory<>("column"));
        columnColumn.setPrefWidth(60);

        problemTable.getColumns().addAll(typeColumn, messageColumn, fileColumn, lineColumn, columnColumn);

        // Custom row factory for icons and colors
        problemTable.setRowFactory(tv -> {
            TableRow<ProblemItem> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setStyle("");
                } else {
                    switch (newItem.getType().toLowerCase()) {
                        case "error" -> row.setStyle("-fx-text-fill: #d32f2f;");
                        case "warning" -> row.setStyle("-fx-text-fill: #f57c00;");
                        case "info" -> row.setStyle("-fx-text-fill: #1976d2;");
                        default -> row.setStyle("");
                    }
                }
            });

            // Double-click to navigate to problem
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && row.getItem() != null) {
                    navigateToProblem(row.getItem());
                }
            });

            return row;
        });

        problemTable.setPrefHeight(150);

        // Context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem clearItem = new MenuItem("Clear All");
        clearItem.setOnAction(e -> clearProblems());
        contextMenu.getItems().add(clearItem);
        problemTable.setContextMenu(contextMenu);

        root.getChildren().addAll(titleLabel, problemTable);
    }

    public void updateProblems(List<FjordError> errors, File currentFile) {
        problems.clear();

        for (FjordError error : errors) {
            ProblemItem item = new ProblemItem(
                "Error",
                error.message,
                currentFile != null ? currentFile.getName() : "Unknown",
                String.valueOf(error.position.line),
                String.valueOf(error.position.column),
                error.position
            );
            problems.add(item);
        }

        // Update title with count
        Label titleLabel = (Label) root.getChildren().get(0);
        int errorCount = (int) problems.stream().filter(p -> "Error".equals(p.getType())).count();
        int warningCount = (int) problems.stream().filter(p -> "Warning".equals(p.getType())).count();

        StringBuilder title = new StringBuilder("Problems");
        if (errorCount > 0 || warningCount > 0) {
            title.append(" (");
            if (errorCount > 0) {
                title.append(errorCount).append(" error").append(errorCount > 1 ? "s" : "");
            }
            if (warningCount > 0) {
                if (errorCount > 0) title.append(", ");
                title.append(warningCount).append(" warning").append(warningCount > 1 ? "s" : "");
            }
            title.append(")");
        }
        titleLabel.setText(title.toString());
    }

    public void clearProblems() {
        problems.clear();
        Label titleLabel = (Label) root.getChildren().get(0);
        titleLabel.setText("Problems");
    }

    private void navigateToProblem(ProblemItem problem) {
        // TODO: Implement navigation to specific line/column
        System.out.println("Navigate to: " + problem.getFile() +
                         " line " + problem.getLine() +
                         " column " + problem.getColumn());
    }

    public VBox getRoot() {
        return root;
    }

    public static class ProblemItem {
        private final String type;
        private final String message;
        private final String file;
        private final String line;
        private final String column;
        private final SourcePosition position;

        public ProblemItem(String type, String message, String file, String line, String column, SourcePosition position) {
            this.type = type;
            this.message = message;
            this.file = file;
            this.line = line;
            this.column = column;
            this.position = position;
        }

        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getFile() { return file; }
        public String getLine() { return line; }
        public String getColumn() { return column; }
        public SourcePosition getPosition() { return position; }
    }
}