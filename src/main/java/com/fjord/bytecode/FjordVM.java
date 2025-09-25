package com.fjord.bytecode;

import com.fjord.runtime.*;
import com.fjord.runtime.builtin.BuiltinRegistry;
import com.fjord.error.SourcePosition;

import java.util.*;

/**
 * Virtual machine for executing Fjord bytecode.
 * Provides better performance than tree-walking interpretation through
 * a stack-based execution model with optimized instruction dispatch.
 *
 * <p>The Fjord VM features:
 * <ul>
 *   <li><strong>Stack-based execution:</strong> Efficient operand management</li>
 *   <li><strong>Call stack:</strong> Function call and return handling</li>
 *   <li><strong>Variable storage:</strong> Fast local and global variable access</li>
 *   <li><strong>Built-in functions:</strong> Native function call support</li>
 *   <li><strong>Error handling:</strong> Runtime error reporting with source positions</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * BytecodeProgram program = // ... compiled program
 * FjordVM vm = new FjordVM();
 * Value result = vm.execute(program);
 * }</pre>
 */
public final class FjordVM {

    /**
     * Represents a call frame on the call stack.
     */
    private static final class CallFrame {
        /** Instruction pointer for this frame */
        public int instructionPointer;
        /** Local variables for this frame */
        public final Map<String, Value> locals = new HashMap<>();
        /** Function name (for debugging) */
        public final String functionName;

        public CallFrame(String functionName, int startingIP) {
            this.functionName = functionName;
            this.instructionPointer = startingIP;
        }

        @Override
        public String toString() {
            return String.format("CallFrame{function='%s', ip=%d, locals=%d}",
                                 functionName, instructionPointer, locals.size());
        }
    }

    /** Maximum recursion depth to prevent stack overflow */
    private static final int MAX_CALL_STACK_DEPTH = 1000;

    /** Maximum operand stack size to prevent memory issues */
    private static final int MAX_STACK_SIZE = 10000;

    /** Operand stack for expression evaluation */
    private final Deque<Value> operandStack = new ArrayDeque<>();

    /** Call stack for function calls */
    private final Deque<CallFrame> callStack = new ArrayDeque<>();

    /** Global variables */
    private final Map<String, Value> globals = new HashMap<>();

    /** Built-in function registry */
    private final BuiltinRegistry builtins;

    /** Whether to enable debug tracing */
    private final boolean traceExecution;

    /** Current program being executed */
    private BytecodeProgram currentProgram;

    /**
     * Creates a new Fjord VM with default settings.
     */
    public FjordVM() {
        this(BuiltinRegistry.createDefault(), false);
    }

    /**
     * Creates a new Fjord VM with custom settings.
     *
     * @param builtins        built-in function registry
     * @param traceExecution  whether to enable execution tracing
     */
    public FjordVM(BuiltinRegistry builtins, boolean traceExecution) {
        this.builtins = builtins;
        this.traceExecution = traceExecution;
    }

    /**
     * Executes bytecode program with the given environment.
     * Sets up the VM state and begins instruction execution from the program start.
     *
     * @param program compiled bytecode to execute
     * @return final execution result
     * @throws RuntimeException if execution fails
     */
    public Value execute(BytecodeProgram program) {
        if (program == null) {
            throw new IllegalArgumentException("Program cannot be null");
        }

        // Validate program before execution
        List<String> validationErrors = program.validate();
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Invalid program: " + validationErrors);
        }

        this.currentProgram = program;

        // Reset VM state
        operandStack.clear();
        callStack.clear();
        globals.clear();

        // Setup built-in functions in globals
        setupBuiltinGlobals();

        try {
            // Create main call frame
            CallFrame mainFrame = new CallFrame("<main>", 0);
            callStack.push(mainFrame);

            // Execute instructions
            return executeInstructions();

        } catch (Exception e) {
            throw new RuntimeException("VM execution failed: " + e.getMessage(), e);
        } finally {
            this.currentProgram = null;
        }
    }

    /**
     * Sets up built-in functions as global variables.
     */
    private void setupBuiltinGlobals() {
        for (String name : builtins.getBuiltinNames()) {
            var builtin = builtins.getFunction(name);
            if (builtin.isPresent()) {
                // Create a simple callable wrapper for built-ins
                globals.put(name, builtin.get());
            }
        }
    }

    /**
     * Main instruction execution loop.
     */
    private Value executeInstructions() {
        while (!callStack.isEmpty()) {
            CallFrame frame = callStack.peek();

            if (frame.instructionPointer >= currentProgram.getInstructionCount()) {
                // End of program - return unit
                callStack.pop();
                if (operandStack.isEmpty()) {
                    return UnitValue.INSTANCE;
                } else {
                    return operandStack.pop();
                }
            }

            Instruction instruction = currentProgram.getInstruction(frame.instructionPointer);

            if (traceExecution) {
                System.out.printf("[VM] %s: %s (stack: %d)\n",
                                  frame.functionName, instruction, operandStack.size());
            }

            executeInstruction(instruction);
            frame.instructionPointer++;
        }

        // Return top of stack or unit if stack is empty
        return operandStack.isEmpty() ? UnitValue.INSTANCE : operandStack.pop();
    }

    /**
     * Executes a single instruction.
     */
    private void executeInstruction(Instruction instruction) {
        switch (instruction.opcode) {
            case LOAD_CONST -> {
                Object operand = instruction.operand;
                if (operand instanceof Value) {
                    push((Value) operand);
                } else {
                    // Convert Java objects to Values
                    push(convertToValue(operand));
                }
            }

            case LOAD_VAR -> {
                String varName = instruction.getStringOperand();
                Value value = lookupVariable(varName);
                if (value == null) {
                    throw new RuntimeException("Undefined variable: " + varName);
                }
                push(value);
            }

            case STORE_VAR -> {
                String varName = instruction.getStringOperand();
                Value value = pop();
                storeVariable(varName, value);
            }

            case POP -> pop();

            case DUP -> {
                Value value = peek();
                push(value);
            }

            case ADD -> {
                Value b = pop();
                Value a = pop();
                push(performAdd(a, b));
            }

            case SUB -> {
                Value b = pop();
                Value a = pop();
                push(performSubtract(a, b));
            }

            case MUL -> {
                Value b = pop();
                Value a = pop();
                push(performMultiply(a, b));
            }

            case DIV -> {
                Value b = pop();
                Value a = pop();
                push(performDivide(a, b));
            }

            case NEG -> {
                Value a = pop();
                push(performNegate(a));
            }

            case EQ -> {
                Value b = pop();
                Value a = pop();
                push(new BoolValue(valuesEqual(a, b)));
            }

            case NE -> {
                Value b = pop();
                Value a = pop();
                push(new BoolValue(!valuesEqual(a, b)));
            }

            case LT -> {
                Value b = pop();
                Value a = pop();
                push(new BoolValue(compareValues(a, b) < 0));
            }

            case LE -> {
                Value b = pop();
                Value a = pop();
                push(new BoolValue(compareValues(a, b) <= 0));
            }

            case GT -> {
                Value b = pop();
                Value a = pop();
                push(new BoolValue(compareValues(a, b) > 0));
            }

            case GE -> {
                Value b = pop();
                Value a = pop();
                push(new BoolValue(compareValues(a, b) >= 0));
            }

            case JUMP -> {
                int offset = instruction.getIntOperand();
                callStack.peek().instructionPointer = offset - 1; // -1 because it will be incremented
            }

            case JUMP_IF_FALSE -> {
                Value condition = pop();
                if (!isTruthy(condition)) {
                    int offset = instruction.getIntOperand();
                    callStack.peek().instructionPointer = offset - 1; // -1 because it will be incremented
                }
            }

            case CALL -> {
                int argCount = instruction.getIntOperand();
                executeCall(argCount);
            }

            case CALL_BUILTIN -> {
                String operandStr = instruction.getStringOperand();
                String[] parts = operandStr.split(":");
                String funcName = parts[0];
                int argCount = Integer.parseInt(parts[1]);
                executeBuiltinCall(funcName, argCount);
            }

            case RETURN -> {
                callStack.pop();
                // Return value should be on stack
            }

            case HALT -> {
                // Clear call stack to end execution
                callStack.clear();
            }

            default -> throw new RuntimeException("Unimplemented instruction: " + instruction.opcode);
        }
    }

    // Stack operations

    private void push(Value value) {
        if (operandStack.size() >= MAX_STACK_SIZE) {
            throw new RuntimeException("Stack overflow: maximum stack size exceeded");
        }
        operandStack.push(value);
    }

    private Value pop() {
        if (operandStack.isEmpty()) {
            throw new RuntimeException("Stack underflow: attempted to pop from empty stack");
        }
        return operandStack.pop();
    }

    private Value peek() {
        if (operandStack.isEmpty()) {
            throw new RuntimeException("Stack underflow: attempted to peek empty stack");
        }
        return operandStack.peek();
    }

    // Variable operations

    private Value lookupVariable(String name) {
        // Check locals first
        if (!callStack.isEmpty()) {
            Value local = callStack.peek().locals.get(name);
            if (local != null) {
                return local;
            }
        }

        // Check globals
        return globals.get(name);
    }

    private void storeVariable(String name, Value value) {
        // Store in current frame's locals
        if (!callStack.isEmpty()) {
            callStack.peek().locals.put(name, value);
        } else {
            globals.put(name, value);
        }
    }

    // Arithmetic operations

    private Value performAdd(Value a, Value b) {
        // String concatenation
        if (a instanceof StringValue || b instanceof StringValue) {
            return new StringValue(a.getValue().toString() + b.getValue().toString());
        }

        // Numeric addition
        if (a instanceof FloatValue || b instanceof FloatValue) {
            return new FloatValue(asDouble(a) + asDouble(b));
        }

        return new IntValue(asInt(a) + asInt(b));
    }

    private Value performSubtract(Value a, Value b) {
        if (a instanceof FloatValue || b instanceof FloatValue) {
            return new FloatValue(asDouble(a) - asDouble(b));
        }
        return new IntValue(asInt(a) - asInt(b));
    }

    private Value performMultiply(Value a, Value b) {
        if (a instanceof FloatValue || b instanceof FloatValue) {
            return new FloatValue(asDouble(a) * asDouble(b));
        }
        return new IntValue(asInt(a) * asInt(b));
    }

    private Value performDivide(Value a, Value b) {
        if (a instanceof FloatValue || b instanceof FloatValue) {
            double divisor = asDouble(b);
            if (divisor == 0.0) {
                throw new RuntimeException("Division by zero");
            }
            return new FloatValue(asDouble(a) / divisor);
        }

        int divisor = asInt(b);
        if (divisor == 0) {
            throw new RuntimeException("Division by zero");
        }
        return new IntValue(asInt(a) / divisor);
    }

    private Value performNegate(Value a) {
        if (a instanceof FloatValue) {
            return new FloatValue(-asDouble(a));
        }
        return new IntValue(-asInt(a));
    }

    // Comparison operations

    private boolean valuesEqual(Value a, Value b) {
        Object aVal = a.getValue();
        Object bVal = b.getValue();
        return Objects.equals(aVal, bVal);
    }

    private int compareValues(Value a, Value b) {
        if (a instanceof FloatValue || b instanceof FloatValue) {
            return Double.compare(asDouble(a), asDouble(b));
        }
        if (a instanceof StringValue || b instanceof StringValue) {
            return a.getValue().toString().compareTo(b.getValue().toString());
        }
        return Integer.compare(asInt(a), asInt(b));
    }

    private boolean isTruthy(Value value) {
        if (value instanceof BoolValue) {
            return ((BoolValue) value).asBoolean();
        }
        if (value instanceof UnitValue) {
            return false;
        }
        return true; // All other values are truthy
    }

    // Function calls

    private void executeCall(int argCount) {
        // For now, simplified function call handling
        // In a full implementation, this would handle user-defined functions
        throw new RuntimeException("User-defined function calls not yet implemented in VM");
    }

    private void executeBuiltinCall(String funcName, int argCount) {
        Optional<Callable> builtin = builtins.getFunction(funcName);
        if (builtin.isEmpty()) {
            throw new RuntimeException("Unknown built-in function: " + funcName);
        }

        // Collect arguments from stack
        List<Value> args = new ArrayList<>();
        for (int i = 0; i < argCount; i++) {
            args.add(0, pop()); // Add to front to maintain order
        }

        // Call the built-in function
        Value result = builtin.get().call(args);
        push(result);
    }

    // Type conversion utilities

    private Value convertToValue(Object obj) {
        if (obj instanceof Integer) {
            return new IntValue((Integer) obj);
        }
        if (obj instanceof Double) {
            return new FloatValue((Double) obj);
        }
        if (obj instanceof String) {
            return new StringValue((String) obj);
        }
        if (obj instanceof Boolean) {
            return new BoolValue((Boolean) obj);
        }
        throw new RuntimeException("Cannot convert to Value: " + obj.getClass().getSimpleName());
    }

    private int asInt(Value v) {
        if (v instanceof IntValue) {
            return ((IntValue) v).asInt();
        }
        if (v instanceof FloatValue) {
            return (int) ((FloatValue) v).asDouble();
        }
        if (v instanceof BoolValue) {
            return ((BoolValue) v).asBoolean() ? 1 : 0;
        }
        throw new RuntimeException("Cannot convert to int: " + v.getClass().getSimpleName());
    }

    private double asDouble(Value v) {
        if (v instanceof FloatValue) {
            return ((FloatValue) v).asDouble();
        }
        if (v instanceof IntValue) {
            return ((IntValue) v).asInt();
        }
        if (v instanceof BoolValue) {
            return ((BoolValue) v).asBoolean() ? 1.0 : 0.0;
        }
        throw new RuntimeException("Cannot convert to double: " + v.getClass().getSimpleName());
    }

    /**
     * Returns current VM state for debugging.
     */
    public String getVMState() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Fjord VM State ===\n");
        sb.append("Operand Stack (").append(operandStack.size()).append(" items):\n");
        int i = 0;
        for (Value value : operandStack) {
            sb.append("  ").append(i++).append(": ").append(value).append("\n");
        }
        sb.append("Call Stack (").append(callStack.size()).append(" frames):\n");
        i = 0;
        for (CallFrame frame : callStack) {
            sb.append("  ").append(i++).append(": ").append(frame).append("\n");
        }
        sb.append("Globals (").append(globals.size()).append(" variables):\n");
        for (Map.Entry<String, Value> entry : globals.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}