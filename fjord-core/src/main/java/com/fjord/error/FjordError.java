package com.fjord.error;

/**
 * Base class for all Fjord compiler errors.
 * Provides structured error information with source location and categorization.
 */
public abstract class FjordError {

    /** The type of error that occurred */
    public enum ErrorType {
        LEXICAL,    // Tokenization errors (invalid characters, unterminated strings)
        SYNTAX,     // Parsing errors (missing tokens, malformed expressions)
        SEMANTIC,   // Type checking errors (undefined variables, type mismatches)
        RUNTIME     // Evaluation errors (division by zero, stack overflow)
    }

    /** The position in source code where this error occurred */
    public final SourcePosition position;

    /** Human-readable error message */
    public final String message;

    /** Category of this error */
    public final ErrorType type;

    /**
     * Creates a new Fjord compiler error.
     *
     * @param type     the category of error
     * @param position the source location where error occurred
     * @param message  human-readable description of the error
     */
    protected FjordError(ErrorType type, SourcePosition position, String message) {
        this.type = type;
        this.position = position;
        this.message = message;
    }

    /**
     * Returns a formatted error message suitable for display to users.
     * Format: "[ErrorType] at line X, column Y: message"
     *
     * @return formatted error string
     */
    @Override
    public String toString() {
        return String.format("[%s] at %s: %s", type, position, message);
    }

    /**
     * Lexical analysis errors such as invalid characters or unterminated strings.
     */
    public static final class LexicalError extends FjordError {
        public LexicalError(SourcePosition position, String message) {
            super(ErrorType.LEXICAL, position, message);
        }
    }

    /**
     * Syntax errors during parsing such as missing tokens or malformed expressions.
     */
    public static final class SyntaxError extends FjordError {
        /** The token that caused the syntax error */
        public final String actualToken;
        /** What was expected at this position */
        public final String expected;

        public SyntaxError(SourcePosition position, String actualToken, String expected) {
            super(ErrorType.SYNTAX, position,
                  String.format("Expected %s but found '%s'", expected, actualToken));
            this.actualToken = actualToken;
            this.expected = expected;
        }
    }

    /**
     * Semantic errors such as undefined variables or type mismatches.
     */
    public static final class SemanticError extends FjordError {
        public SemanticError(SourcePosition position, String message) {
            super(ErrorType.SEMANTIC, position, message);
        }
    }

    /**
     * Runtime errors that occur during program execution.
     */
    public static final class RuntimeError extends FjordError {
        public RuntimeError(SourcePosition position, String message) {
            super(ErrorType.RUNTIME, position, message);
        }
    }
}