package com.fjord.ast;

import com.fjord.runtime.Environment;
import com.fjord.runtime.Value;

import java.util.Optional;

/**
 * Represents a reference to a variable. During evaluation the variable
 * name is looked up in the environment chain and the bound value is
 * returned. If the variable cannot be found an exception is thrown.
 */
public final class VariableExpr implements Expression {

    private final String name;

    /**
     * Constructs a new variable expression.
     *
     * @param name the identifier of the variable
     */
    public VariableExpr(String name) {
        this.name = name;
    }

    @Override
    public Value evaluate(Environment env) {
        Optional<Value> value = env.get(name);
        if (value.isPresent()) {
            return value.get();
        }
        throw new IllegalStateException("Undefined variable '" + name + "'");
    }

    /**
     * Returns the variable name.
     *
     * @return the variable name
     */
    public String getName() {
        return name;
    }
}