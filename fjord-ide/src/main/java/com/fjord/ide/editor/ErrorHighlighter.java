package com.fjord.ide.editor;

import com.fjord.compiler.CompilationResult;
import com.fjord.compiler.FjordCompiler;
import com.fjord.error.FjordError;
import com.fjord.error.SourcePosition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ErrorHighlighter {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ErrorHighlighter");
        t.setDaemon(true);
        return t;
    });

    private final FjordCompiler compiler = FjordCompiler.quiet();
    private Task<CompilationResult> currentTask;

    public void highlightErrors(CodeArea codeArea, String content) {
        // Cancel previous task if running
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }

        currentTask = new Task<CompilationResult>() {
            @Override
            protected CompilationResult call() {
                return compiler.compile(content);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    CompilationResult result = getValue();
                    if (result.isFailure()) {
                        applyErrorHighlighting(codeArea, result.getErrorReporter().getErrors(), content);
                    } else {
                        clearErrorHighlighting(codeArea, content);
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> clearErrorHighlighting(codeArea, content));
            }
        };

        executor.submit(currentTask);
    }

    private void applyErrorHighlighting(CodeArea codeArea, List<FjordError> errors, String content) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        // Sort errors by position
        errors.sort((a, b) -> Integer.compare(a.position.offset, b.position.offset));

        for (FjordError error : errors) {
            SourcePosition pos = error.position;

            // Add normal text before error
            if (pos.offset > lastEnd) {
                spansBuilder.add(Collections.emptyList(), pos.offset - lastEnd);
            }

            // Determine error length (default to 1 if we can't determine)
            int errorLength = 1;
            if (pos.offset < content.length()) {
                // Try to highlight the whole word/token
                int start = pos.offset;
                int end = start;
                while (end < content.length() &&
                       (Character.isLetterOrDigit(content.charAt(end)) ||
                        content.charAt(end) == '_')) {
                    end++;
                }
                errorLength = Math.max(1, end - start);
            }

            // Add error highlighting
            spansBuilder.add(Collections.singleton("error"), errorLength);
            lastEnd = pos.offset + errorLength;
        }

        // Add remaining text
        if (lastEnd < content.length()) {
            spansBuilder.add(Collections.emptyList(), content.length() - lastEnd);
        }

        StyleSpans<Collection<String>> highlighting = spansBuilder.create();
        codeArea.setStyleSpans(0, highlighting);
    }

    private void clearErrorHighlighting(CodeArea codeArea, String content) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        spansBuilder.add(Collections.emptyList(), content.length());
        codeArea.setStyleSpans(0, spansBuilder.create());
    }

    public void shutdown() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        executor.shutdown();
    }
}