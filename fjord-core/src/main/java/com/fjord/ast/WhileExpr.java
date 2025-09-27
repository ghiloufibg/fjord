package com.fjord.ast;

import com.fjord.error.SourcePosition;
import com.fjord.runtime.BoolValue;
import com.fjord.runtime.Environment;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;

/**
 * Represents a while loop expression. The condition is evaluated before each
 * iteration, and if it evaluates to true, the body is executed. The loop
 * continues until the condition becomes false. While loops always return
 * the unit value.
 */
public final class WhileExpr extends ASTNode {

    private final Expression condition;
    private final Expression body;

    /**
     * Constructs a new while expression.
     *
     * @param condition the condition expression that controls loop execution
     * @param body the expression to execute repeatedly while condition is true
     */
    public WhileExpr(Expression condition, Expression body) {
        super(new SourcePosition(1, 1, 0)); // TODO: get actual position from parser
        this.condition = condition;
        this.body = body;
    }

    @Override
    public Value evaluate(Environment env) {
        while (true) {
            Value cond = condition.evaluate(env);
            if (!(cond instanceof BoolValue)) {
                throw new IllegalStateException("While condition must be a boolean");
            }
            boolean test = ((BoolValue) cond).asBoolean();
            if (!test) {
                break;
            }
            body.evaluate(env);
        }
        return UnitValue.INSTANCE;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitWhileExpr(this);
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
     * Returns the body expression.
     *
     * @return the body
     */
    public Expression getBody() {
        return body;
    }
}