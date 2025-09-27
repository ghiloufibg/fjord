package com.fjord.ide.ui;

import com.fjord.ide.analysis.Symbol;
import com.fjord.ide.analysis.SimpleSymbolAnalyzer;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class SymbolOutlinePanel {
    private final VBox root;
    private final TreeView<Symbol> symbolTree;
    private final TextField filterField;
    private final SimpleSymbolAnalyzer symbolAnalyzer;
    private File currentFile;
    private BiConsumer<File, Integer> navigationCallback;

    public SymbolOutlinePanel() {
        this.symbolAnalyzer = new SimpleSymbolAnalyzer();
        this.root = new VBox();
        this.symbolTree = new TreeView<>();
        this.filterField = new TextField();

        setupUI();
        setupEventHandlers();
    }

    private void setupUI() {
        Label titleLabel = new Label("Outline");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");

        filterField.setPromptText("Filter symbols...");
        filterField.setPrefHeight(25);

        symbolTree.setShowRoot(false);
        symbolTree.setCellFactory(tv -> new SymbolTreeCell());

        root.getChildren().addAll(titleLabel, filterField, symbolTree);
        VBox.setVgrow(symbolTree, javafx.scene.layout.Priority.ALWAYS);
    }

    private void setupEventHandlers() {
        // Filter symbols as user types
        filterField.textProperty().addListener((obs, oldText, newText) -> {
            updateSymbolTree();
        });

        // Navigate to symbol on double-click
        symbolTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<Symbol> selectedItem = symbolTree.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.getValue() != null) {
                    navigateToSymbol(selectedItem.getValue());
                }
            }
        });

        // Context menu for symbols
        ContextMenu contextMenu = new ContextMenu();
        MenuItem goToDefinition = new MenuItem("Go to Definition");
        MenuItem findReferences = new MenuItem("Find References");
        MenuItem copyName = new MenuItem("Copy Name");

        goToDefinition.setOnAction(e -> {
            TreeItem<Symbol> selected = symbolTree.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue() != null) {
                navigateToSymbol(selected.getValue());
            }
        });

        findReferences.setOnAction(e -> {
            TreeItem<Symbol> selected = symbolTree.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue() != null) {
                // TODO: Implement find references
                System.out.println("Finding references for: " + selected.getValue().getName());
            }
        });

        copyName.setOnAction(e -> {
            TreeItem<Symbol> selected = symbolTree.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue() != null) {
                // Copy to clipboard
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(
                    Map.of(javafx.scene.input.DataFormat.PLAIN_TEXT, selected.getValue().getName())
                );
            }
        });

        contextMenu.getItems().addAll(goToDefinition, findReferences, new SeparatorMenuItem(), copyName);
        symbolTree.setContextMenu(contextMenu);
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        updateSymbolTree();
    }

    public void setNavigationCallback(BiConsumer<File, Integer> callback) {
        this.navigationCallback = callback;
    }

    private void updateSymbolTree() {
        if (currentFile == null) {
            symbolTree.setRoot(null);
            return;
        }

        List<Symbol> symbols = symbolAnalyzer.getFileSymbols(currentFile);
        String filter = filterField.getText().toLowerCase();

        // Filter symbols if needed
        if (!filter.isEmpty()) {
            symbols = symbols.stream()
                    .filter(s -> s.getName().toLowerCase().contains(filter))
                    .toList();
        }

        TreeItem<Symbol> root = new TreeItem<>();

        // Group symbols by type
        TreeItem<Symbol> functions = new TreeItem<>(new GroupSymbol("Functions", Symbol.SymbolType.FUNCTION));
        TreeItem<Symbol> variables = new TreeItem<>(new GroupSymbol("Variables", Symbol.SymbolType.VARIABLE));
        TreeItem<Symbol> structs = new TreeItem<>(new GroupSymbol("Structs", Symbol.SymbolType.STRUCT));

        functions.setExpanded(true);
        variables.setExpanded(true);
        structs.setExpanded(true);

        for (Symbol symbol : symbols) {
            TreeItem<Symbol> item = new TreeItem<>(symbol);

            switch (symbol.getType()) {
                case FUNCTION -> functions.getChildren().add(item);
                case VARIABLE, LOCAL_VARIABLE -> variables.getChildren().add(item);
                case STRUCT -> {
                    structs.getChildren().add(item);
                    // Add fields as children
                    List<Symbol> fields = symbols.stream()
                            .filter(s -> s.getType() == Symbol.SymbolType.FIELD)
                            .toList();
                    for (Symbol field : fields) {
                        item.getChildren().add(new TreeItem<>(field));
                    }
                    item.setExpanded(true);
                }
            }
        }

        // Only add non-empty groups
        if (!functions.getChildren().isEmpty()) root.getChildren().add(functions);
        if (!variables.getChildren().isEmpty()) root.getChildren().add(variables);
        if (!structs.getChildren().isEmpty()) root.getChildren().add(structs);

        Platform.runLater(() -> symbolTree.setRoot(root));
    }

    private void navigateToSymbol(Symbol symbol) {
        if (navigationCallback != null && symbol != null) {
            navigationCallback.accept(symbol.getFile(), symbol.getDefinition().line);
        }
    }

    public VBox getRoot() {
        return root;
    }

    // Custom tree cell for symbol display
    private static class SymbolTreeCell extends TreeCell<Symbol> {
        @Override
        protected void updateItem(Symbol symbol, boolean empty) {
            super.updateItem(symbol, empty);

            if (empty || symbol == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                if (symbol instanceof GroupSymbol) {
                    setText(symbol.getName());
                    setStyle("-fx-font-weight: bold;");
                } else {
                    setText(symbol.getIcon() + " " + symbol.getDisplayName());
                    setStyle("");

                    // Add tooltip with additional info
                    if (symbol.getSignature() != null || symbol.getDocumentation() != null) {
                        StringBuilder tooltip = new StringBuilder();
                        if (symbol.getSignature() != null) {
                            tooltip.append(symbol.getName()).append(symbol.getSignature());
                        }
                        if (symbol.getDocumentation() != null) {
                            if (tooltip.length() > 0) tooltip.append("\n\n");
                            tooltip.append(symbol.getDocumentation());
                        }
                        setTooltip(new Tooltip(tooltip.toString()));
                    }
                }
            }
        }
    }

    // Helper class for grouping symbols
    private static class GroupSymbol extends Symbol {
        public GroupSymbol(String name, SymbolType type) {
            super(name, type, new com.fjord.error.SourcePosition(0, 0, 0), null);
        }
    }
}