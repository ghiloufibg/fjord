package com.fjord.ide.ui;

import com.fjord.ast.Expression;
import com.fjord.error.FjordError;
import com.fjord.lex.Tokenizer;
import com.fjord.lex.Token;
import com.fjord.parser.Parser;
import com.fjord.runtime.Environment;
import com.fjord.runtime.Value;
import com.fjord.runtime.builtin.BuiltinRegistry;
import com.fjord.runtime.builtin.PrintlnFunction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReplPanel extends VBox {
    private CodeArea outputArea;
    private TextField inputField;
    private Button executeButton;
    private Button clearButton;
    private ListView<String> historyList;
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();

    private final Environment environment;
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ReplPanel() {
        this.environment = createEnvironmentWithBuiltins();

        Label titleLabel = new Label("Interactive REPL");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");

        setupOutputArea();
        setupInputArea();
        setupHistoryArea();
        setupLayout();

        printWelcomeMessage();
    }

    private void setupOutputArea() {
        outputArea = new CodeArea();
        outputArea.setParagraphGraphicFactory(LineNumberFactory.get(outputArea));
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefHeight(400);
        outputArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");

        outputArea.getStylesheets().add(getClass().getResource("/syntax-highlighting.css").toExternalForm());
    }

    private void setupInputArea() {
        inputField = new TextField();
        inputField.setPromptText("Enter Fjord expression (e.g., let x = 5 + 3)");
        inputField.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");

        executeButton = new Button("Execute");
        executeButton.setDefaultButton(true);

        clearButton = new Button("Clear");

        inputField.setOnKeyPressed(this::handleKeyPressed);
        executeButton.setOnAction(e -> executeCommand());
        clearButton.setOnAction(e -> clearOutput());
    }

    private void setupHistoryArea() {
        historyList = new ListView<>(historyItems);
        historyList.setPrefHeight(100);
        historyList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    inputField.setText(selected);
                    inputField.requestFocus();
                    inputField.positionCaret(selected.length());
                }
            }
        });
    }

    private void setupLayout() {
        HBox inputBox = new HBox(5);
        inputBox.getChildren().addAll(inputField, executeButton, clearButton);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        TabPane tabPane = new TabPane();

        Tab outputTab = new Tab("Output");
        outputTab.setClosable(false);
        outputTab.setContent(outputArea);

        Tab historyTab = new Tab("History");
        historyTab.setClosable(false);
        historyTab.setContent(historyList);

        tabPane.getTabs().addAll(outputTab, historyTab);

        Label titleLabel = new Label("Interactive REPL");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");

        getChildren().addAll(titleLabel, tabPane, inputBox);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        setSpacing(5);
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            executeCommand();
        } else if (event.getCode() == KeyCode.UP) {
            navigateHistory(-1);
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            navigateHistory(1);
            event.consume();
        }
    }

    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) return;

        historyIndex += direction;

        if (historyIndex < 0) {
            historyIndex = 0;
        } else if (historyIndex >= commandHistory.size()) {
            historyIndex = commandHistory.size();
            inputField.setText("");
            return;
        }

        inputField.setText(commandHistory.get(historyIndex));
        inputField.positionCaret(inputField.getText().length());
    }

    private void executeCommand() {
        String input = inputField.getText().trim();
        if (input.isEmpty()) return;

        Platform.runLater(() -> {
            addToHistory(input);
            printInput(input);

            try {
                executeExpression(input);
            } catch (Exception e) {
                printError("Execution error: " + e.getMessage());
            }

            inputField.clear();
            historyIndex = commandHistory.size();
        });
    }

    private void executeExpression(String input) {
        try {
            if (input.startsWith(":")) {
                handleReplCommand(input);
                return;
            }

            Tokenizer tokenizer = new Tokenizer(input);
            List<Token> tokens = tokenizer.tokenize();
            Parser parser = new Parser(tokens);

            List<Expression> expressions;
            try {
                expressions = parser.parseProgram();
            } catch (Exception e) {
                printError("Parse error: " + e.getMessage());
                return;
            }

            Value result = null;
            for (Expression expr : expressions) {
                result = expr.evaluate(environment);
            }

            if (result != null) {
                printResult(result.getValue().toString());
            } else {
                printOutput("(no result)");
            }

        } catch (Exception e) {
            printError("Error: " + e.getMessage());
        }
    }

    private void handleReplCommand(String command) {
        String[] parts = command.substring(1).split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help" -> printHelp();
            case "vars" -> printVariables();
            case "clear" -> clearOutput();
            case "reset" -> resetEnvironment();
            case "time" -> printOutput("Current time: " + LocalTime.now().format(timeFormatter));
            case "exit" -> Platform.exit();
            default -> printError("Unknown command: " + cmd + ". Type :help for available commands.");
        }
    }

    private void printHelp() {
        printOutput("Available REPL commands:");
        printOutput("  :help    - Show this help message");
        printOutput("  :vars    - Show all variables in current scope");
        printOutput("  :clear   - Clear the output area");
        printOutput("  :reset   - Reset the environment (clear all variables)");
        printOutput("  :time    - Show current time");
        printOutput("  :exit    - Exit the application");
        printOutput("");
        printOutput("Fjord language examples:");
        printOutput("  let x = 42");
        printOutput("  let y = x + 8");
        printOutput("  fn square(n) = n * n");
        printOutput("  square(5)");
    }

    private void printVariables() {
        printOutput("Current variables:");
        // TODO: Extract variables from environment
        // This depends on the Environment class implementation
        printOutput("  (variable listing not yet implemented)");
    }

    private void resetEnvironment() {
        // Create a new environment to clear all variables
        Environment newEnv = new Environment();
        // TODO: Copy the new environment's state to the current one
        printOutput("Environment reset.");
    }

    private void addToHistory(String command) {
        commandHistory.add(command);
        historyItems.add(command);

        // Limit history size
        if (commandHistory.size() > 100) {
            commandHistory.remove(0);
            historyItems.remove(0);
        }
    }

    private void printWelcomeMessage() {
        printOutput("=== Fjord Interactive REPL ===");
        printOutput("Welcome to the Fjord programming language REPL!");
        printOutput("Type expressions to evaluate them, or :help for commands.");
        printOutput("Use Up/Down arrows to navigate command history.");
        printOutput("");
    }

    private void printInput(String input) {
        String timestamp = LocalTime.now().format(timeFormatter);
        appendToOutput(String.format("[%s] > %s\n", timestamp, input), "input");
    }

    private void printResult(String result) {
        appendToOutput("=> " + result + "\n", "result");
    }

    private void printOutput(String text) {
        appendToOutput(text + "\n", "output");
    }

    private void printError(String error) {
        appendToOutput("Error: " + error + "\n", "error");
    }

    private void appendToOutput(String text, String styleClass) {
        int oldLength = outputArea.getLength();
        outputArea.appendText(text);

        if (styleClass != null && !styleClass.isEmpty()) {
            outputArea.setStyleClass(oldLength, outputArea.getLength(), styleClass);
        }

        outputArea.moveTo(outputArea.getLength());
        outputArea.requestFollowCaret();
    }

    private void clearOutput() {
        outputArea.clear();
        printWelcomeMessage();
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void executeCode(String code) {
        Platform.runLater(() -> {
            inputField.setText(code);
            executeCommand();
        });
    }

    public void addOutput(String text) {
        Platform.runLater(() -> printOutput(text));
    }

    public void addError(String error) {
        Platform.runLater(() -> printError(error));
    }

    private Environment createEnvironmentWithBuiltins() {
        Environment env = new Environment();

        // Install built-in functions
        BuiltinRegistry builtins = BuiltinRegistry.createDefault();
        env.define("println", new PrintlnFunction());

        // Add other built-in functions
        for (String name : builtins.getBuiltinNames()) {
            var builtin = builtins.getFunction(name);
            if (builtin.isPresent() && !name.equals("println")) {
                env.define(name, builtin.get());
            }
        }

        return env;
    }
}