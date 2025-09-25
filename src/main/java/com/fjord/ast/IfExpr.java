package com.fjord.ast;

import com.fjord.runtime.BoolValue;
import com.fjord.runtime.Environment;
import com.fjord.runtime.Value;

/**
 * Represents a conditional expression. The condition is evaluated first,
 * followed by either the then branch or the else branch depending on the
 * boolean result. Both branches are evaluated lazily and the value of
 * the selected branch becomes the result.
 */
public final class IfExpr implements Expression {

    private final Expression condition;
    private final Expression thenBranch;
    private final Expression elseBranch;

    /**
     * Constructs a new if expression.
     *
     * @param condition the condition expression
     * @param thenBranch the expression to evaluate when the condition is true
     * @param elseBranch the expression to evaluate when the condition is false
     */
    public IfExpr(Expression condition, Expression thenBranch, Expression elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public Value evaluate(Environment env) {
        Value cond = condition.evaluate(env);
        if (!(cond instanceof BoolValue)) {
            throw new IllegalStateException("If condition must be a boolean");
        }
        boolean test = ((BoolValue) cond).asBoolean();
        return test ? thenBranch.evaluate(env) : elseBranch.evaluate(env);
    }

    /**
     * Returns the condition expression.
     *
     * @return the condition
     */
    public Expression getCondition() {
        return condition;
    }

    /**
     * Returns the then branch expression.
     *
     * @return the then branch
     */
    public Expression getThenBranch() {
        return thenBranch;
    }

    /**
     * Returns the else branch expression.
     *
     * @return the else branch
     */
    public Expression getElseBranch() {
        return elseBranch;
    }
}