package com.fjord.ide.navigation;

import java.io.File;
import java.util.Objects;

public class NavigationTarget {
    public enum TargetType {
        DEFINITION,
        REFERENCE,
        DECLARATION
    }

    private final File file;
    private final int line;
    private final int column;
    private final String context;
    private final TargetType type;

    public NavigationTarget(File file, int line, int column, String context, TargetType type) {
        this.file = file;
        this.line = line;
        this.column = column;
        this.context = context;
        this.type = type;
    }

    public File getFile() { return file; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getContext() { return context; }
    public TargetType getType() { return type; }

    public String getDisplayName() {
        String fileName = file != null ? file.getName() : "Unknown";
        return String.format("%s:%d:%d", fileName, line, column);
    }

    public String getFullDisplayName() {
        StringBuilder display = new StringBuilder(getDisplayName());
        if (context != null && !context.trim().isEmpty()) {
            display.append(" - ").append(context.trim());
        }
        return display.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NavigationTarget that = (NavigationTarget) obj;
        return line == that.line &&
               column == that.column &&
               Objects.equals(file, that.file) &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, line, column, type);
    }

    @Override
    public String toString() {
        return getFullDisplayName();
    }
}