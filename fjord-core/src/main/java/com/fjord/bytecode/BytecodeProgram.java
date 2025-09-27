package com.fjord.bytecode;

import com.fjord.runtime.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a compiled Fjord program as a sequence of bytecode instructions.
 * Contains the instruction stream, constant pool, and metadata needed for
 * execution by the Fjord virtual machine.
 *
 * <p>A bytecode program consists of:
 * <ul>
 *   <li><strong>Instructions:</strong> Linear sequence of VM instructions</li>
 *   <li><strong>Constants:</strong> Pool of literal values used by the program</li>
 *   <li><strong>Functions:</strong> Entry points for user-defined functions</li>
 *   <li><strong>Metadata:</strong> Debug information and source mapping</li>
 * </ul>
 *
 * <p>Example structure:
 * <pre>{@code
 * BytecodeProgram program = new BytecodeProgram();
 * program.addInstruction(Instruction.Factory.loadConst(42));
 * program.addInstruction(Instruction.Factory.loadConst(24));
 * program.addInstruction(Instruction.Factory.ADD);
 * program.addInstruction(Instruction.Factory.HALT);
 * }</pre>
 */
public final class BytecodeProgram {

    /** Linear sequence of instructions */
    private final List<Instruction> instructions = new ArrayList<>();

    /** Constant pool for literal values */
    private final List<Value> constants = new ArrayList<>();

    /** Function entry points: name -> instruction index */
    private final Map<String, Integer> functions = new HashMap<>();

    /** Debug information: instruction index -> source position */
    private final Map<Integer, com.fjord.error.SourcePosition> debugInfo = new HashMap<>();

    /**
     * Creates a new empty bytecode program.
     */
    public BytecodeProgram() {
    }

    /**
     * Adds an instruction to the program.
     *
     * @param instruction the instruction to add
     * @return the index where the instruction was added
     */
    public int addInstruction(Instruction instruction) {
        int index = instructions.size();
        instructions.add(instruction);
        return index;
    }

    /**
     * Adds multiple instructions to the program.
     *
     * @param instructionList the instructions to add
     * @return the starting index of the added instructions
     */
    public int addInstructions(List<Instruction> instructionList) {
        int startIndex = instructions.size();
        instructions.addAll(instructionList);
        return startIndex;
    }

    /**
     * Adds a constant to the constant pool.
     *
     * @param value the constant value
     * @return the index of the constant in the pool
     */
    public int addConstant(Value value) {
        // Check if constant already exists to avoid duplicates
        int existingIndex = constants.indexOf(value);
        if (existingIndex != -1) {
            return existingIndex;
        }

        int index = constants.size();
        constants.add(value);
        return index;
    }

    /**
     * Defines a function entry point.
     *
     * @param name           the function name
     * @param startingIndex  the instruction index where function begins
     */
    public void defineFunction(String name, int startingIndex) {
        if (functions.containsKey(name)) {
            throw new IllegalArgumentException("Function '" + name + "' is already defined");
        }
        functions.put(name, startingIndex);
    }

    /**
     * Adds debug information for an instruction.
     *
     * @param instructionIndex the instruction index
     * @param position         the source position
     */
    public void addDebugInfo(int instructionIndex, com.fjord.error.SourcePosition position) {
        debugInfo.put(instructionIndex, position);
    }

    /**
     * Returns all instructions in the program.
     *
     * @return unmodifiable list of instructions
     */
    public List<Instruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    /**
     * Returns the instruction at the specified index.
     *
     * @param index the instruction index
     * @return the instruction
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Instruction getInstruction(int index) {
        return instructions.get(index);
    }

    /**
     * Returns the number of instructions in the program.
     *
     * @return instruction count
     */
    public int getInstructionCount() {
        return instructions.size();
    }

    /**
     * Returns all constants in the constant pool.
     *
     * @return unmodifiable list of constants
     */
    public List<Value> getConstants() {
        return Collections.unmodifiableList(constants);
    }

    /**
     * Returns the constant at the specified index.
     *
     * @param index the constant index
     * @return the constant value
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Value getConstant(int index) {
        return constants.get(index);
    }

    /**
     * Returns the number of constants in the pool.
     *
     * @return constant count
     */
    public int getConstantCount() {
        return constants.size();
    }

    /**
     * Returns all function definitions.
     *
     * @return unmodifiable map of function names to entry points
     */
    public Map<String, Integer> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    /**
     * Returns the entry point for a function.
     *
     * @param name the function name
     * @return the instruction index, or -1 if function not found
     */
    public int getFunctionEntryPoint(String name) {
        return functions.getOrDefault(name, -1);
    }

    /**
     * Returns whether a function is defined.
     *
     * @param name the function name
     * @return true if function is defined
     */
    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    /**
     * Returns debug information for an instruction.
     *
     * @param instructionIndex the instruction index
     * @return source position, or null if no debug info available
     */
    public com.fjord.error.SourcePosition getDebugInfo(int instructionIndex) {
        return debugInfo.get(instructionIndex);
    }

    /**
     * Returns whether the program is empty.
     *
     * @return true if no instructions are present
     */
    public boolean isEmpty() {
        return instructions.isEmpty();
    }

    /**
     * Validates the program for common issues.
     *
     * @return list of validation errors, empty if program is valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        // Check for empty program
        if (instructions.isEmpty()) {
            errors.add("Program contains no instructions");
            return errors;
        }

        // Check that program ends with HALT or RETURN
        Instruction lastInstruction = instructions.get(instructions.size() - 1);
        if (lastInstruction.opcode != Instruction.Opcode.HALT &&
            lastInstruction.opcode != Instruction.Opcode.RETURN) {
            errors.add("Program should end with HALT or RETURN instruction");
        }

        // Validate jump targets
        for (int i = 0; i < instructions.size(); i++) {
            Instruction inst = instructions.get(i);
            if (inst.opcode == Instruction.Opcode.JUMP ||
                inst.opcode == Instruction.Opcode.JUMP_IF_FALSE) {
                int target = inst.getIntOperand();
                if (target < 0 || target >= instructions.size()) {
                    errors.add("Invalid jump target at instruction " + i + ": " + target);
                }
            }
        }

        // Validate function entry points
        for (Map.Entry<String, Integer> entry : functions.entrySet()) {
            int entryPoint = entry.getValue();
            if (entryPoint < 0 || entryPoint >= instructions.size()) {
                errors.add("Invalid function entry point for '" + entry.getKey() + "': " + entryPoint);
            }
        }

        return errors;
    }

    /**
     * Returns a human-readable disassembly of the program.
     *
     * @return formatted program listing
     */
    public String disassemble() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Fjord Bytecode Program ===\n");
        sb.append("Instructions: ").append(instructions.size()).append("\n");
        sb.append("Constants: ").append(constants.size()).append("\n");
        sb.append("Functions: ").append(functions.size()).append("\n\n");

        // Constants section
        if (!constants.isEmpty()) {
            sb.append("Constants:\n");
            for (int i = 0; i < constants.size(); i++) {
                sb.append(String.format("  %3d: %s\n", i, constants.get(i)));
            }
            sb.append("\n");
        }

        // Functions section
        if (!functions.isEmpty()) {
            sb.append("Functions:\n");
            for (Map.Entry<String, Integer> entry : functions.entrySet()) {
                sb.append(String.format("  %-12s @ %d\n", entry.getKey(), entry.getValue()));
            }
            sb.append("\n");
        }

        // Instructions section
        sb.append("Instructions:\n");
        for (int i = 0; i < instructions.size(); i++) {
            String marker = "";
            // Mark function entry points
            for (Map.Entry<String, Integer> entry : functions.entrySet()) {
                if (entry.getValue() == i) {
                    marker = " <-- " + entry.getKey();
                    break;
                }
            }

            sb.append(String.format("  %3d: %-20s%s\n", i, instructions.get(i), marker));
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("BytecodeProgram{instructions=%d, constants=%d, functions=%d}",
                             instructions.size(), constants.size(), functions.size());
    }
}