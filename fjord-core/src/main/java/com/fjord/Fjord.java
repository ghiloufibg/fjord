package com.fjord;

import com.fjord.ast.Expression;
import com.fjord.bytecode.BytecodeGenerator;
import com.fjord.bytecode.BytecodeProgram;
import com.fjord.bytecode.FjordVM;
import com.fjord.compiler.CompilationResult;
import com.fjord.compiler.FjordCompiler;
import com.fjord.lex.Token;
import com.fjord.lex.Tokenizer;
import com.fjord.parser.Parser;
import com.fjord.runtime.Environment;
import com.fjord.runtime.FunctionValue;
import com.fjord.runtime.Value;
import com.fjord.runtime.builtin.BuiltinRegistry;
import com.fjord.runtime.builtin.PrintlnFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command line entry point for the Fjord interpreter and compiler.
 * This class provides multiple execution modes:
 *
 * <ul>
 *   <li><strong>Tree-walking mode:</strong> Traditional AST interpretation (default)</li>
 *   <li><strong>Bytecode mode:</strong> Compile to bytecode and execute on VM (--bytecode)</li>
 *   <li><strong>Compiler mode:</strong> Full compilation pipeline with error reporting (--compile)</li>
 * </ul>
 *
 * <p>The new architecture provides:
 * <ul>
 *   <li>Comprehensive error reporting with source locations</li>
 *   <li>Semantic analysis and type checking</li>
 *   <li>Performance optimization through bytecode compilation</li>
 *   <li>Extensible built-in function registry</li>
 * </ul>
 *
 * <p>Usage examples:
 * <pre>{@code
 * fjord program.fj                    # Tree-walking interpreter
 * fjord --bytecode program.fj         # Bytecode VM execution
 * fjord --compile program.fj          # Full compilation pipeline
 * fjord --verbose program.fj          # Enable detailed logging
 * }</pre>
 */
public final class Fjord {

    private Fjord() {
    }

    /**
     * Execution modes for the Fjord runtime.
     */
    private enum ExecutionMode {
        /** Traditional tree-walking interpreter */
        TREE_WALKING,
        /** Bytecode compilation and VM execution */
        BYTECODE,
        /** Full compilation pipeline with comprehensive error reporting */
        COMPILE_ONLY
    }

    /**
     * Launches the Fjord interpreter/compiler with support for multiple execution modes.
     * Parses command line arguments to determine the execution mode and options.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(64);
        }

        // Parse command line arguments
        ExecutionMode mode = ExecutionMode.TREE_WALKING;
        boolean verbose = false;
        boolean showHelp = false;
        String sourceFile = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help", "-h" -> showHelp = true;
                case "--bytecode", "-b" -> mode = ExecutionMode.BYTECODE;
                case "--compile", "-c" -> mode = ExecutionMode.COMPILE_ONLY;
                case "--verbose", "-v" -> verbose = true;
                default -> {
                    if (!arg.startsWith("-")) {
                        sourceFile = arg;
                    } else {
                        System.err.println("Unknown option: " + arg);
                        System.exit(64);
                    }
                }
            }
        }

        if (showHelp) {
            printUsage();
            System.exit(0);
        }

        if (sourceFile == null) {
            System.err.println("Error: No source file specified");
            printUsage();
            System.exit(64);
        }

        // Read source file
        String source;
        try {
            source = Files.readString(Path.of(sourceFile));
        } catch (IOException e) {
            System.err.println("Error: Could not read file: " + sourceFile);
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(65);
            return;
        }

        // Execute based on mode
        try {
            switch (mode) {
                case TREE_WALKING -> executeTreeWalking(source, verbose);
                case BYTECODE -> executeBytecode(source, verbose);
                case COMPILE_ONLY -> executeCompileOnly(source, verbose);
            }
        } catch (Exception e) {
            System.err.println("Execution failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(70);
        }
    }

    /**
     * Executes using the traditional tree-walking interpreter.
     */
    private static void executeTreeWalking(String source, boolean verbose) {
        if (verbose) {
            System.out.println("[FJORD] Using tree-walking interpreter");
        }

        // Use original implementation for backward compatibility
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();
        Parser parser = new Parser(tokens);
        List<Expression> program = parser.parseProgram();

        Environment env = new Environment();
        // Install built-in functions
        BuiltinRegistry builtins = BuiltinRegistry.createDefault();
        env.define("println", new PrintlnFunction());

        // Add other built-in functions
        for (String name : builtins.getBuiltinNames()) {
            var builtin = builtins.getFunction(name);
            if (builtin.isPresent() && !name.equals("println")) {
                env.define(name, builtin.get());
            }
        }

        // Evaluate top-level statements
        Value last = null;
        for (Expression expr : program) {
            last = expr.evaluate(env);
        }

        // Handle main function
        env.get("main").ifPresent(value -> {
            if (value instanceof FunctionValue) {
                FunctionValue mainFn = (FunctionValue) value;
                if (!mainFn.getParameters().isEmpty()) {
                    throw new IllegalStateException("main function must not take arguments");
                }
                mainFn.getBody().evaluate(new Environment(mainFn.getClosure()));
            } else {
                throw new IllegalStateException("main is not a function");
            }
        });

        // Print result if appropriate
        if (!env.get("main").isPresent() && last != null &&
            !(last instanceof com.fjord.runtime.UnitValue)) {
            System.out.println(last.getValue());
        }

        if (verbose) {
            System.out.println("[FJORD] Execution completed");
        }
    }

    /**
     * Executes using bytecode compilation and VM.
     */
    private static void executeBytecode(String source, boolean verbose) {
        if (verbose) {
            System.out.println("[FJORD] Using bytecode VM execution");
        }

        // Compile to bytecode
        FjordCompiler compiler = verbose ? FjordCompiler.verbose() : FjordCompiler.quiet();
        CompilationResult result = compiler.compile(source);

        if (result.isFailure()) {
            System.err.println("Compilation failed:");
            result.getErrorReporter().printErrorSummary();
            System.exit(65);
        }

        List<Expression> program = result.getProgram().get();

        // Generate bytecode
        BytecodeGenerator generator = new BytecodeGenerator();
        BytecodeProgram bytecodeProgram = generator.generate(program);

        if (verbose) {
            System.out.println("[FJORD] Generated bytecode:");
            System.out.println(bytecodeProgram.disassemble());
        }

        // Execute on VM
        BuiltinRegistry builtins = BuiltinRegistry.createDefault();
        FjordVM vm = new FjordVM(builtins, verbose);
        Value finalResult = vm.execute(bytecodeProgram);

        if (!(finalResult instanceof com.fjord.runtime.UnitValue)) {
            System.out.println(finalResult.getValue());
        }

        if (verbose) {
            System.out.println("[FJORD] VM execution completed");
        }
    }

    /**
     * Executes compilation pipeline without running the program.
     */
    private static void executeCompileOnly(String source, boolean verbose) {
        if (verbose) {
            System.out.println("[FJORD] Compile-only mode");
        }

        FjordCompiler compiler = verbose ? FjordCompiler.verbose() : FjordCompiler.quiet();
        CompilationResult result = compiler.compile(source);

        if (result.isFailure()) {
            System.err.println("Compilation failed:");
            result.getErrorReporter().printErrorSummary();
            System.exit(65);
        } else {
            System.out.println("Compilation successful");
            if (verbose) {
                List<Expression> program = result.getProgram().get();
                System.out.println("Generated " + program.size() + " top-level expressions");
            }
        }
    }

    /**
     * Prints usage information.
     */
    private static void printUsage() {
        System.out.println("Fjord - A functional programming language for the JVM");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("    fjord [OPTIONS] <source.fj>");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("    -h, --help       Show this help message");
        System.out.println("    -v, --verbose    Enable verbose output");
        System.out.println("    -b, --bytecode   Use bytecode VM execution (faster)");
        System.out.println("    -c, --compile    Compile only, don't execute");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("    fjord hello.fj                 # Tree-walking interpreter");
        System.out.println("    fjord --bytecode hello.fj       # Bytecode VM (recommended)");
        System.out.println("    fjord --verbose hello.fj        # Show detailed execution info");
        System.out.println("    fjord --compile hello.fj        # Check syntax and types only");
    }
}