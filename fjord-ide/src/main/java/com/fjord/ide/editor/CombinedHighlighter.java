package com.fjord.ide.editor;

import com.fjord.compiler.CompilationResult;
import com.fjord.compiler.FjordCompiler;
import com.fjord.error.FjordError;
import com.fjord.error.SourcePosition;
import com.fjord.lex.Token;
import com.fjord.lex.TokenType;
import com.fjord.lex.Tokenizer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CombinedHighlighter {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CombinedHighlighter");
        t.setDaemon(true);
        return t;
    });

    private final FjordCompiler compiler = FjordCompiler.quiet();
    private Task<HighlightResult> currentTask;

    public void highlight(CodeArea codeArea) {
        String text = codeArea.getText();

        // Cancel previous task if running
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }

        currentTask = new Task<HighlightResult>() {
            @Override
            protected HighlightResult call() {
                return computeHighlighting(text);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    HighlightResult result = getValue();
                    codeArea.setStyleSpans(0, result.styleSpans);
                });
            }
        };

        executor.submit(currentTask);
    }

    private HighlightResult computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        try {
            // Get syntax highlighting
            Map<Integer, Set<String>> syntaxStyles = computeSyntaxHighlighting(text);

            // Get error highlighting
            Map<Integer, Set<String>> errorStyles = computeErrorHighlighting(text);

            // Combine both
            Map<Integer, Set<String>> combinedStyles = new TreeMap<>();

            // Add syntax styles
            syntaxStyles.forEach((pos, styles) ->
                combinedStyles.computeIfAbsent(pos, k -> new HashSet<>()).addAll(styles));

            // Add error styles (they override syntax)
            errorStyles.forEach((pos, styles) ->
                combinedStyles.computeIfAbsent(pos, k -> new HashSet<>()).addAll(styles));

            // Build spans
            int lastEnd = 0;
            for (Map.Entry<Integer, Set<String>> entry : combinedStyles.entrySet()) {
                int pos = entry.getKey();
                Set<String> styles = entry.getValue();

                // Add gap if needed
                if (pos > lastEnd) {
                    spansBuilder.add(Collections.emptyList(), pos - lastEnd);
                }

                // Add styled span (assume 1 character for now)
                spansBuilder.add(styles, 1);
                lastEnd = pos + 1;
            }

            // Add remaining text
            if (lastEnd < text.length()) {
                spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
            }

        } catch (Exception e) {
            // If highlighting fails, return no highlighting
            spansBuilder.add(Collections.emptyList(), text.length());
        }

        return new HighlightResult(spansBuilder.create());
    }

    private Map<Integer, Set<String>> computeSyntaxHighlighting(String text) {
        Map<Integer, Set<String>> styles = new TreeMap<>();

        try {
            Tokenizer tokenizer = new Tokenizer(text);
            List<Token> tokens = tokenizer.tokenize();

            for (Token token : tokens) {
                String styleClass = getStyleClass(token.getType());
                if (styleClass != null) {
                    int start = token.getPosition().offset;
                    for (int i = 0; i < token.getLexeme().length(); i++) {
                        styles.computeIfAbsent(start + i, k -> new HashSet<>()).add(styleClass);
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore tokenization errors
        }

        return styles;
    }

    private Map<Integer, Set<String>> computeErrorHighlighting(String text) {
        Map<Integer, Set<String>> styles = new TreeMap<>();

        try {
            CompilationResult result = compiler.compile(text);
            if (result.isFailure()) {
                List<FjordError> errors = result.getErrorReporter().getErrors();
                for (FjordError error : errors) {
                    SourcePosition pos = error.position;
                    if (pos.offset < text.length()) {
                        // Highlight the character at error position
                        styles.computeIfAbsent(pos.offset, k -> new HashSet<>()).add("error");

                        // Try to highlight the whole token/word
                        int start = pos.offset;
                        int end = start + 1;
                        while (end < text.length() &&
                               (Character.isLetterOrDigit(text.charAt(end)) ||
                                text.charAt(end) == '_')) {
                            styles.computeIfAbsent(end, k -> new HashSet<>()).add("error");
                            end++;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore compilation errors
        }

        return styles;
    }

    private String getStyleClass(TokenType tokenType) {
        return switch (tokenType) {
            case LET, MUT, FN, IF, THEN, ELSE, WHILE, DO, FOR, IN, STRUCT -> "keyword";
            case BOOL_LITERAL -> "boolean-literal";
            case INTEGER_LITERAL, FLOAT_LITERAL -> "number-literal";
            case STRING_LITERAL -> "string-literal";
            case IDENTIFIER -> "identifier";
            case PLUS, MINUS, STAR, SLASH, PERCENT,
                 EQEQ, BANGEQ, LT, LTE, GT, GTE,
                 EQUAL, ARROW -> "operator";
            case LPAREN, RPAREN, LBRACE, RBRACE,
                 LBRACKET, RBRACKET -> "punctuation";
            case SEMICOLON, COMMA, DOT, COLON -> "delimiter";
            default -> null;
        };
    }

    public void shutdown() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        executor.shutdown();
    }

    private static class HighlightResult {
        final StyleSpans<Collection<String>> styleSpans;

        HighlightResult(StyleSpans<Collection<String>> styleSpans) {
            this.styleSpans = styleSpans;
        }
    }
}