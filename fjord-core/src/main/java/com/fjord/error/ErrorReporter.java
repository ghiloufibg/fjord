package com.fjord.error;

import com.fjord.lex.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive error reporting system for the Fjord compiler.
 * Collects and formats errors with precise location information and
 * user-friendly error messages. Supports different error types and
 * provides context for better debugging.
 *
 * <p>Example usage:
 * <pre>{@code
 * ErrorReporter reporter = new ErrorReporter();
 * reporter.lexicalError(new SourcePosition(5, 10, 89), "Unterminated string literal");
 * if (reporter.hasErrors()) {
 *     reporter.printErrors();
 * }
 * }</pre>
 */
public final class ErrorReporter {

    /** Collection of all reported errors */
    private final List<FjordError> errors = new ArrayList<>();

    /** Whether to print errors immediately when reported */
    private boolean immediateOutput = false;

    /**
     * Creates a new error reporter.
     */
    public ErrorReporter() {
    }

    /**
     * Creates a new error reporter with optional immediate output.
     *
     * @param immediateOutput if true, errors are printed immediately when reported
     */
    public ErrorReporter(boolean immediateOutput) {
        this.immediateOutput = immediateOutput;
    }

    /**
     * Reports a lexical error at the specified position.
     * Used for tokenization issues like invalid characters or unterminated strings.
     *
     * @param position the source position where error occurred
     * @param message  human-readable error description
     */
    public void lexicalError(SourcePosition position, String message) {
        FjordError error = new FjordError.LexicalError(position, message);
        addError(error);
    }

    /**
     * Reports a syntax error during parsing.
     * Used when the parser encounters unexpected tokens or malformed syntax.
     *
     * @param token    the problematic token
     * @param expected description of what was expected at this position
     */
    public void syntaxError(Token token, String expected) {
        FjordError error = new FjordError.SyntaxError(
            token.getPosition(),
            token.getLexeme(),
            expected
        );
        addError(error);
    }

    /**
     * Reports a semantic error during analysis.
     * Used for type checking errors, undefined variables, etc.
     *
     * @param position the source position where error occurred
     * @param message  detailed error description
     */
    public void semanticError(SourcePosition position, String message) {
        FjordError error = new FjordError.SemanticError(position, message);
        addError(error);
    }

    /**
     * Reports a runtime error during execution.
     * Used for evaluation errors like division by zero or stack overflow.
     *
     * @param position the source position where error occurred
     * @param message  detailed error description
     */
    public void runtimeError(SourcePosition position, String message) {
        FjordError error = new FjordError.RuntimeError(position, message);
        addError(error);
    }

    /**
     * Adds a generic error to the collection.
     *
     * @param error the error to add
     */
    private void addError(FjordError error) {
        errors.add(error);
        if (immediateOutput) {
            System.err.println(error);
        }
    }

    /**
     * Returns whether any errors have been reported.
     *
     * @return true if there are any errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns the number of errors reported.
     *
     * @return error count
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Returns an unmodifiable view of all reported errors.
     *
     * @return list of all errors
     */
    public List<FjordError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Prints all errors to standard error stream.
     * Each error is printed on a separate line with full location information.
     */
    public void printErrors() {
        for (FjordError error : errors) {
            System.err.println(error);
        }
    }

    /**
     * Prints all errors with a summary header.
     * Useful for showing compilation results.
     */
    public void printErrorSummary() {
        if (errors.isEmpty()) {
            return;
        }

        System.err.println("Compilation failed with " + errors.size() + " error(s):");
        System.err.println();

        for (int i = 0; i < errors.size(); i++) {
            System.err.printf("%d. %s%n", i + 1, errors.get(i));
        }
    }

    /**
     * Clears all reported errors.
     * Useful for reusing the same reporter instance.
     */
    public void clear() {
        errors.clear();
    }

    /**
     * Returns the first error of the specified type, if any.
     *
     * @param type the error type to search for
     * @return the first matching error, or null if none found
     */
    public FjordError getFirstErrorOfType(FjordError.ErrorType type) {
        return errors.stream()
                .filter(error -> error.type == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns all errors of the specified type.
     *
     * @param type the error type to filter by
     * @return list of matching errors
     */
    public List<FjordError> getErrorsOfType(FjordError.ErrorType type) {
        return errors.stream()
                .filter(error -> error.type == type)
                .toList();
    }
}