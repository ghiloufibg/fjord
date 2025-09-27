package com.fjord.ide.analysis;

import com.fjord.error.SourcePosition;

import java.io.File;
import java.util.Objects;

public class Symbol {
    public enum SymbolType {
        FUNCTION,
        VARIABLE,
        PARAMETER,
        STRUCT,
        FIELD,
        LOCAL_VARIABLE
    }

    private final String name;
    private final SymbolType type;
    private final SourcePosition definition;
    private final File file;
    private final String signature; // For functions: return type and parameters
    private final String documentation;

    public Symbol(String name, SymbolType type, SourcePosition definition, File file,
                  String signature, String documentation) {
        this.name = name;
        this.type = type;
        this.definition = definition;
        this.file = file;
        this.signature = signature;
        this.documentation = documentation;
    }

    public Symbol(String name, SymbolType type, SourcePosition definition, File file) {
        this(name, type, definition, file, null, null);
    }

    public String getName() { return name; }
    public SymbolType getType() { return type; }
    public SourcePosition getDefinition() { return definition; }
    public File getFile() { return file; }
    public String getSignature() { return signature; }
    public String getDocumentation() { return documentation; }

    public String getDisplayName() {
        StringBuilder display = new StringBuilder(name);
        if (signature != null && !signature.isEmpty()) {
            display.append(signature);
        }
        return display.toString();
    }

    public String getIcon() {
        return switch (type) {
            case FUNCTION -> "🔧";
            case VARIABLE, LOCAL_VARIABLE -> "📝";
            case PARAMETER -> "📥";
            case STRUCT -> "🏗️";
            case FIELD -> "🔸";
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Symbol symbol = (Symbol) obj;
        return Objects.equals(name, symbol.name) &&
               type == symbol.type &&
               Objects.equals(definition, symbol.definition) &&
               Objects.equals(file, symbol.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, definition, file);
    }

    @Override
    public String toString() {
        return String.format("%s %s (%s at %s)", getIcon(), getDisplayName(), type, definition);
    }
}