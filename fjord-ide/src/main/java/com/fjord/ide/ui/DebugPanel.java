package com.fjord.ide.ui;

import com.fjord.ide.debug.*;
import com.fjord.runtime.Value;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class DebugPanel implements DebugSession.DebugEventListener {
    private final VBox root;
    private final DebugSession debugSession;

    // Control buttons
    private final Button startButton;
    private final Button pauseButton;
    private final Button stopButton;
    private final Button stepIntoButton;
    private final Button stepOverButton;
    private final Button stepOutButton;

    // Status
    private final Label statusLabel;

    // Call stack
    private final ListView<StackFrame> callStackList;
    private final ObservableList<StackFrame> callStackItems = FXCollections.observableArrayList();

    // Variables
    private final TableView<VariableItem> variablesTable;
    private final ObservableList<VariableItem> variableItems = FXCollections.observableArrayList();

    // Breakpoints
    private final ListView<Breakpoint> breakpointsList;
    private final ObservableList<Breakpoint> breakpointItems = FXCollections.observableArrayList();

    // Expression evaluation
    private final TextField expressionField;
    private final TextArea expressionResult;

    // Navigation callback
    private BiConsumer<File, Integer> navigationCallback;

    public DebugPanel() {
        this.debugSession = new DebugSession();
        this.root = new VBox(5);

        // Initialize controls
        this.startButton = new Button("▶ Start");
        this.pauseButton = new Button("⏸ Pause");
        this.stopButton = new Button("⏹ Stop");
        this.stepIntoButton = new Button("⬇ Step Into");
        this.stepOverButton = new Button("➡ Step Over");
        this.stepOutButton = new Button("⬆ Step Out");

        this.statusLabel = new Label("Ready");

        this.callStackList = new ListView<>(callStackItems);
        this.variablesTable = new TableView<>(variableItems);
        this.breakpointsList = new ListView<>(breakpointItems);

        this.expressionField = new TextField();
        this.expressionResult = new TextArea();

        setupUI();
        setupEventHandlers();
        updateControls();

        debugSession.addListener(this);
    }

    private void setupUI() {
        Label titleLabel = new Label("Debug");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");

        // Control buttons
        HBox controlsBox = new HBox(5);
        controlsBox.getChildren().addAll(
            startButton, pauseButton, stopButton,
            new Separator(),
            stepIntoButton, stepOverButton, stepOutButton
        );

        // Status
        statusLabel.setStyle("-fx-padding: 5;");

        // Create tabbed interface for debug info
        TabPane debugTabs = new TabPane();

        // Call stack tab
        Tab callStackTab = new Tab("Call Stack");
        callStackTab.setClosable(false);
        callStackList.setCellFactory(lv -> new CallStackCell());
        callStackTab.setContent(callStackList);

        // Variables tab
        Tab variablesTab = new Tab("Variables");
        variablesTab.setClosable(false);
        setupVariablesTable();
        variablesTab.setContent(variablesTable);

        // Breakpoints tab
        Tab breakpointsTab = new Tab("Breakpoints");
        breakpointsTab.setClosable(false);
        setupBreakpointsList();
        breakpointsTab.setContent(breakpointsList);

        // Expression evaluation tab
        Tab expressionTab = new Tab("Expressions");
        expressionTab.setClosable(false);
        VBox expressionBox = setupExpressionEvaluation();
        expressionTab.setContent(expressionBox);

        debugTabs.getTabs().addAll(callStackTab, variablesTab, breakpointsTab, expressionTab);

        root.getChildren().addAll(titleLabel, controlsBox, statusLabel, debugTabs);
        VBox.setVgrow(debugTabs, Priority.ALWAYS);
    }

    private void setupVariablesTable() {
        TableColumn<VariableItem, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(120);

        TableColumn<VariableItem, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueColumn.setPrefWidth(150);

        TableColumn<VariableItem, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setPrefWidth(80);

        variablesTable.getColumns().addAll(nameColumn, valueColumn, typeColumn);
        variablesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupBreakpointsList() {
        breakpointsList.setCellFactory(lv -> new BreakpointCell());

        // Context menu for breakpoints
        ContextMenu contextMenu = new ContextMenu();
        MenuItem enableDisable = new MenuItem();
        MenuItem delete = new MenuItem("Delete");
        MenuItem deleteAll = new MenuItem("Delete All");

        enableDisable.setOnAction(e -> {
            Breakpoint selected = breakpointsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setEnabled(!selected.isEnabled());
                breakpointsList.refresh();
            }
        });

        delete.setOnAction(e -> {
            Breakpoint selected = breakpointsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                debugSession.removeBreakpoint(selected);
                updateBreakpoints();
            }
        });

        deleteAll.setOnAction(e -> {
            debugSession.getBreakpoints().forEach(debugSession::removeBreakpoint);
            updateBreakpoints();
        });

        contextMenu.getItems().addAll(enableDisable, delete, new SeparatorMenuItem(), deleteAll);
        breakpointsList.setContextMenu(contextMenu);

        // Update context menu text based on selection
        contextMenu.setOnShowing(e -> {
            Breakpoint selected = breakpointsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                enableDisable.setText(selected.isEnabled() ? "Disable" : "Enable");
            }
        });
    }

    private VBox setupExpressionEvaluation() {
        VBox expressionBox = new VBox(5);

        Label expressionLabel = new Label("Evaluate Expression:");
        expressionField.setPromptText("Enter expression to evaluate...");

        Button evaluateButton = new Button("Evaluate");
        evaluateButton.setOnAction(e -> evaluateExpression());

        expressionField.setOnAction(e -> evaluateExpression());

        expressionResult.setEditable(false);
        expressionResult.setPrefRowCount(5);
        expressionResult.setPromptText("Evaluation results will appear here...");

        HBox inputBox = new HBox(5);
        inputBox.getChildren().addAll(expressionField, evaluateButton);
        HBox.setHgrow(expressionField, Priority.ALWAYS);

        expressionBox.getChildren().addAll(expressionLabel, inputBox, expressionResult);
        VBox.setVgrow(expressionResult, Priority.ALWAYS);

        return expressionBox;
    }

    private void setupEventHandlers() {
        startButton.setOnAction(e -> debugSession.start());
        pauseButton.setOnAction(e -> debugSession.pause());
        stopButton.setOnAction(e -> debugSession.stop());
        stepIntoButton.setOnAction(e -> debugSession.stepInto());
        stepOverButton.setOnAction(e -> debugSession.stepOver());
        stepOutButton.setOnAction(e -> debugSession.stepOut());

        // Navigate to source on call stack selection
        callStackList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                StackFrame selected = callStackList.getSelectionModel().getSelectedItem();
                if (selected != null && navigationCallback != null) {
                    navigationCallback.accept(selected.getFile(), selected.getPosition().line);
                }
            }
        });

        // Navigate to breakpoint on selection
        breakpointsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Breakpoint selected = breakpointsList.getSelectionModel().getSelectedItem();
                if (selected != null && navigationCallback != null) {
                    navigationCallback.accept(selected.getFile(), selected.getLine());
                }
            }
        });
    }

    private void evaluateExpression() {
        String expression = expressionField.getText().trim();
        if (expression.isEmpty()) return;

        var result = debugSession.evaluateExpression(expression);
        if (result.isPresent()) {
            expressionResult.setText(expression + " = " + result.get().getValue());
        } else {
            expressionResult.setText("Error: Could not evaluate expression '" + expression + "'");
        }
    }

    private void updateControls() {
        Platform.runLater(() -> {
            boolean isRunning = debugSession.isRunning();
            boolean isPaused = debugSession.isPaused();
            boolean isStopped = debugSession.isStopped();

            startButton.setDisable(!isStopped);
            pauseButton.setDisable(!isRunning);
            stopButton.setDisable(isStopped);
            stepIntoButton.setDisable(!isPaused);
            stepOverButton.setDisable(!isPaused);
            stepOutButton.setDisable(!isPaused);

            statusLabel.setText("Status: " + debugSession.getState().toString());
        });
    }

    private void updateCallStack() {
        Platform.runLater(() -> {
            callStackItems.clear();
            callStackItems.addAll(debugSession.getCallStack());
        });
    }

    private void updateVariables() {
        Platform.runLater(() -> {
            variableItems.clear();
            Map<String, Value> variables = debugSession.getVariables();
            for (Map.Entry<String, Value> entry : variables.entrySet()) {
                variableItems.add(new VariableItem(
                    entry.getKey(),
                    entry.getValue().getValue().toString(),
                    entry.getValue().getClass().getSimpleName()
                ));
            }
        });
    }

    private void updateBreakpoints() {
        Platform.runLater(() -> {
            breakpointItems.clear();
            breakpointItems.addAll(debugSession.getBreakpoints());
        });
    }

    // Debug session event handlers
    @Override
    public void onStateChanged(DebugSession.DebugState oldState, DebugSession.DebugState newState) {
        updateControls();
    }

    @Override
    public void onBreakpointHit(Breakpoint breakpoint, StackFrame frame) {
        Platform.runLater(() -> {
            if (navigationCallback != null) {
                navigationCallback.accept(breakpoint.getFile(), breakpoint.getLine());
            }
        });
    }

    @Override
    public void onStepComplete(StackFrame frame) {
        Platform.runLater(() -> {
            if (navigationCallback != null && frame != null) {
                navigationCallback.accept(frame.getFile(), frame.getPosition().line);
            }
        });
    }

    @Override
    public void onVariablesUpdated(Map<String, Value> variables) {
        updateVariables();
    }

    @Override
    public void onCallStackUpdated(List<StackFrame> callStack) {
        updateCallStack();
    }

    // Public interface
    public void setNavigationCallback(BiConsumer<File, Integer> callback) {
        this.navigationCallback = callback;
    }

    public DebugSession getDebugSession() {
        return debugSession;
    }

    public VBox getRoot() {
        return root;
    }

    // Helper classes
    public static class VariableItem {
        private final String name;
        private final String value;
        private final String type;

        public VariableItem(String name, String value, String type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public String getName() { return name; }
        public String getValue() { return value; }
        public String getType() { return type; }
    }

    private static class CallStackCell extends ListCell<StackFrame> {
        @Override
        protected void updateItem(StackFrame frame, boolean empty) {
            super.updateItem(frame, empty);
            if (empty || frame == null) {
                setText(null);
            } else {
                setText(frame.getFullDisplayName());
            }
        }
    }

    private static class BreakpointCell extends ListCell<Breakpoint> {
        @Override
        protected void updateItem(Breakpoint breakpoint, boolean empty) {
            super.updateItem(breakpoint, empty);
            if (empty || breakpoint == null) {
                setText(null);
                setStyle("");
            } else {
                setText(breakpoint.getDisplayName());
                if (!breakpoint.isEnabled()) {
                    setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                } else {
                    setStyle("");
                }
            }
        }
    }
}