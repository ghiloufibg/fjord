package com.fjord.ide.project;

import com.fjord.ide.editor.EditorManager;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;

public class ProjectExplorer {
    private final VBox root;
    private final TreeView<File> fileTree;
    private File currentProjectRoot;
    private EditorManager editorManager;

    public ProjectExplorer() {
        this.root = new VBox();
        this.fileTree = new TreeView<>();

        setupUI();
        setupEventHandlers();
    }

    private void setupUI() {
        Label titleLabel = new Label("Project Explorer");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");

        fileTree.setPrefWidth(250);
        fileTree.setShowRoot(false);

        // Custom cell factory to show only file names
        fileTree.setCellFactory(tv -> new TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    // You could add icons here for different file types
                }
            }
        });

        root.getChildren().addAll(titleLabel, fileTree);
    }

    private void setupEventHandlers() {
        // Double-click to open file
        fileTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<File> selectedItem = fileTree.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.getValue().isFile()) {
                    openFile(selectedItem.getValue());
                }
            }
        });
    }

    public void openFolder(Stage parentStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Project Folder");

        File selectedDirectory = directoryChooser.showDialog(parentStage);
        if (selectedDirectory != null) {
            loadProject(selectedDirectory);
        }
    }

    private void loadProject(File projectRoot) {
        this.currentProjectRoot = projectRoot;

        TreeItem<File> rootItem = createTreeItem(projectRoot);
        fileTree.setRoot(rootItem);
        fileTree.setShowRoot(true);

        // Expand the root by default
        rootItem.setExpanded(true);
    }

    private TreeItem<File> createTreeItem(File file) {
        TreeItem<File> item = new TreeItem<>(file);

        if (file.isDirectory()) {
            // Lazy loading - only load children when expanded
            item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                if (isExpanded && item.getChildren().isEmpty()) {
                    loadChildren(item);
                }
            });

            // Add a dummy child to show the expand arrow
            item.getChildren().add(new TreeItem<>());
        }

        return item;
    }

    private void loadChildren(TreeItem<File> parentItem) {
        File parentFile = parentItem.getValue();
        File[] children = parentFile.listFiles();

        if (children != null) {
            // Clear dummy child
            parentItem.getChildren().clear();

            // Sort files - directories first, then by name
            Arrays.sort(children, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File child : children) {
                // Only show .fj files and directories
                if (child.isDirectory() || child.getName().endsWith(".fj")) {
                    parentItem.getChildren().add(createTreeItem(child));
                }
            }
        }
    }

    private void openFile(File file) {
        if (editorManager != null) {
            editorManager.openFile(file);
        } else {
            System.out.println("EditorManager not set - cannot open file: " + file.getAbsolutePath());
        }
    }

    public void setEditorManager(EditorManager editorManager) {
        this.editorManager = editorManager;
    }

    public VBox getRoot() {
        return root;
    }

    public File getCurrentProjectRoot() {
        return currentProjectRoot;
    }
}