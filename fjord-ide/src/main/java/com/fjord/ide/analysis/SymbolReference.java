package com.fjord.ide.analysis;

import com.fjord.error.SourcePosition;

import java.io.File;
import java.util.Objects;

public class SymbolReference {
    public enum ReferenceType {
        DEFINITION,
        USAGE,
        ASSIGNMENT,
        CALL
    }

    private final Symbol symbol;
    private final SourcePosition position;
    private final File file;
    private final ReferenceType type;
    private final String context; // Surrounding code context

    public SymbolReference(Symbol symbol, SourcePosition position, File file,
                          ReferenceType type, String context) {
        this.symbol = symbol;
        this.position = position;
        this.file = file;
        this.type = type;
        this.context = context;
    }

    public Symbol getSymbol() { return symbol; }
    public SourcePosition getPosition() { return position; }
    public File getFile() { return file; }
    public ReferenceType getType() { return type; }
    public String getContext() { return context; }

    public boolean isDefinition() {
        return type == ReferenceType.DEFINITION;
    }

    public String getDisplayText() {
        String fileName = file != null ? file.getName() : "Unknown";
        return String.format("%s:%d:%d - %s", fileName, position.line, position.column,
                           context != null ? context.trim() : symbol.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SymbolReference that = (SymbolReference) obj;
        return Objects.equals(symbol, that.symbol) &&
               Objects.equals(position, that.position) &&
               Objects.equals(file, that.file) &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, position, file, type);
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}