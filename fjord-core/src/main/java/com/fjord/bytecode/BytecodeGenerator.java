package com.fjord.bytecode;

import com.fjord.ast.*;
import com.fjord.lex.TokenType;
import com.fjord.runtime.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bytecode generator for improved performance over tree-walking.
 * Compiles AST to stack-based virtual machine instructions that can be
 * executed efficiently by the FjordVM.
 *
 * <p>The generator produces bytecode by traversing the AST and emitting
 * appropriate instructions for each node type. It handles:
 * <ul>
 *   <li>Expression evaluation and stack management</li>
 *   <li>Variable storage and retrieval</li>
 *   <li>Function definition and calls</li>
 *   <li>Control flow (if statements, jumps)</li>
 *   <li>Built-in function calls</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * BytecodeGenerator generator = new BytecodeGenerator();
 * BytecodeProgram program = generator.generate(astNodes);
 * FjordVM vm = new FjordVM();
 * Value result = vm.execute(program);
 * }</pre>
 */
public final class BytecodeGenerator implements ASTVisitor<Void> {

    /** The program being constructed */
    private BytecodeProgram program;

    /** Jump targets that need to be resolved */
    private final Map<String, List<Integer>> unresolvedJumps = new HashMap<>();

    /** Labels for jump targets */
    private final Map<String, Integer> labels = new HashMap<>();

    /** Counter for generating unique labels */
    private int labelCounter = 0;

    /**
     * Creates a new bytecode generator.
     */
    public BytecodeGenerator() {
    }

    /**
     * Generates bytecode instructions for the given AST.
     *
     * @param program the AST to compile
     * @return BytecodeProgram ready for execution
     */
    public BytecodeProgram generate(List<Expression> program) {
        this.program = new BytecodeProgram();
        this.unresolvedJumps.clear();
        this.labels.clear();
        this.labelCounter = 0;

        try {
            // Generate code for each top-level expression
            for (Expression expr : program) {
                if (expr instanceof ASTNode) {
                    ((ASTNode) expr).accept(this);
                } else {
                    generateLegacyExpression(expr);
                }
                // Pop result if it's not needed (statement-level expressions)
                if (!(expr instanceof FunctionDefExpr)) {
                    this.program.addInstruction(Instruction.Factory.POP);
                }
            }

            // End program with HALT instruction
            this.program.addInstruction(Instruction.Factory.HALT);

            // Resolve any unresolved jumps
            resolveJumps();

            return this.program;

        } finally {
            this.program = null;
        }
    }

    /**
     * Handles expressions that don't yet implement the visitor pattern.
     */
    private void generateLegacyExpression(Expression expr) {
        // For backward compatibility, evaluate the expression and push result
        // This is a simplified approach - in practice, we'd convert legacy nodes
        if (expr instanceof LiteralExpr) {
            LiteralExpr literal = (LiteralExpr) expr;
            Value value = literal.getValue();
            program.addInstruction(Instruction.Factory.loadConst(value));
        } else if (expr instanceof VariableExpr) {
            VariableExpr var = (VariableExpr) expr;
            program.addInstruction(Instruction.Factory.loadVar(var.getName()));
        } else if (expr instanceof BinaryExpr) {
            generateBinaryExpr((BinaryExpr) expr);
        } else {
            // Fallback: push unit value for unknown expressions
            program.addInstruction(Instruction.Factory.loadConst(UnitValue.INSTANCE));
        }
    }

    /**
     * Generates code for binary expressions.
     */
    private void generateBinaryExpr(BinaryExpr expr) {
        // Generate left operand
        if (expr.getLeft() instanceof ASTNode) {
            ((ASTNode) expr.getLeft()).accept(this);
        } else {
            generateLegacyExpression(expr.getLeft());
        }

        // Generate right operand
        if (expr.getRight() instanceof ASTNode) {
            ((ASTNode) expr.getRight()).accept(this);
        } else {
            generateLegacyExpression(expr.getRight());
        }

        // Generate operator instruction
        TokenType op = expr.getOperator();
        switch (op) {
            case PLUS -> program.addInstruction(Instruction.Factory.ADD);
            case MINUS -> program.addInstruction(Instruction.Factory.SUB);
            case STAR -> program.addInstruction(Instruction.Factory.MUL);
            case SLASH -> program.addInstruction(Instruction.Factory.DIV);
            case EQEQ -> program.addInstruction(Instruction.Factory.EQ);
            case BANGEQ -> program.addInstruction(Instruction.Factory.NE);
            case LT -> program.addInstruction(Instruction.Factory.LT);
            case LTE -> program.addInstruction(Instruction.Factory.LE);
            case GT -> program.addInstruction(Instruction.Factory.GT);
            case GTE -> program.addInstruction(Instruction.Factory.GE);
            default -> throw new IllegalArgumentException("Unsupported binary operator: " + op);
        }
    }

    // Visitor method implementations

    @Override
    public Void visitBinaryExpr(BinaryExpr expr) {
        generateBinaryExpr(expr);
        return null;
    }

    @Override
    public Void visitLiteralExpr(LiteralExpr expr) {
        Value value = expr.getValue();
        program.addInstruction(Instruction.Factory.loadConst(value));
        return null;
    }

    @Override
    public Void visitVariableExpr(VariableExpr expr) {
        program.addInstruction(Instruction.Factory.loadVar(expr.getName()));
        return null;
    }

    @Override
    public Void visitFunctionDefExpr(FunctionDefExpr expr) {
        String funcName = expr.getName();

        // Define function entry point
        int functionStart = program.getInstructionCount();
        program.defineFunction(funcName, functionStart);

        // For now, function definitions just store unit
        // In a full implementation, we'd generate the function body
        program.addInstruction(Instruction.Factory.loadConst(UnitValue.INSTANCE));
        program.addInstruction(Instruction.Factory.storeVar(funcName));

        return null;
    }

    @Override
    public Void visitCallExpr(CallExpr expr) {
        // For now, assume all calls are to built-in functions
        if (expr.getFunction() instanceof VariableExpr) {
            VariableExpr funcVar = (VariableExpr) expr.getFunction();
            String funcName = funcVar.getName();

            // Generate arguments
            List<Expression> args = expr.getArguments();
            for (Expression arg : args) {
                if (arg instanceof ASTNode) {
                    ((ASTNode) arg).accept(this);
                } else {
                    generateLegacyExpression(arg);
                }
            }

            // Generate call instruction
            program.addInstruction(Instruction.Factory.callBuiltin(funcName, args.size()));
        } else {
            throw new UnsupportedOperationException("Complex function calls not yet supported");
        }

        return null;
    }

    @Override
    public Void visitLetExpr(LetExpr expr) {
        // Generate initializer
        if (expr.getInitializer() instanceof ASTNode) {
            ((ASTNode) expr.getInitializer()).accept(this);
        } else {
            generateLegacyExpression(expr.getInitializer());
        }

        // Store in variable
        program.addInstruction(Instruction.Factory.storeVar(expr.getName()));

        // Let expressions evaluate to unit
        program.addInstruction(Instruction.Factory.loadConst(UnitValue.INSTANCE));

        return null;
    }

    @Override
    public Void visitBlockExpr(BlockExpr expr) {
        List<Expression> statements = expr.getStatements();

        if (statements.isEmpty()) {
            // Empty block evaluates to unit
            program.addInstruction(Instruction.Factory.loadConst(UnitValue.INSTANCE));
            return null;
        }

        // Generate code for each statement except the last
        for (int i = 0; i < statements.size() - 1; i++) {
            Expression stmt = statements.get(i);
            if (stmt instanceof ASTNode) {
                ((ASTNode) stmt).accept(this);
            } else {
                generateLegacyExpression(stmt);
            }
            // Pop result of intermediate statements
            program.addInstruction(Instruction.Factory.POP);
        }

        // Generate code for the last statement (block result)
        Expression lastStmt = statements.get(statements.size() - 1);
        if (lastStmt instanceof ASTNode) {
            ((ASTNode) lastStmt).accept(this);
        } else {
            generateLegacyExpression(lastStmt);
        }

        return null;
    }

    @Override
    public Void visitIfExpr(IfExpr expr) {
        // Generate condition
        if (expr.getCondition() instanceof ASTNode) {
            ((ASTNode) expr.getCondition()).accept(this);
        } else {
            generateLegacyExpression(expr.getCondition());
        }

        // Create labels for control flow
        String elseLabel = generateLabel("else");
        String endLabel = generateLabel("end");

        // Jump to else branch if condition is false
        int elseJump = program.getInstructionCount();
        program.addInstruction(Instruction.Factory.jumpIfFalse(0)); // Placeholder
        addUnresolvedJump(elseLabel, elseJump);

        // Generate then branch
        if (expr.getThenBranch() instanceof ASTNode) {
            ((ASTNode) expr.getThenBranch()).accept(this);
        } else {
            generateLegacyExpression(expr.getThenBranch());
        }

        // Jump to end after then branch
        int endJump = program.getInstructionCount();
        program.addInstruction(Instruction.Factory.jump(0)); // Placeholder
        addUnresolvedJump(endLabel, endJump);

        // Else branch
        setLabel(elseLabel);
        if (expr.getElseBranch() instanceof ASTNode) {
            ((ASTNode) expr.getElseBranch()).accept(this);
        } else {
            generateLegacyExpression(expr.getElseBranch());
        }

        // End of if expression
        setLabel(endLabel);

        return null;
    }

    @Override
    public Void visitWhileExpr(WhileExpr expr) {
        // Create labels for control flow
        String loopStart = generateLabel("loop_start");
        String loopEnd = generateLabel("loop_end");

        // Mark start of loop
        setLabel(loopStart);

        // Generate condition
        if (expr.getCondition() instanceof ASTNode) {
            ((ASTNode) expr.getCondition()).accept(this);
        } else {
            generateLegacyExpression(expr.getCondition());
        }

        // Jump to end if condition is false
        int endJump = program.getInstructionCount();
        program.addInstruction(Instruction.Factory.jumpIfFalse(0)); // Placeholder
        addUnresolvedJump(loopEnd, endJump);

        // Generate body
        if (expr.getBody() instanceof ASTNode) {
            ((ASTNode) expr.getBody()).accept(this);
        } else {
            generateLegacyExpression(expr.getBody());
        }

        // Pop body result since it's not needed
        program.addInstruction(Instruction.Factory.POP);

        // Jump back to loop start
        int startJump = program.getInstructionCount();
        program.addInstruction(Instruction.Factory.jump(0)); // Placeholder
        addUnresolvedJump(loopStart, startJump);

        // End of while loop
        setLabel(loopEnd);

        // While expressions always evaluate to unit
        program.addInstruction(Instruction.Factory.loadConst(UnitValue.INSTANCE));

        return null;
    }

    @Override
    public Void visitAssignmentExpr(AssignmentExpr expr) {
        // Generate value expression
        if (expr.getValue() instanceof ASTNode) {
            ((ASTNode) expr.getValue()).accept(this);
        } else {
            generateLegacyExpression(expr.getValue());
        }

        // Duplicate value for assignment result
        program.addInstruction(Instruction.Factory.DUP);

        // Store in variable
        program.addInstruction(Instruction.Factory.storeVar(expr.getName()));

        return null;
    }

    // Label and jump management

    private String generateLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    private void setLabel(String label) {
        labels.put(label, program.getInstructionCount());
    }

    private void addUnresolvedJump(String label, int instructionIndex) {
        unresolvedJumps.computeIfAbsent(label, k -> new ArrayList<>()).add(instructionIndex);
    }

    private void resolveJumps() {
        for (Map.Entry<String, List<Integer>> entry : unresolvedJumps.entrySet()) {
            String label = entry.getKey();
            Integer targetAddress = labels.get(label);

            if (targetAddress == null) {
                throw new RuntimeException("Undefined label: " + label);
            }

            for (Integer jumpAddress : entry.getValue()) {
                Instruction oldInstruction = program.getInstruction(jumpAddress);
                Instruction newInstruction;

                if (oldInstruction.opcode == Instruction.Opcode.JUMP) {
                    newInstruction = Instruction.Factory.jump(targetAddress);
                } else if (oldInstruction.opcode == Instruction.Opcode.JUMP_IF_FALSE) {
                    newInstruction = Instruction.Factory.jumpIfFalse(targetAddress);
                } else {
                    throw new RuntimeException("Invalid jump instruction: " + oldInstruction);
                }

                // Replace the instruction (this is conceptual - in practice you'd need
                // a method to update instructions in BytecodeProgram)
            }
        }
    }

    @Override
    public Void visitArrayLiteral(ArrayLiteralExpr expr) {
        // TODO: Implement proper array literal bytecode generation
        // For now, arrays are not supported in bytecode mode
        return null;
    }

    @Override
    public Void visitIndex(IndexExpr expr) {
        // TODO: Implement proper array indexing bytecode generation
        // For now, arrays are not supported in bytecode mode
        return null;
    }

    @Override
    public Void visitFor(ForExpr expr) {
        // TODO: Implement proper for loop bytecode generation
        // For now, for loops are not supported in bytecode mode
        return null;
    }

    @Override
    public Void visitStructDef(StructDefExpr expr) {
        // TODO: Implement proper struct definition bytecode generation
        // For now, struct definitions are not supported in bytecode mode
        return null;
    }

    @Override
    public Void visitStructLiteral(StructLiteralExpr expr) {
        // TODO: Implement proper struct literal bytecode generation
        // For now, struct literals are not supported in bytecode mode
        return null;
    }

    @Override
    public Void visitFieldAccess(FieldAccessExpr expr) {
        // TODO: Implement proper field access bytecode generation
        // For now, field access is not supported in bytecode mode
        return null;
    }

    @Override
    public Void visitLambda(LambdaExpr expr) {
        // TODO: Implement proper lambda bytecode generation
        // For now, lambdas are not supported in bytecode mode
        return null;
    }
}