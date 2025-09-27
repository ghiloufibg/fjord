package com.fjord.ide.editor;

import com.fjord.lex.Token;
import com.fjord.lex.TokenType;
import com.fjord.lex.Tokenizer;

import java.util.List;

public class CodeFormatter {
    private static final String INDENT = "    "; // 4 spaces
    private static final int MAX_LINE_LENGTH = 100;

    public static class FormattingOptions {
        public boolean insertSpaceAfterComma = true;
        public boolean insertSpaceAroundOperators = true;
        public boolean insertSpaceAfterKeywords = true;
        public boolean alignParameters = false;
        public boolean wrapLongLines = true;
        public int maxLineLength = MAX_LINE_LENGTH;
        public String indentString = INDENT;
        public boolean insertFinalNewline = true;
        public boolean trimTrailingWhitespace = true;
    }

    public static String formatCode(String code) {
        return formatCode(code, new FormattingOptions());
    }

    public static String formatCode(String code, FormattingOptions options) {
        try {
            Tokenizer tokenizer = new Tokenizer(code);
            List<Token> tokens = tokenizer.tokenize();

            StringBuilder formatted = new StringBuilder();
            int indentLevel = 0;
            boolean needsNewline = false;
            boolean atLineStart = true;
            Token previousToken = null;

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                Token nextToken = i < tokens.size() - 1 ? tokens.get(i + 1) : null;

                // Handle indentation at line start
                if (atLineStart && !isNewlineToken(token)) {
                    if (isClosingBrace(token)) {
                        indentLevel = Math.max(0, indentLevel - 1);
                    }
                    formatted.append(options.indentString.repeat(indentLevel));
                    atLineStart = false;
                }

                // Insert newlines before certain tokens
                if (needsNewline && !isNewlineToken(token)) {
                    formatted.append("\n");
                    atLineStart = true;
                    needsNewline = false;
                    continue; // Reprocess this token
                }

                // Add spaces before token if needed
                if (previousToken != null && shouldInsertSpaceBefore(token, previousToken, options)) {
                    formatted.append(" ");
                }

                // Add the token text
                formatted.append(token.getLexeme());

                // Add spaces after token if needed
                if (shouldInsertSpaceAfter(token, nextToken, options)) {
                    formatted.append(" ");
                }

                // Handle newlines and indentation
                if (isOpeningBrace(token)) {
                    indentLevel++;
                    needsNewline = true;
                } else if (isClosingBrace(token)) {
                    if (nextToken != null && !isEndOfStatement(nextToken)) {
                        needsNewline = true;
                    }
                } else if (isSemicolon(token)) {
                    needsNewline = true;
                }

                // Handle special cases for function definitions
                if (token.getType() == TokenType.EQUAL &&
                    previousToken != null && previousToken.getType() == TokenType.RPAREN) {
                    // Function definition: fn name() = body
                    formatted.append(" ");
                }

                previousToken = token;
            }

            // Post-processing
            String result = formatted.toString();

            if (options.trimTrailingWhitespace) {
                result = trimTrailingWhitespace(result);
            }

            if (options.wrapLongLines) {
                result = wrapLongLines(result, options.maxLineLength, options.indentString);
            }

            if (options.insertFinalNewline && !result.endsWith("\n")) {
                result += "\n";
            }

            return result;

        } catch (Exception e) {
            // If formatting fails, return original code
            System.err.println("Code formatting failed: " + e.getMessage());
            return code;
        }
    }

    private static boolean shouldInsertSpaceBefore(Token token, Token previousToken, FormattingOptions options) {
        if (previousToken == null) return false;

        TokenType current = token.getType();
        TokenType previous = previousToken.getType();

        // No space after opening brackets
        if (isOpeningBracket(previous)) return false;

        // No space before closing brackets
        if (isClosingBracket(current)) return false;

        // No space before punctuation
        if (current == TokenType.COMMA || current == TokenType.SEMICOLON || current == TokenType.DOT) return false;

        // Space around operators
        if (options.insertSpaceAroundOperators && isOperator(current)) return true;

        // Space after keywords
        if (options.insertSpaceAfterKeywords && isKeyword(previous)) return true;

        // Space after commas
        if (options.insertSpaceAfterComma && previous == TokenType.COMMA) return true;

        // Default: space between identifiers and literals
        if ((isIdentifierOrLiteral(previous) && isIdentifierOrLiteral(current)) ||
            (isIdentifierOrLiteral(previous) && isKeyword(current)) ||
            (isKeyword(previous) && isIdentifierOrLiteral(current))) {
            return true;
        }

        return false;
    }

    private static boolean shouldInsertSpaceAfter(Token token, Token nextToken, FormattingOptions options) {
        if (nextToken == null) return false;

        TokenType current = token.getType();
        TokenType next = nextToken.getType();

        // Space around operators
        if (options.insertSpaceAroundOperators && isOperator(current)) return true;

        // Space after commas
        if (options.insertSpaceAfterComma && current == TokenType.COMMA) return true;

        // Space after keywords (except when followed by opening parenthesis)
        if (options.insertSpaceAfterKeywords && isKeyword(current) && next != TokenType.LPAREN) return true;

        // Space after closing parenthesis in function definitions
        if (current == TokenType.RPAREN && next == TokenType.EQUAL) return true;

        return false;
    }

    private static boolean isKeyword(TokenType type) {
        return type == TokenType.LET || type == TokenType.MUT || type == TokenType.FN ||
               type == TokenType.IF || type == TokenType.THEN || type == TokenType.ELSE ||
               type == TokenType.WHILE || type == TokenType.DO || type == TokenType.FOR ||
               type == TokenType.IN || type == TokenType.STRUCT;
    }

    private static boolean isOperator(TokenType type) {
        return type == TokenType.PLUS || type == TokenType.MINUS || type == TokenType.STAR ||
               type == TokenType.SLASH || type == TokenType.PERCENT || type == TokenType.EQUAL ||
               type == TokenType.EQEQ || type == TokenType.BANGEQ || type == TokenType.LT ||
               type == TokenType.LTE || type == TokenType.GT || type == TokenType.GTE ||
               type == TokenType.ARROW;
    }

    private static boolean isIdentifierOrLiteral(TokenType type) {
        return type == TokenType.IDENTIFIER || type == TokenType.INTEGER_LITERAL ||
               type == TokenType.FLOAT_LITERAL || type == TokenType.STRING_LITERAL ||
               type == TokenType.BOOL_LITERAL;
    }

    private static boolean isOpeningBracket(TokenType type) {
        return type == TokenType.LPAREN || type == TokenType.LBRACE || type == TokenType.LBRACKET;
    }

    private static boolean isClosingBracket(TokenType type) {
        return type == TokenType.RPAREN || type == TokenType.RBRACE || type == TokenType.RBRACKET;
    }

    private static boolean isOpeningBrace(Token token) {
        return token.getType() == TokenType.LBRACE;
    }

    private static boolean isClosingBrace(Token token) {
        return token.getType() == TokenType.RBRACE;
    }

    private static boolean isSemicolon(Token token) {
        return token.getType() == TokenType.SEMICOLON;
    }

    private static boolean isNewlineToken(Token token) {
        return token.getLexeme().equals("\n");
    }

    private static boolean isEndOfStatement(Token token) {
        return token.getType() == TokenType.EOF || token.getType() == TokenType.SEMICOLON;
    }

    private static String trimTrailingWhitespace(String code) {
        String[] lines = code.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].replaceAll("\\s+$", "");
            result.append(line);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    private static String wrapLongLines(String code, int maxLength, String indentString) {
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() <= maxLength) {
                result.append(line);
            } else {
                // Simple line wrapping at commas or operators
                String wrapped = wrapLine(line, maxLength, indentString);
                result.append(wrapped);
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    private static String wrapLine(String line, int maxLength, String indentString) {
        if (line.length() <= maxLength) return line;

        // Find good break points (after commas, before operators)
        StringBuilder result = new StringBuilder();
        String[] parts = line.split("(?<=,)|(?=[+\\-*/=<>!])");

        String currentLine = "";
        String baseIndent = line.substring(0, line.length() - line.trim().length());

        for (String part : parts) {
            if ((currentLine + part).length() <= maxLength) {
                currentLine += part;
            } else {
                if (!currentLine.isEmpty()) {
                    result.append(currentLine).append("\n");
                    currentLine = baseIndent + indentString + part.trim();
                } else {
                    currentLine = part;
                }
            }
        }

        if (!currentLine.isEmpty()) {
            result.append(currentLine);
        }

        return result.toString();
    }

    public static String formatSelection(String code, int selectionStart, int selectionEnd) {
        // Extract the selection
        String beforeSelection = code.substring(0, selectionStart);
        String selection = code.substring(selectionStart, selectionEnd);
        String afterSelection = code.substring(selectionEnd);

        // Format only the selection
        String formattedSelection = formatCode(selection);

        // Reconstruct the code
        return beforeSelection + formattedSelection + afterSelection;
    }

    public static boolean isFormattingNeeded(String code) {
        String formatted = formatCode(code);
        return !code.equals(formatted);
    }
}