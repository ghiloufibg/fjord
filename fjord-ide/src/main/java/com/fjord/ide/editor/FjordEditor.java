package com.fjord.ide.editor;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.time.Duration;

public class FjordEditor {
    private final CodeArea codeArea;
    private final CombinedHighlighter combinedHighlighter;
    private final AutoCompletion autoCompletion;
    private File file;
    private boolean modified = false;

    public FjordEditor() {
        this.codeArea = new CodeArea();
        this.combinedHighlighter = new CombinedHighlighter();
        this.autoCompletion = new AutoCompletion();

        setupCodeArea();
    }

    private void setupCodeArea() {
        // Add line numbers
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        // Enable syntax highlighting with debouncing
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .successionEnds(Duration.ofMillis(300))
                .subscribe(change -> {
                    combinedHighlighter.highlight(codeArea);
                });

        // Track modifications
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            setModified(true);
        });

        // Attach auto-completion
        autoCompletion.attachTo(codeArea);

        // Set default style
        codeArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 14px;");

        // Enable word wrap (optional)
        codeArea.setWrapText(false);

        // Auto-indent
        codeArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    handleEnterKey();
                }
                case TAB -> {
                    if (!event.isShiftDown()) {
                        codeArea.insertText(codeArea.getCaretPosition(), "    ");
                        event.consume();
                    }
                }
            }
        });
    }

    private void handleEnterKey() {
        int caretPos = codeArea.getCaretPosition();
        int currentParagraph = codeArea.getCurrentParagraph();

        if (currentParagraph > 0) {
            String currentLine = codeArea.getParagraph(currentParagraph).getText();

            // Calculate indentation of current line
            int indent = 0;
            for (char c : currentLine.toCharArray()) {
                if (c == ' ') indent++;
                else if (c == '\t') indent += 4;
                else break;
            }

            // Check if we need extra indentation (after { or fn)
            String trimmedLine = currentLine.trim();
            if (trimmedLine.endsWith("{") || trimmedLine.startsWith("fn ") ||
                trimmedLine.startsWith("if ") || trimmedLine.startsWith("while ") ||
                trimmedLine.startsWith("for ")) {
                indent += 4;
            }

            // Insert newline and indentation
            if (indent > 0) {
                StringBuilder indentStr = new StringBuilder("\n");
                for (int i = 0; i < indent; i++) {
                    indentStr.append(" ");
                }
                codeArea.insertText(caretPos, indentStr.toString());
            }
        }
    }

    public void setContent(String content) {
        codeArea.replaceText(content);
        combinedHighlighter.highlight(codeArea);
        setModified(false);
    }

    public String getContent() {
        return codeArea.getText();
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void dispose() {
        combinedHighlighter.shutdown();
        autoCompletion.shutdown();
    }

    // Convenience methods for common operations
    public void goToLine(int line) {
        if (line > 0 && line <= codeArea.getParagraphs().size()) {
            codeArea.moveTo(line - 1, 0);
            codeArea.requestFollowCaret();
        }
    }

    public void selectLine(int line) {
        if (line > 0 && line <= codeArea.getParagraphs().size()) {
            int start = codeArea.getAbsolutePosition(line - 1, 0);
            int end = line < codeArea.getParagraphs().size() ?
                     codeArea.getAbsolutePosition(line, 0) - 1 :
                     codeArea.getLength();
            codeArea.selectRange(start, end);
        }
    }

    public int getCurrentLine() {
        return codeArea.getCurrentParagraph() + 1;
    }

    public int getCurrentColumn() {
        return codeArea.getCaretColumn() + 1;
    }
}