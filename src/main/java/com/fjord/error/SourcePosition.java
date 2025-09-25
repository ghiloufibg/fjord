package com.fjord.error;

/**
 * Tracks source code positions for better error reporting.
 * Each token and AST node should maintain its source location to provide
 * precise error messages to users.
 *
 * <p>Example usage:
 * <pre>{@code
 * SourcePosition pos = new SourcePosition(10, 5, 234);
 * System.out.println("Error at line " + pos.line + ", column " + pos.column);
 * }</pre>
 */
public final class SourcePosition {

    /** Line number (1-based) where this position occurs */
    public final int line;

    /** Column number (1-based) within the line */
    public final int column;

    /** Absolute character offset from start of source (0-based) */
    public final int offset;

    /**
     * Creates a new source position.
     *
     * @param line   the line number (1-based)
     * @param column the column number (1-based)
     * @param offset the absolute character offset (0-based)
     */
    public SourcePosition(int line, int column, int offset) {
        this.line = line;
        this.column = column;
        this.offset = offset;
    }

    /**
     * Returns a human-readable representation of this position.
     *
     * @return formatted position string like "line 10, column 5"
     */
    @Override
    public String toString() {
        return String.format("line %d, column %d", line, column);
    }

    /**
     * Checks equality based on all position components.
     *
     * @param obj the object to compare with
     * @return true if positions are identical
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SourcePosition that = (SourcePosition) obj;
        return line == that.line && column == that.column && offset == that.offset;
    }

    /**
     * Returns hash code based on position components.
     *
     * @return computed hash code
     */
    @Override
    public int hashCode() {
        return 31 * (31 * line + column) + offset;
    }
}