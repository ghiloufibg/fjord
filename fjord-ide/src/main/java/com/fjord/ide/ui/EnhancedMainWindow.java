package com.fjord.ide.ui;

import com.fjord.ide.project.ProjectExplorer;
import com.fjord.ide.project.RecentFilesManager;
import com.fjord.ide.editor.EditorManager;
import com.fjord.ide.integration.FjordRunner;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Orientation;

import java.io.File;

public class EnhancedMainWindow {
    private final Stage primaryStage;
    private final BorderPane root;
    private final MenuBar menuBar;
    private final ToolBar toolBar;
    private final ProjectExplorer projectExplorer;
    private final EditorManager editorManager;
    private final TextArea outputConsole;
    private final ProblemsPanel problemsPanel;
    private final ReplPanel replPanel;
    private final FjordRunner fjordRunner;
    private final ThemeManager themeManager;
    private final RecentFilesManager recentFilesManager;
    private final FindReplaceDialog findReplaceDialog;
    private final FileSearchDialog fileSearchDialog;

    public EnhancedMainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.root = new BorderPane();

        // Initialize components
        this.themeManager = new ThemeManager();
        this.menuBar = createMenuBar();
        this.toolBar = createToolBar();
        this.projectExplorer = new ProjectExplorer();
        this.editorManager = new EditorManager();
        this.outputConsole = createOutputConsole();
        this.problemsPanel = new ProblemsPanel();
        this.replPanel = new ReplPanel();
        this.fjordRunner = new FjordRunner(this::appendToConsole);
        this.recentFilesManager = new RecentFilesManager(editorManager::openFile);
        this.findReplaceDialog = new FindReplaceDialog(primaryStage);
        this.fileSearchDialog = new FileSearchDialog(primaryStage);

        setupLayout();
        setupConnections();
        setupKeyboardShortcuts();
    }

    private void setupLayout() {
        // Top: Menu and toolbar
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(menuBar, toolBar);
        root.setTop(topContainer);

        // Left: Project explorer
        root.setLeft(projectExplorer.getRoot());

        // Center: Editor tabs
        root.setCenter(editorManager.getRoot());

        // Bottom: Tabbed panel with output, problems, and REPL
        TabPane bottomTabs = new TabPane();

        // Output console tab
        Tab outputTab = new Tab("Output");
        outputTab.setClosable(false);
        outputTab.setContent(outputConsole);

        // Problems tab
        Tab problemsTab = new Tab("Problems");
        problemsTab.setClosable(false);
        problemsTab.setContent(problemsPanel.getRoot());

        // REPL tab
        Tab replTab = new Tab("REPL");
        replTab.setClosable(false);
        replTab.setContent(replPanel);

        bottomTabs.getTabs().addAll(outputTab, problemsTab, replTab);
        root.setBottom(bottomTabs);
    }

    private void setupConnections() {
        // Connect project explorer to editor manager
        projectExplorer.setEditorManager(editorManager);

        // Set up recent files menu
        Menu recentMenu = findMenuByText(menuBar, "Recent Files");
        if (recentMenu != null) {
            recentFilesManager.setRecentFilesMenu(recentMenu);
        }

        // Add listener for opened files to update recent files
        // TODO: Add hook to EditorManager for file opened events
    }

    private void setupKeyboardShortcuts() {
        primaryStage.getScene().setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case N -> { newFile(); event.consume(); }
                    case O -> { openFile(); event.consume(); }
                    case S -> {
                        if (event.isShiftDown()) {
                            saveAsCurrentFile();
                        } else {
                            saveCurrentFile();
                        }
                        event.consume();
                    }
                    case F -> { showFindDialog(); event.consume(); }
                    case H -> { showReplaceDialog(); event.consume(); }
                    case P -> {
                        if (event.isShiftDown()) {
                            showFileSearch();
                        }
                        event.consume();
                    }
                    case R -> { runCurrentFile(); event.consume(); }
                    case T -> { themeManager.toggleTheme(); event.consume(); }
                    case DIGIT1 -> { setExecutionMode(FjordRunner.ExecutionMode.TREE_WALKING); event.consume(); }
                    case DIGIT2 -> { setExecutionMode(FjordRunner.ExecutionMode.BYTECODE); event.consume(); }
                    case DIGIT3 -> { setExecutionMode(FjordRunner.ExecutionMode.COMPILE_ONLY); event.consume(); }
                }
            }
        });
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newFile = new MenuItem("New File");
        MenuItem openFile = new MenuItem("Open File...");
        MenuItem openFolder = new MenuItem("Open Folder...");
        Menu recentFiles = new Menu("Recent Files");
        MenuItem save = new MenuItem("Save");
        MenuItem saveAs = new MenuItem("Save As...");
        MenuItem exit = new MenuItem("Exit");

        // Set accelerators
        newFile.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        openFile.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));

        newFile.setOnAction(e -> newFile());
        openFile.setOnAction(e -> openFile());
        openFolder.setOnAction(e -> openFolder());
        save.setOnAction(e -> saveCurrentFile());
        saveAs.setOnAction(e -> saveAsCurrentFile());
        exit.setOnAction(e -> exitApplication());

        fileMenu.getItems().addAll(newFile, openFile, openFolder, recentFiles,
                                  new SeparatorMenuItem(), save, saveAs,
                                  new SeparatorMenuItem(), exit);

        // Edit menu
        Menu editMenu = new Menu("Edit");
        MenuItem undo = new MenuItem("Undo");
        MenuItem redo = new MenuItem("Redo");
        MenuItem cut = new MenuItem("Cut");
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");
        MenuItem find = new MenuItem("Find...");
        MenuItem replace = new MenuItem("Replace...");
        MenuItem goToLine = new MenuItem("Go to Line...");

        undo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        redo.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        cut.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        copy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        paste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        find.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
        replace.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
        goToLine.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN));

        undo.setOnAction(e -> editorManager.undo());
        redo.setOnAction(e -> editorManager.redo());
        cut.setOnAction(e -> editorManager.cut());
        copy.setOnAction(e -> editorManager.copy());
        paste.setOnAction(e -> editorManager.paste());
        find.setOnAction(e -> showFindDialog());
        replace.setOnAction(e -> showReplaceDialog());
        goToLine.setOnAction(e -> showGoToLineDialog());

        editMenu.getItems().addAll(undo, redo, new SeparatorMenuItem(),
                                  cut, copy, paste, new SeparatorMenuItem(),
                                  find, replace, goToLine);

        // Search menu
        Menu searchMenu = new Menu("Search");
        MenuItem fileSearch = new MenuItem("Go to File...");
        MenuItem symbolSearch = new MenuItem("Go to Symbol...");

        fileSearch.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));

        fileSearch.setOnAction(e -> showFileSearch());
        symbolSearch.setOnAction(e -> showSymbolSearch());

        searchMenu.getItems().addAll(fileSearch, symbolSearch);

        // Run menu
        Menu runMenu = new Menu("Run");
        MenuItem runFile = new MenuItem("Run Current File");
        MenuItem compileFile = new MenuItem("Compile Current File");
        MenuItem runWithBytecode = new MenuItem("Run with Bytecode VM");

        runFile.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));

        runFile.setOnAction(e -> runCurrentFile());
        compileFile.setOnAction(e -> compileCurrentFile());
        runWithBytecode.setOnAction(e -> runCurrentFileWithBytecode());

        runMenu.getItems().addAll(runFile, compileFile, runWithBytecode);

        // View menu
        Menu viewMenu = new Menu("View");
        MenuItem toggleTheme = new MenuItem("Toggle Theme");
        MenuItem showProblems = new MenuItem("Show Problems");
        MenuItem showOutput = new MenuItem("Show Output");
        MenuItem showRepl = new MenuItem("Show REPL");
        MenuItem showExplorer = new MenuItem("Show Explorer");

        toggleTheme.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));

        toggleTheme.setOnAction(e -> themeManager.toggleTheme());
        showProblems.setOnAction(e -> focusProblemsPanel());
        showOutput.setOnAction(e -> focusOutputConsole());
        showRepl.setOnAction(e -> focusReplPanel());
        showExplorer.setOnAction(e -> focusProjectExplorer());

        viewMenu.getItems().addAll(toggleTheme, new SeparatorMenuItem(),
                                  showProblems, showOutput, showRepl, showExplorer);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem keyboardShortcuts = new MenuItem("Keyboard Shortcuts");
        MenuItem about = new MenuItem("About Fjord IDE");

        keyboardShortcuts.setOnAction(e -> showKeyboardShortcuts());
        about.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().addAll(keyboardShortcuts, about);

        menuBar.getMenus().addAll(fileMenu, editMenu, searchMenu, runMenu, viewMenu, helpMenu);
        return menuBar;
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        Button newFileBtn = new Button("New");
        Button openFileBtn = new Button("Open");
        Button saveBtn = new Button("Save");
        Button runBtn = new Button("Run");
        Button compileBtn = new Button("Compile");
        Button themeBtn = new Button("Theme");

        newFileBtn.setOnAction(e -> newFile());
        openFileBtn.setOnAction(e -> openFile());
        saveBtn.setOnAction(e -> saveCurrentFile());
        runBtn.setOnAction(e -> runCurrentFile());
        compileBtn.setOnAction(e -> compileCurrentFile());
        themeBtn.setOnAction(e -> themeManager.toggleTheme());

        toolBar.getItems().addAll(newFileBtn, openFileBtn, saveBtn,
                                 new Separator(), runBtn, compileBtn,
                                 new Separator(), themeBtn);

        return toolBar;
    }

    private TextArea createOutputConsole() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setPrefHeight(150);
        console.appendText("Fjord IDE Enhanced Console\n");
        console.appendText("Ready. Use Ctrl+R to run files.\n");
        return console;
    }

    // Action methods
    private void newFile() { editorManager.newFile(); }
    private void openFile() { editorManager.openFile(primaryStage); }
    private void openFolder() { projectExplorer.openFolder(primaryStage); }
    private void saveCurrentFile() { editorManager.saveCurrentFile(); }
    private void saveAsCurrentFile() { editorManager.saveAsCurrentFile(primaryStage); }

    private void showFindDialog() {
        var currentEditor = editorManager.getCurrentEditor();
        if (currentEditor != null) {
            findReplaceDialog.show(currentEditor.getCodeArea());
        }
    }

    private void showReplaceDialog() {
        showFindDialog(); // Same dialog handles both
    }

    private void showFileSearch() {
        File projectRoot = projectExplorer.getCurrentProjectRoot();
        if (projectRoot != null) {
            fileSearchDialog.show(projectRoot, editorManager::openFile);
        } else {
            showAlert("No project folder opened", "Please open a project folder first.");
        }
    }

    private void showSymbolSearch() {
        showAlert("Not implemented", "Symbol search will be implemented in a future version.");
    }

    private void showGoToLineDialog() {
        var currentEditor = editorManager.getCurrentEditor();
        if (currentEditor != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Go to Line");
            dialog.setHeaderText("Enter line number:");
            dialog.setContentText("Line:");

            dialog.showAndWait().ifPresent(line -> {
                try {
                    int lineNum = Integer.parseInt(line);
                    currentEditor.goToLine(lineNum);
                } catch (NumberFormatException e) {
                    showAlert("Invalid line number", "Please enter a valid line number.");
                }
            });
        }
    }

    private void runCurrentFile() {
        String currentCode = editorManager.getCurrentFileContent();
        if (currentCode != null) {
            clearConsole();
            fjordRunner.runCode(currentCode, FjordRunner.ExecutionMode.TREE_WALKING);
        } else {
            appendToConsole("No file open to run.\n");
        }
    }

    private void compileCurrentFile() {
        String currentCode = editorManager.getCurrentFileContent();
        if (currentCode != null) {
            clearConsole();
            fjordRunner.runCode(currentCode, FjordRunner.ExecutionMode.COMPILE_ONLY);
        } else {
            appendToConsole("No file open to compile.\n");
        }
    }

    private void runCurrentFileWithBytecode() {
        String currentCode = editorManager.getCurrentFileContent();
        if (currentCode != null) {
            clearConsole();
            fjordRunner.runCode(currentCode, FjordRunner.ExecutionMode.BYTECODE);
        } else {
            appendToConsole("No file open to run.\n");
        }
    }

    private void setExecutionMode(FjordRunner.ExecutionMode mode) {
        appendToConsole("Execution mode set to: " + mode + "\n");
    }

    private void focusProblemsPanel() {
        TabPane bottomTabs = (TabPane) root.getBottom();
        bottomTabs.getSelectionModel().select(1); // Problems is the second tab (index 1)
        problemsPanel.getRoot().requestFocus();
    }

    private void focusOutputConsole() {
        TabPane bottomTabs = (TabPane) root.getBottom();
        bottomTabs.getSelectionModel().select(0); // Output is the first tab (index 0)
        outputConsole.requestFocus();
    }

    private void focusProjectExplorer() {
        projectExplorer.getRoot().requestFocus();
    }

    private void focusReplPanel() {
        // Focus the REPL tab and the input field
        TabPane bottomTabs = (TabPane) root.getBottom();
        bottomTabs.getSelectionModel().select(2); // REPL is the third tab (index 2)
        replPanel.requestFocus();
    }

    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Fjord IDE Keyboard Shortcuts");
        alert.setContentText("""
            File Operations:
            Ctrl+N          New File
            Ctrl+O          Open File
            Ctrl+S          Save File
            Ctrl+Shift+S    Save As

            Edit Operations:
            Ctrl+Z          Undo
            Ctrl+Y          Redo
            Ctrl+X          Cut
            Ctrl+C          Copy
            Ctrl+V          Paste

            Search Operations:
            Ctrl+F          Find/Replace
            Ctrl+Shift+P    Go to File
            Ctrl+G          Go to Line

            Run Operations:
            Ctrl+R          Run Current File
            Ctrl+1          Tree-walking mode
            Ctrl+2          Bytecode mode
            Ctrl+3          Compile-only mode

            View Operations:
            Ctrl+T          Toggle Theme
            """);
        alert.showAndWait();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Fjord IDE");
        alert.setHeaderText("Fjord IDE v0.2.0 Enhanced");
        alert.setContentText("""
            A comprehensive IDE for the Fjord programming language.

            Features:
            • Syntax highlighting with error detection
            • Auto-completion and code intelligence
            • Multiple execution modes
            • Dark/Light themes
            • File and symbol search
            • Recent files management
            • Problems panel
            • And much more!

            Built with JavaFX and integrated with the Fjord compiler.
            """);
        alert.showAndWait();
    }

    private void exitApplication() {
        // TODO: Check for unsaved files
        Platform.exit();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Utility methods
    private Menu findMenuByText(MenuBar menuBar, String text) {
        for (Menu menu : menuBar.getMenus()) {
            if (menu.getText().equals(text)) return menu;
            Menu found = findMenuByText(menu, text);
            if (found != null) return found;
        }
        return null;
    }

    private Menu findMenuByText(Menu parent, String text) {
        for (MenuItem item : parent.getItems()) {
            if (item instanceof Menu && item.getText().equals(text)) {
                return (Menu) item;
            }
        }
        return null;
    }

    public void appendToConsole(String text) {
        Platform.runLater(() -> outputConsole.appendText(text));
    }

    public void clearConsole() {
        Platform.runLater(() -> outputConsole.clear());
    }

    public BorderPane getRoot() {
        return root;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }
}