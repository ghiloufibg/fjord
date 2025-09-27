package com.fjord.ide.editor;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class EditorManager {
    private final TabPane tabPane;
    private final Map<Tab, FjordEditor> editorMap;

    public EditorManager() {
        this.tabPane = new TabPane();
        this.editorMap = new HashMap<>();

        setupTabPane();
    }

    private void setupTabPane() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    public void newFile() {
        String defaultName = "Untitled" + (tabPane.getTabs().size() + 1) + ".fj";
        FjordEditor editor = new FjordEditor();
        createTab(defaultName, editor, null);
    }

    public void openFile(Stage parentStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Fjord File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Fjord Files", "*.fj"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(parentStage);
        if (selectedFile != null) {
            openFile(selectedFile);
        }
    }

    public void openFile(File file) {
        // Check if file is already open
        for (Map.Entry<Tab, FjordEditor> entry : editorMap.entrySet()) {
            FjordEditor editor = entry.getValue();
            if (file.equals(editor.getFile())) {
                // File already open, switch to that tab
                tabPane.getSelectionModel().select(entry.getKey());
                return;
            }
        }

        try {
            String content = Files.readString(file.toPath());
            FjordEditor editor = new FjordEditor();
            editor.setContent(content);
            editor.setFile(file);
            createTab(file.getName(), editor, file);
        } catch (IOException e) {
            System.err.println("Failed to open file: " + e.getMessage());
        }
    }

    private void createTab(String title, FjordEditor editor, File file) {
        Tab tab = new Tab(title);
        tab.setContent(editor.getCodeArea());

        // Mark tab as modified when content changes
        editor.getCodeArea().textProperty().addListener((obs, oldText, newText) -> {
            if (editor.isModified() && !tab.getText().endsWith("*")) {
                tab.setText(tab.getText() + "*");
            }
        });

        editorMap.put(tab, editor);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        // Remove from map when tab is closed
        tab.setOnClosed(e -> {
            FjordEditor closingEditor = editorMap.remove(tab);
            if (closingEditor != null) {
                closingEditor.dispose();
            }
        });
    }

    public void saveCurrentFile() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            FjordEditor editor = editorMap.get(currentTab);
            if (editor != null) {
                if (editor.getFile() != null) {
                    saveEditor(editor, currentTab);
                } else {
                    // No file associated, need to save as
                    saveAsCurrentFile(null);
                }
            }
        }
    }

    public void saveAsCurrentFile(Stage parentStage) {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            FjordEditor editor = editorMap.get(currentTab);
            if (editor != null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Fjord File");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Fjord Files", "*.fj"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
                );

                File selectedFile = fileChooser.showSaveDialog(parentStage);
                if (selectedFile != null) {
                    editor.setFile(selectedFile);
                    saveEditor(editor, currentTab);
                    currentTab.setText(selectedFile.getName());
                }
            }
        }
    }

    private void saveEditor(FjordEditor editor, Tab tab) {
        try {
            Files.writeString(editor.getFile().toPath(), editor.getContent());
            editor.setModified(false);
            // Remove the * marking from tab title
            String title = tab.getText();
            if (title.endsWith("*")) {
                tab.setText(title.substring(0, title.length() - 1));
            }
        } catch (IOException e) {
            System.err.println("Failed to save file: " + e.getMessage());
        }
    }

    public void undo() {
        FjordEditor currentEditor = getCurrentEditor();
        if (currentEditor != null) {
            currentEditor.getCodeArea().undo();
        }
    }

    public void redo() {
        FjordEditor currentEditor = getCurrentEditor();
        if (currentEditor != null) {
            currentEditor.getCodeArea().redo();
        }
    }

    public void cut() {
        FjordEditor currentEditor = getCurrentEditor();
        if (currentEditor != null) {
            currentEditor.getCodeArea().cut();
        }
    }

    public void copy() {
        FjordEditor currentEditor = getCurrentEditor();
        if (currentEditor != null) {
            currentEditor.getCodeArea().copy();
        }
    }

    public void paste() {
        FjordEditor currentEditor = getCurrentEditor();
        if (currentEditor != null) {
            currentEditor.getCodeArea().paste();
        }
    }

    public void showFindDialog() {
        // TODO: Implement find dialog
        System.out.println("Find dialog not yet implemented");
    }

    public FjordEditor getCurrentEditor() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        return currentTab != null ? editorMap.get(currentTab) : null;
    }

    public String getCurrentFileContent() {
        FjordEditor currentEditor = getCurrentEditor();
        return currentEditor != null ? currentEditor.getContent() : null;
    }

    public TabPane getRoot() {
        return tabPane;
    }
}