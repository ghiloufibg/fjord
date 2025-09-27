package com.fjord.compiler;

import com.fjord.ast.Expression;
import com.fjord.error.ErrorReporter;
import com.fjord.error.FjordError;

import java.util.List;
import java.util.Optional;

/**
 * Represents the result of a compilation operation.
 * Contains either a successfully compiled program or error information.
 * This follows the Result pattern to handle success and failure cases cleanly.
 *
 * <p>Example usage:
 * <pre>{@code
 * CompilationResult result = compiler.compile(sourceCode);
 * if (result.isSuccess()) {
 *     List<Expression> program = result.getProgram().get();
 *     // ... execute or further process program
 * } else {
 *     result.getErrorReporter().printErrorSummary();
 * }
 * }</pre>
 */
public final class CompilationResult {

    private final List<Expression> program;
    private final ErrorReporter errorReporter;
    private final boolean success;

    /**
     * Creates a successful compilation result.
     *
     * @param program the compiled program
     */
    private CompilationResult(List<Expression> program) {
        this.program = List.copyOf(program);
        this.errorReporter = new ErrorReporter();
        this.success = true;
    }

    /**
     * Creates a failed compilation result.
     *
     * @param errorReporter the error reporter containing compilation errors
     */
    private CompilationResult(ErrorReporter errorReporter) {
        this.program = null;
        this.errorReporter = errorReporter;
        this.success = false;
    }

    /**
     * Returns whether the compilation was successful.
     *
     * @return true if compilation succeeded
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns whether the compilation failed.
     *
     * @return true if compilation failed
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Returns the compiled program if compilation was successful.
     *
     * @return optional containing the program, or empty if compilation failed
     */
    public Optional<List<Expression>> getProgram() {
        return success ? Optional.of(program) : Optional.empty();
    }

    /**
     * Returns the error reporter containing any compilation errors.
     *
     * @return the error reporter
     */
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    /**
     * Returns all compilation errors.
     *
     * @return list of compilation errors
     */
    public List<FjordError> getErrors() {
        return errorReporter.getErrors();
    }

    /**
     * Returns the number of compilation errors.
     *
     * @return error count
     */
    public int getErrorCount() {
        return errorReporter.getErrorCount();
    }

    /**
     * Creates a successful compilation result.
     *
     * @param program the compiled program
     * @return successful compilation result
     */
    public static CompilationResult success(List<Expression> program) {
        return new CompilationResult(program);
    }

    /**
     * Creates a failed compilation result.
     *
     * @param errorReporter the error reporter containing errors
     * @return failed compilation result
     */
    public static CompilationResult failure(ErrorReporter errorReporter) {
        if (!errorReporter.hasErrors()) {
            throw new IllegalArgumentException("Cannot create failure result without errors");
        }
        return new CompilationResult(errorReporter);
    }

    /**
     * Creates a failed compilation result with a single error.
     *
     * @param error the compilation error
     * @return failed compilation result
     */
    public static CompilationResult failure(FjordError error) {
        ErrorReporter reporter = new ErrorReporter();
        // Add the error based on its type
        switch (error.type) {
            case LEXICAL -> reporter.lexicalError(error.position, error.message);
            case SYNTAX -> reporter.syntaxError(
                new com.fjord.lex.Token(
                    com.fjord.lex.TokenType.IDENTIFIER,
                    "unknown",
                    error.position
                ),
                error.message
            );
            case SEMANTIC -> reporter.semanticError(error.position, error.message);
            case RUNTIME -> reporter.runtimeError(error.position, error.message);
        }
        return new CompilationResult(reporter);
    }

    /**
     * Returns a string representation of the compilation result.
     *
     * @return formatted result description
     */
    @Override
    public String toString() {
        if (success) {
            return String.format("CompilationResult{success=true, programSize=%d}", program.size());
        } else {
            return String.format("CompilationResult{success=false, errorCount=%d}", errorReporter.getErrorCount());
        }
    }
}