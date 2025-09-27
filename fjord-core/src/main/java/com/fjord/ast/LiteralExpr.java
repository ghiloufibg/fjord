package com.fjord.ast;

import com.fjord.runtime.Environment;
import com.fjord.runtime.Value;

/**
 * Represents a literal value in the abstract syntax tree. Literals include
 * numbers, booleans and strings. Evaluation simply returns the stored
 * {@link Value}.
 */
public final class LiteralExpr implements Expression {

    private final Value value;

    /**
     * Constructs a new literal expression.
     *
     * @param value the value to wrap
     */
    public LiteralExpr(Value value) {
        this.value = value;
    }

    @Override
    public Value evaluate(Environment env) {
        return value;
    }

    /**
     * Returns the literal value.
     *
     * @return the stored value
     */
    public Value getValue() {
        return value;
    }
}