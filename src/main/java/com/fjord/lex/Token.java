package com.fjord.lex;

import com.fjord.error.SourcePosition;

/**
 * A token produced by the {@link Tokenizer}. A token consists of a
 * {@link TokenType}, the raw lexeme from the input, and its source position.
 * Tokens are used by the parser to build the abstract syntax tree and provide
 * precise error location information.
 *
 * <p>Example usage:
 * <pre>{@code
 * Token token = new Token(TokenType.IDENTIFIER, "variable", new SourcePosition(1, 5, 4));
 * System.out.println("Token " + token.getLexeme() + " at " + token.getPosition());
 * }</pre>
 */
public class Token {

    private final TokenType type;
    private final String lexeme;
    private final SourcePosition position;

    /**
     * Constructs a new token with position information.
     *
     * @param type     the classification of this token
     * @param lexeme   the exact string consumed from the source code
     * @param position the source location where this token was found
     */
    public Token(TokenType type, String lexeme, SourcePosition position) {
        this.type = type;
        this.lexeme = lexeme;
        this.position = position;
    }

    /**
     * Constructs a new token without position information.
     * This constructor is provided for backward compatibility.
     *
     * @param type   the classification of this token
     * @param lexeme the exact string consumed from the source code
     * @deprecated Use {@link #Token(TokenType, String, SourcePosition)} instead
     */
    @Deprecated
    public Token(TokenType type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
        this.position = new SourcePosition(1, 1, 0); // Default position
    }

    /**
     * Returns the type of the token.
     *
     * @return the token type
     */
    public TokenType getType() {
        return type;
    }

    /**
     * Returns the raw lexeme corresponding to the token. For literals this is
     * the textual representation; for identifiers it is the variable name.
     *
     * @return the lexeme
     */
    public String getLexeme() {
        return lexeme;
    }

    /**
     * Returns the source position where this token was found.
     *
     * @return the source position
     */
    public SourcePosition getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return type + "(" + lexeme + ")";
    }

    /**
     * Returns a detailed string representation including position information.
     *
     * @return formatted string with type, lexeme, and position
     */
    public String toDetailedString() {
        return String.format("%s('%s') at %s", type, lexeme, position);
    }
}