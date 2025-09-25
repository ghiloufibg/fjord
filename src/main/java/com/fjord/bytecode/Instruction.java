package com.fjord.bytecode;

/**
 * Represents a single bytecode instruction in the Fjord virtual machine.
 * Instructions are the basic operations that the VM can execute, forming
 * a stack-based intermediate representation of Fjord programs.
 *
 * <p>The Fjord VM uses a simple stack-based architecture with the following
 * instruction categories:
 * <ul>
 *   <li><strong>Stack operations:</strong> LOAD_CONST, LOAD_VAR, STORE_VAR, POP, DUP</li>
 *   <li><strong>Arithmetic:</strong> ADD, SUB, MUL, DIV, NEG</li>
 *   <li><strong>Comparison:</strong> EQ, NE, LT, LE, GT, GE</li>
 *   <li><strong>Control flow:</strong> JUMP, JUMP_IF_FALSE, CALL, RETURN</li>
 *   <li><strong>Functions:</strong> DEFINE_FUNC, CALL_BUILTIN</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Load constant 42 onto stack
 * Instruction loadConst = new Instruction(Opcode.LOAD_CONST, 42);
 *
 * // Add two values on stack
 * Instruction add = new Instruction(Opcode.ADD);
 * }</pre>
 */
public final class Instruction {

    /**
     * Bytecode operation codes for the Fjord virtual machine.
     */
    public enum Opcode {
        // Stack operations
        /** Load constant value onto stack: LOAD_CONST <value> */
        LOAD_CONST,
        /** Load variable value onto stack: LOAD_VAR <name> */
        LOAD_VAR,
        /** Store top stack value to variable: STORE_VAR <name> */
        STORE_VAR,
        /** Pop top value from stack: POP */
        POP,
        /** Duplicate top stack value: DUP */
        DUP,

        // Arithmetic operations
        /** Add two values: ADD */
        ADD,
        /** Subtract two values: SUB */
        SUB,
        /** Multiply two values: MUL */
        MUL,
        /** Divide two values: DIV */
        DIV,
        /** Negate top value: NEG */
        NEG,

        // Comparison operations
        /** Equal comparison: EQ */
        EQ,
        /** Not equal comparison: NE */
        NE,
        /** Less than comparison: LT */
        LT,
        /** Less than or equal: LE */
        LE,
        /** Greater than comparison: GT */
        GT,
        /** Greater than or equal: GE */
        GE,

        // Control flow
        /** Unconditional jump: JUMP <offset> */
        JUMP,
        /** Jump if top value is false: JUMP_IF_FALSE <offset> */
        JUMP_IF_FALSE,
        /** Call function: CALL <argCount> */
        CALL,
        /** Return from function: RETURN */
        RETURN,

        // Function operations
        /** Define function: DEFINE_FUNC <name> <paramCount> <bodyStart> */
        DEFINE_FUNC,
        /** Call built-in function: CALL_BUILTIN <name> <argCount> */
        CALL_BUILTIN,

        // Program control
        /** Halt program execution: HALT */
        HALT
    }

    /** The operation code for this instruction */
    public final Opcode opcode;

    /** Optional operand for this instruction (null if not needed) */
    public final Object operand;

    /**
     * Creates an instruction without an operand.
     *
     * @param opcode the operation code
     */
    public Instruction(Opcode opcode) {
        this.opcode = opcode;
        this.operand = null;
    }

    /**
     * Creates an instruction with an operand.
     *
     * @param opcode  the operation code
     * @param operand the operand value
     */
    public Instruction(Opcode opcode, Object operand) {
        this.opcode = opcode;
        this.operand = operand;
    }

    /**
     * Returns the operation code.
     *
     * @return the opcode
     */
    public Opcode getOpcode() {
        return opcode;
    }

    /**
     * Returns the operand, if any.
     *
     * @return the operand, or null if none
     */
    public Object getOperand() {
        return operand;
    }

    /**
     * Returns whether this instruction has an operand.
     *
     * @return true if operand is present
     */
    public boolean hasOperand() {
        return operand != null;
    }

    /**
     * Returns the operand as an integer.
     *
     * @return integer operand
     * @throws IllegalStateException if operand is not an integer
     */
    public int getIntOperand() {
        if (!(operand instanceof Integer)) {
            throw new IllegalStateException("Operand is not an integer: " + operand);
        }
        return (Integer) operand;
    }

    /**
     * Returns the operand as a string.
     *
     * @return string operand
     * @throws IllegalStateException if operand is not a string
     */
    public String getStringOperand() {
        if (!(operand instanceof String)) {
            throw new IllegalStateException("Operand is not a string: " + operand);
        }
        return (String) operand;
    }

    @Override
    public String toString() {
        if (operand != null) {
            return String.format("%-15s %s", opcode, operand);
        } else {
            return opcode.toString();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Instruction that = (Instruction) obj;
        return opcode == that.opcode &&
               java.util.Objects.equals(operand, that.operand);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(opcode, operand);
    }

    /**
     * Factory methods for common instructions.
     */
    public static final class Factory {
        private Factory() {} // Utility class

        public static Instruction loadConst(Object value) {
            return new Instruction(Opcode.LOAD_CONST, value);
        }

        public static Instruction loadVar(String name) {
            return new Instruction(Opcode.LOAD_VAR, name);
        }

        public static Instruction storeVar(String name) {
            return new Instruction(Opcode.STORE_VAR, name);
        }

        public static Instruction jump(int offset) {
            return new Instruction(Opcode.JUMP, offset);
        }

        public static Instruction jumpIfFalse(int offset) {
            return new Instruction(Opcode.JUMP_IF_FALSE, offset);
        }

        public static Instruction call(int argCount) {
            return new Instruction(Opcode.CALL, argCount);
        }

        public static Instruction callBuiltin(String name, int argCount) {
            return new Instruction(Opcode.CALL_BUILTIN, name + ":" + argCount);
        }

        public static Instruction defineFunc(String name, int paramCount, int bodyStart) {
            return new Instruction(Opcode.DEFINE_FUNC, name + ":" + paramCount + ":" + bodyStart);
        }

        // Simple instructions without operands
        public static final Instruction ADD = new Instruction(Opcode.ADD);
        public static final Instruction SUB = new Instruction(Opcode.SUB);
        public static final Instruction MUL = new Instruction(Opcode.MUL);
        public static final Instruction DIV = new Instruction(Opcode.DIV);
        public static final Instruction NEG = new Instruction(Opcode.NEG);
        public static final Instruction EQ = new Instruction(Opcode.EQ);
        public static final Instruction NE = new Instruction(Opcode.NE);
        public static final Instruction LT = new Instruction(Opcode.LT);
        public static final Instruction LE = new Instruction(Opcode.LE);
        public static final Instruction GT = new Instruction(Opcode.GT);
        public static final Instruction GE = new Instruction(Opcode.GE);
        public static final Instruction POP = new Instruction(Opcode.POP);
        public static final Instruction DUP = new Instruction(Opcode.DUP);
        public static final Instruction RETURN = new Instruction(Opcode.RETURN);
        public static final Instruction HALT = new Instruction(Opcode.HALT);
    }
}