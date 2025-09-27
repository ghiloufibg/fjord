package com.fjord.ide.ui;

import com.fjord.ide.project.ProjectExplorer;
import com.fjord.ide.editor.EditorManager;
import com.fjord.ide.integration.FjordRunner;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainWindow {
    private final Stage primaryStage;
    private final BorderPane root;
    private final MenuBar menuBar;
    private final ToolBar toolBar;
    private final ProjectExplorer projectExplorer;
    private final EditorManager editorManager;
    private final TextArea outputConsole;
    private final FjordRunner fjordRunner;

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.root = new BorderPane();

        // Initialize components
        this.menuBar = createMenuBar();
        this.toolBar = createToolBar();
        this.projectExplorer = new ProjectExplorer();
        this.editorManager = new EditorManager();
        this.outputConsole = createOutputConsole();
        this.fjordRunner = new FjordRunner(this::appendToConsole);

        // Connect components
        this.projectExplorer.setEditorManager(this.editorManager);

        setupLayout();
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

        // Bottom: Output console
        root.setBottom(outputConsole);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newFile = new MenuItem("New File");
        MenuItem openFile = new MenuItem("Open File...");
        MenuItem openFolder = new MenuItem("Open Folder...");
        MenuItem save = new MenuItem("Save");
        MenuItem saveAs = new MenuItem("Save As...");
        MenuItem exit = new MenuItem("Exit");

        newFile.setOnAction(e -> editorManager.newFile());
        openFile.setOnAction(e -> editorManager.openFile(primaryStage));
        openFolder.setOnAction(e -> projectExplorer.openFolder(primaryStage));
        save.setOnAction(e -> editorManager.saveCurrentFile());
        saveAs.setOnAction(e -> editorManager.saveAsCurrentFile(primaryStage));
        exit.setOnAction(e -> primaryStage.close());

        fileMenu.getItems().addAll(newFile, openFile, openFolder,
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

        undo.setOnAction(e -> editorManager.undo());
        redo.setOnAction(e -> editorManager.redo());
        cut.setOnAction(e -> editorManager.cut());
        copy.setOnAction(e -> editorManager.copy());
        paste.setOnAction(e -> editorManager.paste());
        find.setOnAction(e -> editorManager.showFindDialog());

        editMenu.getItems().addAll(undo, redo, new SeparatorMenuItem(),
                                  cut, copy, paste, new SeparatorMenuItem(),
                                  find);

        // Run menu
        Menu runMenu = new Menu("Run");
        MenuItem runFile = new MenuItem("Run Current File");
        MenuItem compileFile = new MenuItem("Compile Current File");
        MenuItem runWithBytecode = new MenuItem("Run with Bytecode VM");

        runFile.setOnAction(e -> runCurrentFile());
        compileFile.setOnAction(e -> compileCurrentFile());
        runWithBytecode.setOnAction(e -> runCurrentFileWithBytecode());

        runMenu.getItems().addAll(runFile, compileFile, runWithBytecode);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem about = new MenuItem("About Fjord IDE");
        about.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(about);

        menuBar.getMenus().addAll(fileMenu, editMenu, runMenu, helpMenu);
        return menuBar;
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        Button newFileBtn = new Button("New");
        Button openFileBtn = new Button("Open");
        Button saveBtn = new Button("Save");
        Button runBtn = new Button("Run");
        Button compileBtn = new Button("Compile");

        newFileBtn.setOnAction(e -> editorManager.newFile());
        openFileBtn.setOnAction(e -> editorManager.openFile(primaryStage));
        saveBtn.setOnAction(e -> editorManager.saveCurrentFile());
        runBtn.setOnAction(e -> runCurrentFile());
        compileBtn.setOnAction(e -> compileCurrentFile());

        toolBar.getItems().addAll(newFileBtn, openFileBtn, saveBtn,
                                 new Separator(), runBtn, compileBtn);

        return toolBar;
    }

    private TextArea createOutputConsole() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setPrefHeight(150);
        console.appendText("Fjord IDE Console\n");
        console.appendText("Ready.\n");
        return console;
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

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Fjord IDE");
        alert.setHeaderText("Fjord IDE v0.1.0");
        alert.setContentText("A simple IDE for the Fjord programming language.\n\n" +
                             "Built with JavaFX and integrated with the Fjord compiler.\n" +
                             "Features syntax highlighting, error checking, and integrated execution.");
        alert.showAndWait();
    }

    public void appendToConsole(String text) {
        outputConsole.appendText(text);
    }

    public void clearConsole() {
        outputConsole.clear();
    }

    public BorderPane getRoot() {
        return root;
    }
}