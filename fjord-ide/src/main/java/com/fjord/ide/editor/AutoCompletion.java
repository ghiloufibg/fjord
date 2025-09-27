package com.fjord.ide.editor;

import com.fjord.analyzer.SymbolTable;
import com.fjord.ast.Expression;
import com.fjord.compiler.CompilationResult;
import com.fjord.compiler.FjordCompiler;
import com.fjord.lex.Token;
import com.fjord.lex.TokenType;
import com.fjord.lex.Tokenizer;
import com.fjord.runtime.builtin.BuiltinRegistry;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.stage.Popup;
import org.fxmisc.richtext.CodeArea;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AutoCompletion {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AutoCompletion");
        t.setDaemon(true);
        return t;
    });

    private final FjordCompiler compiler = FjordCompiler.quiet();
    private final BuiltinRegistry builtins = BuiltinRegistry.createDefault();
    private final Popup completionPopup = new Popup();
    private final ListView<CompletionItem> completionList = new ListView<>();

    private CodeArea codeArea;
    private String currentPrefix = "";
    private int completionStartIndex = 0;

    // Fjord keywords
    private static final Set<String> KEYWORDS = Set.of(
        "let", "mut", "fn", "if", "then", "else", "while", "do", "for", "in", "struct",
        "true", "false"
    );

    public AutoCompletion() {
        setupCompletionPopup();
    }

    private void setupCompletionPopup() {
        completionList.setPrefHeight(200);
        completionList.setPrefWidth(300);
        completionList.getStyleClass().add("completion-list");

        completionList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                applyCompletion();
            }
        });

        completionList.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    applyCompletion();
                    event.consume();
                }
                case ESCAPE -> {
                    hideCompletion();
                    event.consume();
                }
                case UP, DOWN -> {
                    // Let ListView handle these
                }
                default -> {
                    // Pass other keys to the code area
                    codeArea.fireEvent(event);
                    event.consume();
                }
            }
        });

        completionPopup.getContent().add(completionList);
        completionPopup.setAutoHide(true);
    }

    public void attachTo(CodeArea codeArea) {
        this.codeArea = codeArea;

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> updateCompletion());
        });

        codeArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            Platform.runLater(() -> updateCompletion());
        });

        codeArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case SPACE -> {
                    if (event.isControlDown()) {
                        showCompletion();
                        event.consume();
                    }
                }
                case TAB -> {
                    if (completionPopup.isShowing()) {
                        applyCompletion();
                        event.consume();
                    }
                }
                case ESCAPE -> {
                    if (completionPopup.isShowing()) {
                        hideCompletion();
                        event.consume();
                    }
                }
            }
        });
    }

    private void updateCompletion() {
        if (codeArea == null) return;

        int caretPos = codeArea.getCaretPosition();
        String text = codeArea.getText();

        // Find the current word being typed
        int start = caretPos - 1;
        while (start >= 0 && (Character.isLetterOrDigit(text.charAt(start)) || text.charAt(start) == '_')) {
            start--;
        }
        start++;

        if (start < caretPos) {
            currentPrefix = text.substring(start, caretPos);
            completionStartIndex = start;

            if (currentPrefix.length() >= 1) {
                computeCompletions(text, currentPrefix);
            } else {
                hideCompletion();
            }
        } else {
            hideCompletion();
        }
    }

    private void showCompletion() {
        if (codeArea == null) return;

        int caretPos = codeArea.getCaretPosition();
        String text = codeArea.getText();

        // Find current word
        int start = caretPos - 1;
        while (start >= 0 && (Character.isLetterOrDigit(text.charAt(start)) || text.charAt(start) == '_')) {
            start--;
        }
        start++;

        currentPrefix = start < caretPos ? text.substring(start, caretPos) : "";
        completionStartIndex = start;

        computeCompletions(text, currentPrefix);
    }

    private void computeCompletions(String text, String prefix) {
        Task<List<CompletionItem>> task = new Task<List<CompletionItem>>() {
            @Override
            protected List<CompletionItem> call() {
                return getCompletionItems(text, prefix);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<CompletionItem> items = getValue();
                    if (!items.isEmpty()) {
                        showCompletionItems(items);
                    } else {
                        hideCompletion();
                    }
                });
            }
        };

        executor.submit(task);
    }

    private List<CompletionItem> getCompletionItems(String text, String prefix) {
        List<CompletionItem> items = new ArrayList<>();

        // Add keywords
        KEYWORDS.stream()
                .filter(keyword -> keyword.startsWith(prefix.toLowerCase()))
                .forEach(keyword -> items.add(new CompletionItem(keyword, "keyword", keyword)));

        // Add built-in functions
        builtins.getBuiltinNames().stream()
                .filter(name -> name.startsWith(prefix))
                .forEach(name -> {
                    var function = builtins.getFunction(name);
                    if (function.isPresent()) {
                        items.add(new CompletionItem(name, "function", name + "()"));
                    }
                });

        // Add identifiers from current file
        Set<String> identifiers = extractIdentifiers(text);
        identifiers.stream()
                .filter(id -> id.startsWith(prefix) && !id.equals(prefix))
                .forEach(id -> items.add(new CompletionItem(id, "identifier", id)));

        // Sort by relevance (exact prefix match first, then alphabetically)
        items.sort((a, b) -> {
            boolean aStarts = a.text.startsWith(prefix);
            boolean bStarts = b.text.startsWith(prefix);
            if (aStarts && !bStarts) return -1;
            if (!aStarts && bStarts) return 1;
            return a.text.compareToIgnoreCase(b.text);
        });

        return items.stream().distinct().collect(Collectors.toList());
    }

    private Set<String> extractIdentifiers(String text) {
        Set<String> identifiers = new HashSet<>();

        try {
            Tokenizer tokenizer = new Tokenizer(text);
            List<Token> tokens = tokenizer.tokenize();

            for (Token token : tokens) {
                if (token.getType() == TokenType.IDENTIFIER) {
                    identifiers.add(token.getLexeme());
                }
            }
        } catch (Exception ignored) {
            // Ignore tokenization errors
        }

        return identifiers;
    }

    private void showCompletionItems(List<CompletionItem> items) {
        completionList.getItems().clear();
        completionList.getItems().addAll(items);

        if (completionList.getItems().isEmpty()) {
            hideCompletion();
            return;
        }

        completionList.getSelectionModel().selectFirst();

        // Position popup near caret
        Bounds caretBounds = codeArea.getCaretBounds().orElse(null);
        if (caretBounds != null) {
            Bounds sceneBounds = codeArea.localToScene(caretBounds);
            var window = codeArea.getScene().getWindow();

            double x = window.getX() + sceneBounds.getMinX();
            double y = window.getY() + sceneBounds.getMaxY();

            completionPopup.show(window, x, y);
        }
    }

    private void applyCompletion() {
        CompletionItem selected = completionList.getSelectionModel().getSelectedItem();
        if (selected != null && codeArea != null) {
            int caretPos = codeArea.getCaretPosition();
            codeArea.replaceText(completionStartIndex, caretPos, selected.completion);
            codeArea.moveTo(completionStartIndex + selected.completion.length());
        }
        hideCompletion();
    }

    private void hideCompletion() {
        completionPopup.hide();
    }

    public void shutdown() {
        executor.shutdown();
    }

    public static class CompletionItem {
        public final String text;
        public final String type;
        public final String completion;

        public CompletionItem(String text, String type, String completion) {
            this.text = text;
            this.type = type;
            this.completion = completion;
        }

        @Override
        public String toString() {
            return text + " (" + type + ")";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CompletionItem that = (CompletionItem) obj;
            return Objects.equals(text, that.text) && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, type);
        }
    }
}