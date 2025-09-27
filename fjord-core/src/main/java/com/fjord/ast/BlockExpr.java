package com.fjord.ast;

import com.fjord.runtime.Environment;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a block of expressions enclosed in braces. Each block
 * introduces a new lexical scope. The value of the block is the value of
 * its final expression; if the block contains no expressions then the
 * unit value is returned.
 */
public final class BlockExpr implements Expression {

    private final List<Expression> expressions;

    /**
     * Constructs a new block expression.
     *
     * @param expressions the expressions to evaluate sequentially
     */
    public BlockExpr(List<Expression> expressions) {
        this.expressions = new ArrayList<>(expressions);
    }

    @Override
    public Value evaluate(Environment env) {
        // Create a new environment for the block scope
        Environment local = new Environment(env);
        Value result = UnitValue.INSTANCE;
        for (Expression expr : expressions) {
            result = expr.evaluate(local);
        }
        return result;
    }

    /**
     * Returns the statements in this block.
     *
     * @return the list of statements
     */
    public List<Expression> getStatements() {
        return new ArrayList<>(expressions);
    }
}