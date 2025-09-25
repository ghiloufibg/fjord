package com.fjord.ast;

import com.fjord.runtime.Environment;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;

/**
 * Represents a variable declaration. A {@code let} binds a name to the
 * evaluated result of an expression within the current scope. A {@code let mut}
 * creates a mutable variable that can be reassigned later.
 * Evaluation of a let expression returns the unit value.
 */
public final class LetExpr implements Expression {

    private final String name;
    private final String declaredType; // may be null if omitted
    private final Expression initializer;
    private final boolean isMutable;

    /**
     * Constructs a new let expression.
     *
     * @param name         the variable name
     * @param declaredType the optional declared type, may be null
     * @param initializer  the expression that computes the value
     * @param isMutable    whether the variable can be reassigned
     */
    public LetExpr(String name, String declaredType, Expression initializer, boolean isMutable) {
        this.name = name;
        this.declaredType = declaredType;
        this.initializer = initializer;
        this.isMutable = isMutable;
    }

    @Override
    public Value evaluate(Environment env) {
        Value value = initializer.evaluate(env);
        env.define(name, value);
        return UnitValue.INSTANCE;
    }

    /**
     * Returns the variable name.
     *
     * @return the variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the declared type, if any.
     *
     * @return the declared type, or null if not specified
     */
    public String getDeclaredType() {
        return declaredType;
    }

    /**
     * Returns the initializer expression.
     *
     * @return the initializer expression
     */
    public Expression getInitializer() {
        return initializer;
    }

    /**
     * Returns whether the variable is mutable.
     *
     * @return true if the variable can be reassigned
     */
    public boolean isMutable() {
        return isMutable;
    }
}