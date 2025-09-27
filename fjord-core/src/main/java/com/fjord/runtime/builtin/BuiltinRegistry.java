package com.fjord.runtime.builtin;

import com.fjord.runtime.Callable;
import com.fjord.runtime.Value;
import com.fjord.types.FunctionType;
import com.fjord.types.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Built-in function registry for standard library functions.
 * Provides extensible mechanism for adding native functions that can be
 * called from Fjord code. Built-in functions are implemented in Java
 * and provide core functionality like I/O, math operations, and type conversions.
 *
 * <p>The registry maintains both runtime callable implementations and
 * compile-time type information for proper semantic analysis.
 *
 * <p>Example usage:
 * <pre>{@code
 * BuiltinRegistry registry = BuiltinRegistry.createDefault();
 *
 * // Register a custom built-in function
 * registry.register("sqrt", new SqrtFunction(),
 *                   FunctionType.unary(Type.FLOAT, Type.FLOAT));
 *
 * // Get a built-in function for execution
 * Optional<Callable> println = registry.getFunction("println");
 * }</pre>
 */
public final class BuiltinRegistry {

    /**
     * Information about a built-in function including its implementation and type.
     */
    public static final class BuiltinInfo {
        private final Callable function;
        private final FunctionType type;
        private final String description;

        /**
         * Creates built-in function information.
         *
         * @param function    the callable implementation
         * @param type        the function type for type checking
         * @param description human-readable description
         */
        public BuiltinInfo(Callable function, FunctionType type, String description) {
            this.function = function;
            this.type = type;
            this.description = description;
        }

        /**
         * Returns the callable implementation.
         *
         * @return the function implementation
         */
        public Callable getFunction() {
            return function;
        }

        /**
         * Returns the function type.
         *
         * @return the type signature
         */
        public FunctionType getType() {
            return type;
        }

        /**
         * Returns the description.
         *
         * @return human-readable description
         */
        public String getDescription() {
            return description;
        }
    }

    /** Registry of all built-in functions */
    private final Map<String, BuiltinInfo> builtins = new HashMap<>();

    /**
     * Creates a new empty built-in registry.
     */
    public BuiltinRegistry() {
    }

    /**
     * Registers a new built-in function.
     *
     * @param name        function name as it appears in Fjord code
     * @param function    implementation of the function
     * @param type        type signature for semantic analysis
     * @param description human-readable description
     * @throws IllegalArgumentException if name is already registered
     */
    public void register(String name, Callable function, FunctionType type, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Built-in function name cannot be null or empty");
        }
        if (function == null) {
            throw new IllegalArgumentException("Built-in function implementation cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Built-in function type cannot be null");
        }

        if (builtins.containsKey(name)) {
            throw new IllegalArgumentException("Built-in function '" + name + "' is already registered");
        }

        builtins.put(name, new BuiltinInfo(function, type, description != null ? description : ""));
    }

    /**
     * Registers a built-in function without description.
     *
     * @param name     function name
     * @param function implementation
     * @param type     type signature
     */
    public void register(String name, Callable function, FunctionType type) {
        register(name, function, type, "");
    }

    /**
     * Returns the implementation of a built-in function.
     *
     * @param name function name
     * @return optional containing the function, or empty if not found
     */
    public Optional<Callable> getFunction(String name) {
        BuiltinInfo info = builtins.get(name);
        return info != null ? Optional.of(info.getFunction()) : Optional.empty();
    }

    /**
     * Returns the type signature of a built-in function.
     *
     * @param name function name
     * @return optional containing the type, or empty if not found
     */
    public Optional<FunctionType> getFunctionType(String name) {
        BuiltinInfo info = builtins.get(name);
        return info != null ? Optional.of(info.getType()) : Optional.empty();
    }

    /**
     * Returns complete information about a built-in function.
     *
     * @param name function name
     * @return optional containing the builtin info, or empty if not found
     */
    public Optional<BuiltinInfo> getBuiltinInfo(String name) {
        return Optional.ofNullable(builtins.get(name));
    }

    /**
     * Checks if a function is registered as a built-in.
     *
     * @param name function name to check
     * @return true if the function is a built-in
     */
    public boolean isBuiltin(String name) {
        return builtins.containsKey(name);
    }

    /**
     * Returns the names of all registered built-in functions.
     *
     * @return set of function names
     */
    public Set<String> getBuiltinNames() {
        return Set.copyOf(builtins.keySet());
    }

    /**
     * Returns the number of registered built-in functions.
     *
     * @return builtin function count
     */
    public int size() {
        return builtins.size();
    }

    /**
     * Removes a built-in function from the registry.
     *
     * @param name function name to remove
     * @return true if a function was removed
     */
    public boolean unregister(String name) {
        return builtins.remove(name) != null;
    }

    /**
     * Clears all registered built-in functions.
     */
    public void clear() {
        builtins.clear();
    }

    /**
     * Creates a registry with the default Fjord standard library functions.
     *
     * @return registry with standard built-ins
     */
    public static BuiltinRegistry createDefault() {
        BuiltinRegistry registry = new BuiltinRegistry();

        // Register println function
        registry.register("println", new PrintlnFunction(),
                         FunctionType.unary(Type.STRING, Type.UNIT),
                         "Prints a value followed by a newline");

        // Register type conversion functions
        registry.register("toString", new ToStringFunction(),
                         FunctionType.unary(Type.INT, Type.STRING), // Simplified - would support multiple types
                         "Converts a value to its string representation");

        registry.register("toInt", new ToIntFunction(),
                         FunctionType.unary(Type.STRING, Type.INT),
                         "Converts a string to an integer");

        registry.register("toFloat", new ToFloatFunction(),
                         FunctionType.unary(Type.STRING, Type.FLOAT),
                         "Converts a string to a floating-point number");

        // Register math functions
        registry.register("abs", new AbsFunction(),
                         FunctionType.unary(Type.INT, Type.INT),
                         "Returns the absolute value of a number");

        registry.register("sqrt", new SqrtFunction(),
                         FunctionType.unary(Type.FLOAT, Type.FLOAT),
                         "Returns the square root of a number");

        // Register array functions
        registry.register("len", new LenFunction(),
                         FunctionType.unary(Type.ARRAY, Type.INT),
                         "Returns the length of an array");

        registry.register("push", new PushFunction(),
                         new FunctionType(java.util.List.of(Type.ARRAY, Type.ANY), Type.UNIT),
                         "Adds an element to the end of an array");

        registry.register("get", new GetFunction(),
                         new FunctionType(java.util.List.of(Type.ARRAY, Type.INT), Type.ANY),
                         "Gets an element from an array at a specific index");

        // Register higher-order array functions
        registry.register("map", new MapFunction(),
                         new FunctionType(java.util.List.of(Type.ARRAY, new FunctionType(java.util.List.of(Type.ANY), Type.ANY)), Type.ARRAY),
                         "Maps a function over each element of an array");

        registry.register("filter", new FilterFunction(),
                         new FunctionType(java.util.List.of(Type.ARRAY, new FunctionType(java.util.List.of(Type.ANY), Type.BOOL)), Type.ARRAY),
                         "Filters an array based on a predicate function");

        registry.register("reduce", new ReduceFunction(),
                         new FunctionType(java.util.List.of(Type.ARRAY, Type.ANY, new FunctionType(java.util.List.of(Type.ANY, Type.ANY), Type.ANY)), Type.ANY),
                         "Reduces an array to a single value using an accumulator function");

        return registry;
    }

    /**
     * Creates a minimal registry with only essential functions.
     *
     * @return minimal registry
     */
    public static BuiltinRegistry createMinimal() {
        BuiltinRegistry registry = new BuiltinRegistry();
        registry.register("println", new PrintlnFunction(),
                         FunctionType.unary(Type.STRING, Type.UNIT),
                         "Prints a value followed by a newline");
        return registry;
    }

    /**
     * Returns a string representation of all registered functions.
     *
     * @return formatted list of built-in functions
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BuiltinRegistry{count=").append(builtins.size()).append(", functions=[\n");

        for (Map.Entry<String, BuiltinInfo> entry : builtins.entrySet()) {
            BuiltinInfo info = entry.getValue();
            sb.append("  ").append(entry.getKey())
              .append(": ").append(info.getType())
              .append(" - ").append(info.getDescription())
              .append("\n");
        }

        sb.append("]}");
        return sb.toString();
    }
}