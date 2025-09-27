package com.fjord.ide.debug;

import com.fjord.runtime.Environment;
import com.fjord.runtime.Value;
import com.fjord.ast.Expression;
import com.fjord.error.SourcePosition;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DebugSession {
    public enum DebugState {
        NOT_STARTED,
        RUNNING,
        PAUSED,
        STOPPED,
        STEP_INTO,
        STEP_OVER,
        STEP_OUT
    }

    private final List<Breakpoint> breakpoints = new CopyOnWriteArrayList<>();
    private final List<StackFrame> callStack = new ArrayList<>();
    private final Map<String, Value> variables = new HashMap<>();
    private final List<DebugEventListener> listeners = new CopyOnWriteArrayList<>();

    private DebugState state = DebugState.NOT_STARTED;
    private File currentFile;
    private int currentLine;
    private Expression currentExpression;
    private Environment currentEnvironment;

    // Step control
    private int stepDepth = 0;
    private boolean stepOver = false;
    private boolean stepOut = false;

    public interface DebugEventListener {
        void onStateChanged(DebugState oldState, DebugState newState);
        void onBreakpointHit(Breakpoint breakpoint, StackFrame frame);
        void onStepComplete(StackFrame frame);
        void onVariablesUpdated(Map<String, Value> variables);
        void onCallStackUpdated(List<StackFrame> callStack);
    }

    public void addListener(DebugEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DebugEventListener listener) {
        listeners.remove(listener);
    }

    // Breakpoint management
    public void addBreakpoint(Breakpoint breakpoint) {
        if (!breakpoints.contains(breakpoint)) {
            breakpoints.add(breakpoint);
        }
    }

    public void removeBreakpoint(Breakpoint breakpoint) {
        breakpoints.remove(breakpoint);
    }

    public void removeBreakpoint(File file, int line) {
        breakpoints.removeIf(bp -> bp.getFile().equals(file) && bp.getLine() == line);
    }

    public void toggleBreakpoint(File file, int line) {
        Breakpoint existing = findBreakpoint(file, line);
        if (existing != null) {
            removeBreakpoint(existing);
        } else {
            addBreakpoint(new Breakpoint(file, line));
        }
    }

    public Breakpoint findBreakpoint(File file, int line) {
        return breakpoints.stream()
                .filter(bp -> bp.getFile().equals(file) && bp.getLine() == line)
                .findFirst()
                .orElse(null);
    }

    public List<Breakpoint> getBreakpoints() {
        return new ArrayList<>(breakpoints);
    }

    public List<Breakpoint> getBreakpoints(File file) {
        return breakpoints.stream()
                .filter(bp -> bp.getFile().equals(file))
                .toList();
    }

    // Debug control
    public void start() {
        setState(DebugState.RUNNING);
        callStack.clear();
        variables.clear();
    }

    public void pause() {
        setState(DebugState.PAUSED);
    }

    public void resume() {
        setState(DebugState.RUNNING);
    }

    public void stop() {
        setState(DebugState.STOPPED);
        callStack.clear();
        variables.clear();
        currentFile = null;
        currentLine = 0;
    }

    public void stepInto() {
        setState(DebugState.STEP_INTO);
        stepDepth = callStack.size();
    }

    public void stepOver() {
        setState(DebugState.STEP_OVER);
        stepDepth = callStack.size();
        stepOver = true;
    }

    public void stepOut() {
        setState(DebugState.STEP_OUT);
        stepDepth = callStack.size();
        stepOut = true;
    }

    // Debug information updates
    public void updateLocation(File file, int line, Expression expression, Environment environment) {
        this.currentFile = file;
        this.currentLine = line;
        this.currentExpression = expression;
        this.currentEnvironment = environment;

        // Check for breakpoints
        if (state == DebugState.RUNNING) {
            Breakpoint breakpoint = findBreakpoint(file, line);
            if (breakpoint != null && breakpoint.shouldBreak()) {
                pause();
                notifyBreakpointHit(breakpoint);
                return;
            }
        }

        // Handle step operations
        handleStepOperation();

        // Update variables and call stack
        updateVariables();
        notifyCallStackUpdated();
    }

    private void handleStepOperation() {
        boolean shouldPause = false;

        switch (state) {
            case STEP_INTO -> {
                shouldPause = true;
                setState(DebugState.PAUSED);
            }
            case STEP_OVER -> {
                if (callStack.size() <= stepDepth) {
                    shouldPause = true;
                    setState(DebugState.PAUSED);
                    stepOver = false;
                }
            }
            case STEP_OUT -> {
                if (callStack.size() < stepDepth) {
                    shouldPause = true;
                    setState(DebugState.PAUSED);
                    stepOut = false;
                }
            }
        }

        if (shouldPause) {
            notifyStepComplete();
        }
    }

    public void enterFunction(String functionName, List<String> parameters, List<Value> arguments) {
        SourcePosition position = new SourcePosition(currentLine, 1, 0);
        StackFrame frame = new StackFrame(functionName, currentFile, position, parameters, arguments);
        callStack.add(frame);
        notifyCallStackUpdated();
    }

    public void exitFunction() {
        if (!callStack.isEmpty()) {
            callStack.remove(callStack.size() - 1);
            notifyCallStackUpdated();
        }
    }

    private void updateVariables() {
        variables.clear();
        if (currentEnvironment != null) {
            // Extract variables from current environment
            // This would need to be implemented based on Environment structure
            extractVariablesFromEnvironment(currentEnvironment);
        }
        notifyVariablesUpdated();
    }

    private void extractVariablesFromEnvironment(Environment env) {
        // TODO: Implement variable extraction from Environment
        // This depends on the internal structure of Environment class
    }

    // Evaluation
    public Optional<Value> evaluateExpression(String expression) {
        if (currentEnvironment == null) {
            return Optional.empty();
        }

        try {
            // TODO: Parse and evaluate the expression in current context
            // This would need a mini-parser for debug expressions
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // State management
    private void setState(DebugState newState) {
        DebugState oldState = this.state;
        this.state = newState;
        notifyStateChanged(oldState, newState);
    }

    // Notification methods
    private void notifyStateChanged(DebugState oldState, DebugState newState) {
        for (DebugEventListener listener : listeners) {
            try {
                listener.onStateChanged(oldState, newState);
            } catch (Exception e) {
                // Handle listener errors
            }
        }
    }

    private void notifyBreakpointHit(Breakpoint breakpoint) {
        StackFrame currentFrame = getCurrentStackFrame();
        for (DebugEventListener listener : listeners) {
            try {
                listener.onBreakpointHit(breakpoint, currentFrame);
            } catch (Exception e) {
                // Handle listener errors
            }
        }
    }

    private void notifyStepComplete() {
        StackFrame currentFrame = getCurrentStackFrame();
        for (DebugEventListener listener : listeners) {
            try {
                listener.onStepComplete(currentFrame);
            } catch (Exception e) {
                // Handle listener errors
            }
        }
    }

    private void notifyVariablesUpdated() {
        for (DebugEventListener listener : listeners) {
            try {
                listener.onVariablesUpdated(new HashMap<>(variables));
            } catch (Exception e) {
                // Handle listener errors
            }
        }
    }

    private void notifyCallStackUpdated() {
        for (DebugEventListener listener : listeners) {
            try {
                listener.onCallStackUpdated(new ArrayList<>(callStack));
            } catch (Exception e) {
                // Handle listener errors
            }
        }
    }

    // Getters
    public DebugState getState() { return state; }
    public File getCurrentFile() { return currentFile; }
    public int getCurrentLine() { return currentLine; }
    public List<StackFrame> getCallStack() { return new ArrayList<>(callStack); }
    public Map<String, Value> getVariables() { return new HashMap<>(variables); }

    public StackFrame getCurrentStackFrame() {
        if (callStack.isEmpty()) {
            return new StackFrame("main", currentFile,
                new SourcePosition(currentLine, 1, 0), List.of(), List.of());
        }
        return callStack.get(callStack.size() - 1);
    }

    public boolean isRunning() {
        return state == DebugState.RUNNING ||
               state == DebugState.STEP_INTO ||
               state == DebugState.STEP_OVER ||
               state == DebugState.STEP_OUT;
    }

    public boolean isPaused() {
        return state == DebugState.PAUSED;
    }

    public boolean isStopped() {
        return state == DebugState.STOPPED || state == DebugState.NOT_STARTED;
    }
}