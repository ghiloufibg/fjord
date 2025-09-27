package com.fjord.ide.editor;

import com.fjord.lex.Token;
import com.fjord.lex.TokenType;
import com.fjord.lex.Tokenizer;
import org.fxmisc.richtext.CodeArea;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeFolding {
    private final CodeArea codeArea;
    private final Map<Integer, FoldRegion> foldRegions = new HashMap<>();
    private final Set<Integer> collapsedRegions = new HashSet<>();

    public CodeFolding(CodeArea codeArea) {
        this.codeArea = codeArea;
        setupFolding();
    }

    private void setupFolding() {
        // Update fold regions when text changes
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            updateFoldRegions();
        });
    }

    public void updateFoldRegions() {
        foldRegions.clear();
        String text = codeArea.getText();

        // Find foldable regions
        findBraceFolds(text);
        findFunctionFolds(text);
        findCommentFolds(text);
        findStructFolds(text);

        // Update visual indicators
        updateFoldIndicators();
    }

    private void findBraceFolds(String text) {
        try {
            Tokenizer tokenizer = new Tokenizer(text);
            List<Token> tokens = tokenizer.tokenize();

            Stack<Integer> braceStack = new Stack<>();
            String[] lines = text.split("\n");

            for (Token token : tokens) {
                if (token.getType() == TokenType.LBRACE) {
                    braceStack.push(token.getPosition().line);
                } else if (token.getType() == TokenType.RBRACE && !braceStack.isEmpty()) {
                    int startLine = braceStack.pop();
                    int endLine = token.getPosition().line;

                    if (endLine > startLine + 1) { // Only fold if more than one line
                        String startContent = getLineContent(lines, startLine);
                        FoldRegion region = new FoldRegion(
                            startLine, endLine,
                            FoldRegion.FoldType.BRACE_BLOCK,
                            startContent + " {...}"
                        );
                        foldRegions.put(startLine, region);
                    }
                }
            }
        } catch (Exception e) {
            // Handle tokenization errors gracefully
        }
    }

    private void findFunctionFolds(String text) {
        Pattern functionPattern = Pattern.compile("^\\s*fn\\s+(\\w+)\\s*\\([^)]*\\)\\s*=?\\s*\\{?", Pattern.MULTILINE);
        Matcher matcher = functionPattern.matcher(text);
        String[] lines = text.split("\n");

        while (matcher.find()) {
            int startLine = getLineNumber(text, matcher.start()) + 1;
            String functionName = matcher.group(1);

            // Find the end of the function by matching braces
            int endLine = findMatchingBrace(text, startLine);
            if (endLine > startLine + 1) {
                FoldRegion region = new FoldRegion(
                    startLine, endLine,
                    FoldRegion.FoldType.FUNCTION,
                    "fn " + functionName + "(...) {...}"
                );
                foldRegions.put(startLine, region);
            }
        }
    }

    private void findStructFolds(String text) {
        Pattern structPattern = Pattern.compile("^\\s*struct\\s+(\\w+)\\s*\\{", Pattern.MULTILINE);
        Matcher matcher = structPattern.matcher(text);

        while (matcher.find()) {
            int startLine = getLineNumber(text, matcher.start()) + 1;
            String structName = matcher.group(1);

            int endLine = findMatchingBrace(text, startLine);
            if (endLine > startLine + 1) {
                FoldRegion region = new FoldRegion(
                    startLine, endLine,
                    FoldRegion.FoldType.STRUCT,
                    "struct " + structName + " {...}"
                );
                foldRegions.put(startLine, region);
            }
        }
    }

    private void findCommentFolds(String text) {
        String[] lines = text.split("\n");
        List<Integer> commentLines = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("//")) {
                commentLines.add(i + 1);
            } else if (!commentLines.isEmpty()) {
                // End of comment block
                if (commentLines.size() > 2) { // Only fold blocks of 3+ comment lines
                    int startLine = commentLines.get(0);
                    int endLine = commentLines.get(commentLines.size() - 1);

                    FoldRegion region = new FoldRegion(
                        startLine, endLine,
                        FoldRegion.FoldType.COMMENT,
                        "// " + commentLines.size() + " lines..."
                    );
                    foldRegions.put(startLine, region);
                }
                commentLines.clear();
            }
        }

        // Handle comment block at end of file
        if (commentLines.size() > 2) {
            int startLine = commentLines.get(0);
            int endLine = commentLines.get(commentLines.size() - 1);

            FoldRegion region = new FoldRegion(
                startLine, endLine,
                FoldRegion.FoldType.COMMENT,
                "// " + commentLines.size() + " lines..."
            );
            foldRegions.put(startLine, region);
        }
    }

    private int findMatchingBrace(String text, int startLine) {
        String[] lines = text.split("\n");
        int braceCount = 0;
        boolean foundOpenBrace = false;

        for (int i = startLine - 1; i < lines.length; i++) {
            String line = lines[i];
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    foundOpenBrace = true;
                } else if (c == '}') {
                    braceCount--;
                    if (foundOpenBrace && braceCount == 0) {
                        return i + 1;
                    }
                }
            }
        }
        return startLine; // No matching brace found
    }

    private int getLineNumber(String text, int position) {
        return (int) text.substring(0, position).chars().filter(ch -> ch == '\n').count();
    }

    private String getLineContent(String[] lines, int lineNumber) {
        if (lineNumber > 0 && lineNumber <= lines.length) {
            return lines[lineNumber - 1].trim();
        }
        return "";
    }

    private void updateFoldIndicators() {
        // TODO: Add visual fold indicators to the editor
        // This would require custom paragraph graphics in RichTextFX
    }

    public void toggleFold(int line) {
        FoldRegion region = foldRegions.get(line);
        if (region != null) {
            if (collapsedRegions.contains(line)) {
                expandRegion(line);
            } else {
                collapseRegion(line);
            }
        }
    }

    public void collapseRegion(int line) {
        FoldRegion region = foldRegions.get(line);
        if (region != null) {
            collapsedRegions.add(line);

            // Hide lines in the fold region
            for (int i = region.startLine + 1; i <= region.endLine; i++) {
                // TODO: Implement line hiding in RichTextFX
                // This requires custom implementation with virtual paragraphs
            }
        }
    }

    public void expandRegion(int line) {
        collapsedRegions.remove(line);
        FoldRegion region = foldRegions.get(line);
        if (region != null) {
            // Show lines in the fold region
            for (int i = region.startLine + 1; i <= region.endLine; i++) {
                // TODO: Implement line showing in RichTextFX
            }
        }
    }

    public void collapseAll() {
        for (int line : foldRegions.keySet()) {
            collapseRegion(line);
        }
    }

    public void expandAll() {
        Set<Integer> toExpand = new HashSet<>(collapsedRegions);
        for (int line : toExpand) {
            expandRegion(line);
        }
    }

    public boolean isFoldable(int line) {
        return foldRegions.containsKey(line);
    }

    public boolean isCollapsed(int line) {
        return collapsedRegions.contains(line);
    }

    public Collection<FoldRegion> getFoldRegions() {
        return foldRegions.values();
    }

    public static class FoldRegion {
        public enum FoldType {
            FUNCTION,
            STRUCT,
            BRACE_BLOCK,
            COMMENT,
            IMPORT
        }

        public final int startLine;
        public final int endLine;
        public final FoldType type;
        public final String placeholder;

        public FoldRegion(int startLine, int endLine, FoldType type, String placeholder) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.type = type;
            this.placeholder = placeholder;
        }

        public int getLineCount() {
            return endLine - startLine + 1;
        }

        @Override
        public String toString() {
            return String.format("%s [%d-%d]: %s", type, startLine, endLine, placeholder);
        }
    }
}