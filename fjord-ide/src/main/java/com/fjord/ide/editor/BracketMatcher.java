package com.fjord.ide.editor;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;

public class BracketMatcher {
    private final CodeArea codeArea;
    private final Map<Character, Character> openToClose = Map.of(
        '(', ')',
        '[', ']',
        '{', '}'
    );
    private final Map<Character, Character> closeToOpen = Map.of(
        ')', '(',
        ']', '[',
        '}', '{'
    );

    public BracketMatcher(CodeArea codeArea) {
        this.codeArea = codeArea;
        setupBracketMatching();
    }

    private void setupBracketMatching() {
        // Update bracket highlighting when caret moves
        codeArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            highlightMatchingBrackets();
        });
    }

    public void highlightMatchingBrackets() {
        int caretPos = codeArea.getCaretPosition();
        String text = codeArea.getText();

        if (caretPos >= text.length()) {
            clearBracketHighlighting();
            return;
        }

        // Check character at caret and character before caret
        BracketPair match = null;

        // Check character at caret position
        if (caretPos < text.length()) {
            char currentChar = text.charAt(caretPos);
            match = findMatchingBracket(text, caretPos, currentChar);
        }

        // If no match found, check character before caret
        if (match == null && caretPos > 0) {
            char prevChar = text.charAt(caretPos - 1);
            match = findMatchingBracket(text, caretPos - 1, prevChar);
        }

        if (match != null) {
            highlightBracketPair(match);
        } else {
            clearBracketHighlighting();
        }
    }

    private BracketPair findMatchingBracket(String text, int position, char bracket) {
        if (openToClose.containsKey(bracket)) {
            // Find closing bracket
            int matchPos = findClosingBracket(text, position, bracket, openToClose.get(bracket));
            if (matchPos != -1) {
                return new BracketPair(position, matchPos, BracketPair.MatchType.MATCHED);
            } else {
                return new BracketPair(position, -1, BracketPair.MatchType.UNMATCHED);
            }
        } else if (closeToOpen.containsKey(bracket)) {
            // Find opening bracket
            int matchPos = findOpeningBracket(text, position, bracket, closeToOpen.get(bracket));
            if (matchPos != -1) {
                return new BracketPair(matchPos, position, BracketPair.MatchType.MATCHED);
            } else {
                return new BracketPair(position, -1, BracketPair.MatchType.UNMATCHED);
            }
        }

        return null;
    }

    private int findClosingBracket(String text, int start, char openChar, char closeChar) {
        int count = 1;
        boolean inString = false;
        boolean inComment = false;

        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);

            // Handle string literals
            if (c == '"' && !inComment && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }

            // Handle comments
            if (!inString && i < text.length() - 1 && text.substring(i, i + 2).equals("//")) {
                inComment = true;
                continue;
            }

            if (inComment && c == '\n') {
                inComment = false;
                continue;
            }

            if (!inString && !inComment) {
                if (c == openChar) {
                    count++;
                } else if (c == closeChar) {
                    count--;
                    if (count == 0) {
                        return i;
                    }
                }
            }
        }

        return -1; // No matching bracket found
    }

    private int findOpeningBracket(String text, int start, char closeChar, char openChar) {
        int count = 1;
        boolean inString = false;

        for (int i = start - 1; i >= 0; i--) {
            char c = text.charAt(i);

            // Handle string literals (simplified)
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == closeChar) {
                    count++;
                } else if (c == openChar) {
                    count--;
                    if (count == 0) {
                        return i;
                    }
                }
            }
        }

        return -1; // No matching bracket found
    }

    private void highlightBracketPair(BracketPair pair) {
        String text = codeArea.getText();
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        int lastEnd = 0;
        List<Integer> positions = new ArrayList<>();

        if (pair.matchType == BracketPair.MatchType.MATCHED) {
            positions.add(pair.start);
            if (pair.end != -1) {
                positions.add(pair.end);
            }
            positions.sort(Integer::compareTo);

            for (int pos : positions) {
                // Add text before bracket
                if (pos > lastEnd) {
                    spansBuilder.add(Collections.emptyList(), pos - lastEnd);
                }

                // Add bracket with highlighting
                spansBuilder.add(Collections.singleton("bracket-match"), 1);
                lastEnd = pos + 1;
            }
        } else {
            // Unmatched bracket
            if (pair.start > lastEnd) {
                spansBuilder.add(Collections.emptyList(), pair.start - lastEnd);
            }
            spansBuilder.add(Collections.singleton("bracket-mismatch"), 1);
            lastEnd = pair.start + 1;
        }

        // Add remaining text
        if (lastEnd < text.length()) {
            spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        }

        StyleSpans<Collection<String>> highlighting = spansBuilder.create();
        codeArea.setStyleSpans(0, highlighting);
    }

    private void clearBracketHighlighting() {
        String text = codeArea.getText();
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        spansBuilder.add(Collections.emptyList(), text.length());
        codeArea.setStyleSpans(0, spansBuilder.create());
    }

    public boolean isBalanced() {
        String text = codeArea.getText();
        Stack<Character> stack = new Stack<>();
        boolean inString = false;
        boolean inComment = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Handle string literals
            if (c == '"' && !inComment && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }

            // Handle comments
            if (!inString && i < text.length() - 1 && text.substring(i, i + 2).equals("//")) {
                inComment = true;
                continue;
            }

            if (inComment && c == '\n') {
                inComment = false;
                continue;
            }

            if (!inString && !inComment) {
                if (openToClose.containsKey(c)) {
                    stack.push(c);
                } else if (closeToOpen.containsKey(c)) {
                    if (stack.isEmpty() || !openToClose.get(stack.pop()).equals(c)) {
                        return false;
                    }
                }
            }
        }

        return stack.isEmpty();
    }

    public List<BracketPair> findUnmatchedBrackets() {
        String text = codeArea.getText();
        List<BracketPair> unmatched = new ArrayList<>();
        Stack<Integer> openPositions = new Stack<>();
        boolean inString = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (openToClose.containsKey(c)) {
                    openPositions.push(i);
                } else if (closeToOpen.containsKey(c)) {
                    if (openPositions.isEmpty()) {
                        unmatched.add(new BracketPair(i, -1, BracketPair.MatchType.UNMATCHED));
                    } else {
                        openPositions.pop();
                    }
                }
            }
        }

        // Remaining open brackets are unmatched
        while (!openPositions.isEmpty()) {
            unmatched.add(new BracketPair(openPositions.pop(), -1, BracketPair.MatchType.UNMATCHED));
        }

        return unmatched;
    }

    public void jumpToMatchingBracket() {
        int caretPos = codeArea.getCaretPosition();
        String text = codeArea.getText();

        if (caretPos >= text.length()) return;

        char currentChar = text.charAt(caretPos);
        BracketPair match = findMatchingBracket(text, caretPos, currentChar);

        if (match != null && match.matchType == BracketPair.MatchType.MATCHED) {
            if (match.start == caretPos && match.end != -1) {
                codeArea.moveTo(match.end);
            } else if (match.end == caretPos) {
                codeArea.moveTo(match.start);
            }
        }
    }

    private static class BracketPair {
        enum MatchType {
            MATCHED,
            UNMATCHED
        }

        final int start;
        final int end;
        final MatchType matchType;

        BracketPair(int start, int end, MatchType matchType) {
            this.start = start;
            this.end = end;
            this.matchType = matchType;
        }
    }
}