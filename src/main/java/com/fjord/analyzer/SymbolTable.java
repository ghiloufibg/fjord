package com.fjord.analyzer;

import com.fjord.types.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Efficient symbol table with scope management.
 * Replaces simple Environment with optimized variable resolution and type information.
 * Supports nested scopes and provides fast symbol lookup during semantic analysis.
 *
 * <p>The symbol table maintains:
 * <ul>
 *   <li>Variable names to type mappings</li>
 *   <li>Function signatures</li>
 *   <li>Nested scope management</li>
 *   <li>Symbol resolution with scope traversal</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * SymbolTable symbols = new SymbolTable();
 * symbols.define("x", Type.INT);
 * symbols.enterScope();
 * symbols.define("y", Type.FLOAT);
 *
 * Optional<SymbolInfo> info = symbols.resolve("x");
 * if (info.isPresent()) {
 *     Type type = info.get().getType();
 * }
 * }</pre>
 */
public final class SymbolTable {

    /**
     * Information about a symbol in the symbol table.
     */
    public static final class SymbolInfo {
        private final String name;
        private final Type type;
        private final int scopeLevel;
        private final boolean isMutable;

        /**
         * Creates symbol information.
         *
         * @param name       the symbol name
         * @param type       the symbol type
         * @param scopeLevel the scope level where symbol was defined
         * @param isMutable  whether the symbol can be reassigned
         */
        public SymbolInfo(String name, Type type, int scopeLevel, boolean isMutable) {
            this.name = name;
            this.type = type;
            this.scopeLevel = scopeLevel;
            this.isMutable = isMutable;
        }

        /**
         * Returns the symbol name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the symbol type.
         *
         * @return the type
         */
        public Type getType() {
            return type;
        }

        /**
         * Returns the scope level where this symbol was defined.
         *
         * @return the scope level
         */
        public int getScopeLevel() {
            return scopeLevel;
        }

        /**
         * Returns whether this symbol can be reassigned.
         *
         * @return true if mutable
         */
        public boolean isMutable() {
            return isMutable;
        }

        @Override
        public String toString() {
            return String.format("SymbolInfo{name='%s', type=%s, scope=%d, mutable=%s}",
                    name, type, scopeLevel, isMutable);
        }
    }

    /** Current scope level (0 = global scope) */
    private int currentScopeLevel = 0;

    /** Maps symbol names to their information at each scope level */
    private final Map<String, SymbolInfo> symbols = new HashMap<>();

    /** Stack to track scope boundaries for cleanup */
    private final java.util.Stack<java.util.Set<String>> scopeStack = new java.util.Stack<>();

    /**
     * Creates a new symbol table.
     */
    public SymbolTable() {
        scopeStack.push(new java.util.HashSet<>());
    }

    /**
     * Defines a new symbol in the current scope.
     * If a symbol with the same name exists in the current scope, it is shadowed.
     *
     * @param name     the symbol name
     * @param type     the symbol type
     * @param mutable  whether the symbol can be reassigned
     * @throws IllegalArgumentException if name is null or empty
     */
    public void define(String name, Type type, boolean mutable) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol name cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Symbol type cannot be null");
        }

        SymbolInfo info = new SymbolInfo(name, type, currentScopeLevel, mutable);
        symbols.put(name, info);
        scopeStack.peek().add(name);
    }

    /**
     * Defines an immutable symbol in the current scope.
     *
     * @param name the symbol name
     * @param type the symbol type
     */
    public void define(String name, Type type) {
        define(name, type, false);
    }

    /**
     * Resolves a symbol to its storage location for fast access.
     * Searches from the current scope up through enclosing scopes.
     *
     * @param name the symbol name to resolve
     * @return SymbolInfo containing type and storage information, or empty if not found
     */
    public Optional<SymbolInfo> resolve(String name) {
        return Optional.ofNullable(symbols.get(name));
    }

    /**
     * Checks if a symbol is defined in the current scope only.
     *
     * @param name the symbol name
     * @return true if the symbol exists in the current scope
     */
    public boolean isDefinedInCurrentScope(String name) {
        SymbolInfo info = symbols.get(name);
        return info != null && info.getScopeLevel() == currentScopeLevel;
    }

    /**
     * Enters a new nested scope.
     * Variables defined in this scope will shadow outer scope variables with the same name.
     */
    public void enterScope() {
        currentScopeLevel++;
        scopeStack.push(new java.util.HashSet<>());
    }

    /**
     * Exits the current scope and removes all symbols defined in it.
     * This restores visibility of shadowed symbols from outer scopes.
     *
     * @throws IllegalStateException if already at global scope
     */
    public void exitScope() {
        if (currentScopeLevel == 0) {
            throw new IllegalStateException("Cannot exit global scope");
        }

        // Remove all symbols defined in the current scope
        java.util.Set<String> scopeSymbols = scopeStack.pop();
        for (String symbolName : scopeSymbols) {
            symbols.remove(symbolName);
        }

        currentScopeLevel--;
    }

    /**
     * Returns the current scope level.
     *
     * @return the current scope level (0 = global)
     */
    public int getCurrentScopeLevel() {
        return currentScopeLevel;
    }

    /**
     * Returns whether we are in the global scope.
     *
     * @return true if in global scope
     */
    public boolean isInGlobalScope() {
        return currentScopeLevel == 0;
    }

    /**
     * Returns all symbols defined in all scopes.
     * This is primarily used for debugging and testing.
     *
     * @return map of all currently visible symbols
     */
    public Map<String, SymbolInfo> getAllSymbols() {
        return new HashMap<>(symbols);
    }

    /**
     * Clears all symbols and resets to global scope.
     * Useful for reusing the same symbol table instance.
     */
    public void clear() {
        symbols.clear();
        scopeStack.clear();
        scopeStack.push(new java.util.HashSet<>());
        currentScopeLevel = 0;
    }

    /**
     * Returns a string representation of the symbol table.
     *
     * @return formatted symbol table content
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SymbolTable{currentLevel=").append(currentScopeLevel).append(", symbols=[\n");
        for (SymbolInfo info : symbols.values()) {
            sb.append("  ").append(info).append("\n");
        }
        sb.append("]}");
        return sb.toString();
    }
}