package com.fjord.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a lexical environment for the Fjord interpreter. Each
 * environment maintains a mapping of variable names to {@link Value}s and
 * optionally refers to an enclosing environment. Variable lookup proceeds
 * through the chain of environments until a binding is found or the root
 * is reached.
 */
public class Environment {

    private final Map<String, Value> values = new HashMap<>();
    private final Environment enclosing;

    /**
     * Constructs a top level environment with no parent.
     */
    public Environment() {
        this(null);
    }

    /**
     * Constructs a nested environment with the given parent.
     *
     * @param enclosing the enclosing environment, or {@code null} for the root
     */
    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * Defines a new variable in the current environment. If the variable
     * already exists in this environment it is overwritten. Shadowing of
     * variables in outer scopes is allowed and respected during lookup.
     *
     * @param name  the variable name
     * @param value the value to bind
     */
    public void define(String name, Value value) {
        values.put(name, value);
    }

    /**
     * Retrieves the value of a variable from this environment or its ancestors.
     *
     * @param name the variable name
     * @return an {@link Optional} containing the value if found, otherwise empty
     */
    public Optional<Value> get(String name) {
        if (values.containsKey(name)) {
            return Optional.ofNullable(values.get(name));
        }
        if (enclosing != null) {
            return enclosing.get(name);
        }
        return Optional.empty();
    }

    /**
     * Assigns a value to an existing variable. Assignment walks up the chain
     * of environments to find the variable. If the variable does not exist
     * an exception is thrown.
     *
     * @param name  the variable name
     * @param value the new value
     * @throws IllegalStateException if the variable is not defined
     */
    public void assign(String name, Value value) {
        if (values.containsKey(name)) {
            values.put(name, value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new IllegalStateException("Undefined variable '" + name + "'");
    }
}