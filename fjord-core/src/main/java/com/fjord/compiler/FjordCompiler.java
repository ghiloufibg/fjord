package com.fjord.compiler;

import com.fjord.ast.Expression;
import com.fjord.error.ErrorReporter;
import com.fjord.error.FjordError;
import com.fjord.lex.Token;
import com.fjord.lex.Tokenizer;
import com.fjord.parser.Parser;

import java.util.List;

/**
 * Main compiler pipeline orchestrating all compilation phases.
 * Follows the traditional compiler design with clear phase separation:
 *
 * <ol>
 *   <li><strong>Lexical Analysis:</strong> Source code → Tokens</li>
 *   <li><strong>Syntax Analysis:</strong> Tokens → Abstract Syntax Tree</li>
 *   <li><strong>Semantic Analysis:</strong> AST → Typed AST (future)</li>
 *   <li><strong>Code Generation:</strong> AST → Bytecode/Execution (future)</li>
 * </ol>
 *
 * <p>The compiler provides comprehensive error reporting at each phase,
 * enabling developers to quickly identify and fix issues in their code.
 *
 * <p>Example usage:
 * <pre>{@code
 * FjordCompiler compiler = new FjordCompiler();
 * CompilationResult result = compiler.compile("let x = 42; x + 1");
 *
 * if (result.isSuccess()) {
 *     List<Expression> program = result.getProgram().get();
 *     // ... execute or further process
 * } else {
 *     result.getErrorReporter().printErrorSummary();
 * }
 * }</pre>
 */
public final class FjordCompiler {

    /** Whether to enable detailed logging of compilation phases */
    private final boolean verbose;

    /**
     * Creates a new Fjord compiler with default settings.
     */
    public FjordCompiler() {
        this(false);
    }

    /**
     * Creates a new Fjord compiler with optional verbose output.
     *
     * @param verbose if true, enables detailed compilation phase logging
     */
    public FjordCompiler(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Compiles source code through all phases: lex → parse → analyze → generate.
     * Returns a result object containing either the compiled program or error information.
     *
     * @param source the source code to compile
     * @return CompilationResult containing either success or error information
     */
    public CompilationResult compile(String source) {
        if (source == null || source.trim().isEmpty()) {
            return CompilationResult.failure(
                new FjordError.SyntaxError(
                    new com.fjord.error.SourcePosition(1, 1, 0),
                    "empty",
                    "program content"
                )
            );
        }

        ErrorReporter errorReporter = new ErrorReporter();

        try {
            // Phase 1: Lexical Analysis
            if (verbose) {
                System.out.println("[COMPILER] Starting lexical analysis...");
            }

            List<Token> tokens = performLexicalAnalysis(source, errorReporter);
            if (errorReporter.hasErrors()) {
                if (verbose) {
                    System.out.println("[COMPILER] Lexical analysis failed with " +
                                       errorReporter.getErrorCount() + " error(s)");
                }
                return CompilationResult.failure(errorReporter);
            }

            if (verbose) {
                System.out.println("[COMPILER] Lexical analysis completed. Generated " +
                                   tokens.size() + " tokens");
            }

            // Phase 2: Syntax Analysis
            if (verbose) {
                System.out.println("[COMPILER] Starting syntax analysis...");
            }

            List<Expression> program = performSyntaxAnalysis(tokens, errorReporter);
            if (errorReporter.hasErrors()) {
                if (verbose) {
                    System.out.println("[COMPILER] Syntax analysis failed with " +
                                       errorReporter.getErrorCount() + " error(s)");
                }
                return CompilationResult.failure(errorReporter);
            }

            if (verbose) {
                System.out.println("[COMPILER] Syntax analysis completed. Generated AST with " +
                                   program.size() + " top-level expressions");
            }

            // Future phases would go here:
            // - Semantic analysis (type checking, symbol resolution)
            // - Optimization passes
            // - Code generation

            if (verbose) {
                System.out.println("[COMPILER] Compilation completed successfully");
            }

            return CompilationResult.success(program);

        } catch (Exception e) {
            // Handle unexpected compilation errors
            errorReporter.runtimeError(
                new com.fjord.error.SourcePosition(1, 1, 0),
                "Internal compiler error: " + e.getMessage()
            );
            return CompilationResult.failure(errorReporter);
        }
    }

    /**
     * Performs lexical analysis on the source code.
     *
     * @param source        the source code to tokenize
     * @param errorReporter error reporter to collect lexical errors
     * @return list of tokens, or empty list if errors occurred
     */
    private List<Token> performLexicalAnalysis(String source, ErrorReporter errorReporter) {
        try {
            Tokenizer tokenizer = new Tokenizer(source);
            return tokenizer.tokenize();
        } catch (IllegalStateException e) {
            // Convert tokenizer exceptions to proper error reports
            errorReporter.lexicalError(
                new com.fjord.error.SourcePosition(1, 1, 0),
                e.getMessage()
            );
            return List.of();
        }
    }

    /**
     * Performs syntax analysis on the token stream.
     *
     * @param tokens        the tokens to parse
     * @param errorReporter error reporter to collect syntax errors
     * @return parsed AST, or empty list if errors occurred
     */
    private List<Expression> performSyntaxAnalysis(List<Token> tokens, ErrorReporter errorReporter) {
        try {
            Parser parser = new Parser(tokens);
            return parser.parseProgram();
        } catch (IllegalStateException e) {
            // Convert parser exceptions to proper error reports
            errorReporter.syntaxError(
                new Token(com.fjord.lex.TokenType.EOF, "", new com.fjord.error.SourcePosition(1, 1, 0)),
                "valid program structure"
            );
            return List.of();
        }
    }

    /**
     * Performs semantic analysis on the AST (future implementation).
     *
     * @param program       the AST to analyze
     * @param errorReporter error reporter to collect semantic errors
     * @return true if semantic analysis passed
     */
    private boolean performSemanticAnalysis(List<Expression> program, ErrorReporter errorReporter) {
        // TODO: Implement semantic analysis
        // - Type checking
        // - Variable resolution
        // - Function signature validation
        // - Dead code detection
        return true;
    }

    /**
     * Returns a compiler configured for verbose output.
     *
     * @return verbose compiler instance
     */
    public static FjordCompiler verbose() {
        return new FjordCompiler(true);
    }

    /**
     * Returns a compiler configured for quiet operation.
     *
     * @return quiet compiler instance
     */
    public static FjordCompiler quiet() {
        return new FjordCompiler(false);
    }
}