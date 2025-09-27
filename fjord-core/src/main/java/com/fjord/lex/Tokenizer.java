package com.fjord.lex;

import com.fjord.error.SourcePosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts source code into a linear sequence of {@link Token}s. The
 * {@code Tokenizer} performs no validation beyond recognising lexemes;
 * syntactic structure is handled later by the parser. Whitespace and
 * comments are skipped. Each token is annotated with precise source location
 * information for better error reporting.
 *
 * <p>The tokenizer tracks line and column numbers as it processes the source,
 * enabling detailed error messages that help users locate problems in their code.
 *
 * <p>Example usage:
 * <pre>{@code
 * Tokenizer tokenizer = new Tokenizer("let x = 42");
 * List<Token> tokens = tokenizer.tokenize();
 * for (Token token : tokens) {
 *     System.out.println(token.toDetailedString());
 * }
 * }</pre>
 */
public class Tokenizer {

    /** Mapping of language keywords to their token types. */
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("let", TokenType.LET);
        KEYWORDS.put("mut", TokenType.MUT);
        KEYWORDS.put("fn", TokenType.FN);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("then", TokenType.THEN);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("do", TokenType.DO);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("struct", TokenType.STRUCT);
        KEYWORDS.put("true", TokenType.BOOL_LITERAL);
        KEYWORDS.put("false", TokenType.BOOL_LITERAL);
    }

    private final String source;
    private final int length;
    private final List<Token> tokens = new ArrayList<>();
    private int current = 0;
    private int line = 1;
    private int column = 1;

    /**
     * Creates a tokenizer for the given source string.
     *
     * @param source the program text to be tokenised
     */
    public Tokenizer(String source) {
        this.source = source;
        this.length = source.length();
    }

    /**
     * Produces a list of tokens representing the input. A terminal EOF token is
     * always appended to the end of the list.
     *
     * @return a list of {@link Token}s
     */
    public List<Token> tokenize() {
        while (!isAtEnd()) {
            skipWhitespace();
            int start = current;
            if (isAtEnd()) break;
            char c = advance();
            switch (c) {
                case '(': addToken(TokenType.LPAREN, "("); break;
                case ')': addToken(TokenType.RPAREN, ")"); break;
                case '{': addToken(TokenType.LBRACE, "{"); break;
                case '}': addToken(TokenType.RBRACE, "}"); break;
                case '[': addToken(TokenType.LBRACKET, "["); break;
                case ']': addToken(TokenType.RBRACKET, "]"); break;
                case ',': addToken(TokenType.COMMA, ","); break;
                case ':': addToken(TokenType.COLON, ":"); break;
                case ';': addToken(TokenType.SEMICOLON, ";"); break;
                case '+': addToken(TokenType.PLUS, "+"); break;
                case '-': {
                    if (match('>')) {
                        addToken(TokenType.ARROW, "->");
                    } else {
                        addToken(TokenType.MINUS, "-");
                    }
                    break;
                }
                case '*': addToken(TokenType.STAR, "*"); break;
                case '/': {
                    if (match('/')) {
                        // skip single line comment
                        while (peek() != '\n' && !isAtEnd()) advance();
                    } else {
                        addToken(TokenType.SLASH, "/");
                    }
                    break;
                }
                case '=': addToken(match('=') ? TokenType.EQEQ : TokenType.EQUAL, match('=') ? "==" : "="); break;
                case '!': addToken(match('=') ? TokenType.BANGEQ : null, "!="); break;
                case '>': addToken(match('=') ? TokenType.GTE : TokenType.GT, match('=') ? ">=" : ">"); break;
                case '<': addToken(match('=') ? TokenType.LTE : TokenType.LT, match('=') ? "<=" : "<"); break;
                case '.': addToken(TokenType.DOT, "."); break;
                case '%': addToken(TokenType.PERCENT, "%"); break;
                case '"': stringLiteral(); break;
                default:
                    if (isDigit(c)) {
                        numberLiteral();
                    } else if (isAlpha(c)) {
                        identifierOrKeyword();
                    } else {
                        throw new IllegalStateException("Unexpected character: '" + c + "'");
                    }
                    break;
            }
        }
        tokens.add(new Token(TokenType.EOF, "", getCurrentPosition()));
        return tokens;
    }

    /**
     * Consumes a sequence of digits and optional decimal point as a number
     * literal. Distinguishes between integers and floating point numbers based
     * on the presence of a decimal point.
     */
    private void numberLiteral() {
        int start = current - 1;
        int startLine = line;
        int startColumn = column - 1;

        while (isDigit(peek())) advance();
        boolean isFloat = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isFloat = true;
            advance(); // consume '.'
            while (isDigit(peek())) advance();
        }
        String lexeme = source.substring(start, current);
        SourcePosition position = new SourcePosition(startLine, startColumn, start);
        tokens.add(new Token(isFloat ? TokenType.FLOAT_LITERAL : TokenType.INTEGER_LITERAL, lexeme, position));
    }

    /**
     * Consumes a string literal enclosed in double quotes. Supports a minimal
     * set of escape sequences (\n for newline, \t for tab, \\ for backslash,
     * \" for a double quote). If the string is not closed before EOF then
     * an exception is thrown.
     */
    private void stringLiteral() {
        int startLine = line;
        int startColumn = column - 1; // Account for opening quote
        int start = current - 1;

        StringBuilder builder = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\\') {
                char next = advance();
                switch (next) {
                    case 'n': builder.append('\n'); break;
                    case 't': builder.append('\t'); break;
                    case '"': builder.append('"'); break;
                    case '\\': builder.append('\\'); break;
                    default: throw new IllegalStateException("Unknown escape sequence: \\" + next);
                }
            } else {
                builder.append(c);
            }
        }
        if (isAtEnd()) {
            throw new IllegalStateException("Unterminated string literal");
        }
        advance(); // closing quote
        SourcePosition position = new SourcePosition(startLine, startColumn, start);
        tokens.add(new Token(TokenType.STRING_LITERAL, builder.toString(), position));
    }

    /**
     * Consumes an identifier or keyword. If the lexeme matches a reserved
     * keyword its corresponding token type is used, otherwise
     * {@link TokenType#IDENTIFIER} is used.
     */
    private void identifierOrKeyword() {
        int start = current - 1;
        int startLine = line;
        int startColumn = column - 1;

        while (isAlphaNumeric(peek())) advance();
        String lexeme = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(lexeme, TokenType.IDENTIFIER);
        SourcePosition position = new SourcePosition(startLine, startColumn, start);
        tokens.add(new Token(type, lexeme, position));
    }

    /**
     * Skips over whitespace and ignores comments. Line comments start with
     * two slashes and run to the end of the line.
     */
    private void skipWhitespace() {
        while (!isAtEnd()) {
            char c = peek();
            switch (c) {
                case ' ': case '\r': case '\t': case '\n':
                    advance();
                    break;
                case '/':
                    if (peekNext() == '/') {
                        // skip comment
                        while (peek() != '\n' && !isAtEnd()) advance();
                    } else {
                        return;
                    }
                    break;
                default:
                    return;
            }
        }
    }

    /** Returns {@code true} if all characters have been consumed. */
    private boolean isAtEnd() {
        return current >= length;
    }

    /** Advances one character in the source and returns it. */
    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    /** Adds a token of the given type and lexeme to the token list. */
    private void addToken(TokenType type, String lexeme) {
        if (type == null) {
            throw new IllegalStateException("Unexpected token");
        }
        // Calculate position based on current location minus lexeme length
        int tokenColumn = Math.max(1, column - lexeme.length());
        SourcePosition position = new SourcePosition(line, tokenColumn, current - lexeme.length());
        tokens.add(new Token(type, lexeme, position));
    }

    /** Creates a source position for the current location. */
    private SourcePosition getCurrentPosition() {
        return new SourcePosition(line, column, current);
    }

    /** Returns {@code true} if the next character matches the expected
     * character; if so advances the cursor. */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    /** Peeks at the current character without consuming it. */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /** Peeks ahead by one character. */
    private char peekNext() {
        if (current + 1 >= length) return '\0';
        return source.charAt(current + 1);
    }

    /** Returns {@code true} if the given character is a digit. */
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** Returns {@code true} if the given character is an alphabetic
     * character or underscore. */
    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /** Returns {@code true} if the given character is alphanumeric or
     * underscore. */
    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}