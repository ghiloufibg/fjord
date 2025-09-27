package com.fjord.ide.integration;

import com.fjord.compiler.CompilationResult;
import com.fjord.compiler.FjordCompiler;
import com.fjord.ast.Expression;
import com.fjord.bytecode.BytecodeGenerator;
import com.fjord.bytecode.BytecodeProgram;
import com.fjord.bytecode.FjordVM;
import com.fjord.lex.Token;
import com.fjord.lex.Tokenizer;
import com.fjord.parser.Parser;
import com.fjord.runtime.Environment;
import com.fjord.runtime.FunctionValue;
import com.fjord.runtime.Value;
import com.fjord.runtime.builtin.BuiltinRegistry;
import com.fjord.runtime.builtin.PrintlnFunction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;

public class FjordRunner {
    private final Consumer<String> outputConsumer;

    public FjordRunner(Consumer<String> outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    public void runCode(String sourceCode, ExecutionMode mode) {
        try {
            switch (mode) {
                case TREE_WALKING -> runTreeWalking(sourceCode);
                case BYTECODE -> runBytecode(sourceCode);
                case COMPILE_ONLY -> compileOnly(sourceCode);
            }
        } catch (Exception e) {
            outputConsumer.accept("Error: " + e.getMessage() + "\n");
        }
    }

    private void runTreeWalking(String sourceCode) {
        outputConsumer.accept("Running with tree-walking interpreter...\n");

        // Capture System.out to redirect to our console
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            Tokenizer tokenizer = new Tokenizer(sourceCode);
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

        } finally {
            System.setOut(originalOut);
            String output = baos.toString();
            if (!output.isEmpty()) {
                outputConsumer.accept(output);
            }
            outputConsumer.accept("Execution completed.\n");
        }
    }

    private void runBytecode(String sourceCode) {
        outputConsumer.accept("Running with bytecode VM...\n");

        // Capture System.out to redirect to our console
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            FjordCompiler compiler = FjordCompiler.quiet();
            CompilationResult result = compiler.compile(sourceCode);

            if (result.isFailure()) {
                outputConsumer.accept("Compilation failed:\n");
                // TODO: Better error formatting
                result.getErrorReporter().printErrorSummary();
                return;
            }

            List<Expression> program = result.getProgram().get();

            // Generate bytecode
            BytecodeGenerator generator = new BytecodeGenerator();
            BytecodeProgram bytecodeProgram = generator.generate(program);

            // Execute on VM
            BuiltinRegistry builtins = BuiltinRegistry.createDefault();
            FjordVM vm = new FjordVM(builtins, false);
            Value finalResult = vm.execute(bytecodeProgram);

            if (!(finalResult instanceof com.fjord.runtime.UnitValue)) {
                System.out.println(finalResult.getValue());
            }

        } finally {
            System.setOut(originalOut);
            String output = baos.toString();
            if (!output.isEmpty()) {
                outputConsumer.accept(output);
            }
            outputConsumer.accept("VM execution completed.\n");
        }
    }

    private void compileOnly(String sourceCode) {
        outputConsumer.accept("Compiling...\n");

        FjordCompiler compiler = FjordCompiler.quiet();
        CompilationResult result = compiler.compile(sourceCode);

        if (result.isFailure()) {
            outputConsumer.accept("Compilation failed:\n");
            // TODO: Better error formatting for IDE
            result.getErrorReporter().printErrorSummary();
        } else {
            outputConsumer.accept("Compilation successful.\n");
            List<Expression> program = result.getProgram().get();
            outputConsumer.accept("Generated " + program.size() + " top-level expressions.\n");
        }
    }

    public enum ExecutionMode {
        TREE_WALKING,
        BYTECODE,
        COMPILE_ONLY
    }
}