package com.fjord.ide.debug;

import java.io.File;
import java.util.Objects;

public class Breakpoint {
    public enum BreakpointType {
        LINE,
        CONDITIONAL,
        EXCEPTION
    }

    private final File file;
    private final int line;
    private final BreakpointType type;
    private final String condition; // For conditional breakpoints
    private boolean enabled;
    private int hitCount;
    private String logMessage; // For tracepoints

    public Breakpoint(File file, int line) {
        this(file, line, BreakpointType.LINE, null);
    }

    public Breakpoint(File file, int line, BreakpointType type, String condition) {
        this.file = file;
        this.line = line;
        this.type = type;
        this.condition = condition;
        this.enabled = true;
        this.hitCount = 0;
    }

    public File getFile() { return file; }
    public int getLine() { return line; }
    public BreakpointType getType() { return type; }
    public String getCondition() { return condition; }
    public boolean isEnabled() { return enabled; }
    public int getHitCount() { return hitCount; }
    public String getLogMessage() { return logMessage; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setLogMessage(String logMessage) { this.logMessage = logMessage; }

    public void hit() {
        hitCount++;
    }

    public boolean shouldBreak() {
        if (!enabled) return false;

        hit();

        if (type == BreakpointType.LINE) {
            return true;
        } else if (type == BreakpointType.CONDITIONAL && condition != null) {
            // TODO: Evaluate condition in current context
            return evaluateCondition();
        }

        return false;
    }

    private boolean evaluateCondition() {
        // Placeholder for condition evaluation
        // This would need to evaluate the condition in the current execution context
        return true;
    }

    public String getDisplayName() {
        String fileName = file != null ? file.getName() : "Unknown";
        StringBuilder display = new StringBuilder();
        display.append(fileName).append(":").append(line);

        if (type == BreakpointType.CONDITIONAL && condition != null) {
            display.append(" [").append(condition).append("]");
        }

        if (hitCount > 0) {
            display.append(" (hit ").append(hitCount).append(" times)");
        }

        return display.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Breakpoint that = (Breakpoint) obj;
        return line == that.line &&
               Objects.equals(file, that.file) &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, line, type);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}