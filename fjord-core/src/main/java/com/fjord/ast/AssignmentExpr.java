package com.fjord.ast;

import com.fjord.error.SourcePosition;
import com.fjord.runtime.Environment;
import com.fjord.runtime.Value;

/**
 * Represents a variable assignment expression. Assigns a new value to an
 * existing mutable variable. The assignment expression evaluates to the
 * assigned value.
 */
public final class AssignmentExpr extends ASTNode {

    private final String name;
    private final Expression value;

    /**
     * Constructs a new assignment expression.
     *
     * @param name the variable name to assign to
     * @param value the expression that computes the new value
     */
    public AssignmentExpr(String name, Expression value) {
        super(new SourcePosition(1, 1, 0)); // TODO: get actual position from parser
        this.name = name;
        this.value = value;
    }

    @Override
    public Value evaluate(Environment env) {
        Value newValue = value.evaluate(env);
        env.assign(name, newValue);
        return newValue;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAssignmentExpr(this);
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
     * Returns the value expression.
     *
     * @return the value expression
     */
    public Expression getValue() {
        return value;
    }
}