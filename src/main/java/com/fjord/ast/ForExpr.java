package com.fjord.ast;

import com.fjord.error.SourcePosition;
import com.fjord.runtime.ArrayValue;
import com.fjord.runtime.Environment;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;

/**
 * Represents a for loop expression in the abstract syntax tree.
 * For loops iterate over arrays using the syntax: for variable in array do body.
 * The loop variable is bound to each element of the array in sequence.
 */
public final class ForExpr extends ASTNode implements Expression {

    private final String variable;
    private final Expression iterable;
    private final Expression body;

    /**
     * Constructs a new for loop expression.
     *
     * @param variable the name of the loop variable
     * @param iterable the expression that evaluates to an array to iterate over
     * @param body the expression to execute for each iteration
     */
    public ForExpr(String variable, Expression iterable, Expression body) {
        super(null); // TODO: Pass actual source position from parser
        this.variable = variable;
        this.iterable = iterable;
        this.body = body;
    }

    @Override
    public Value evaluate(Environment env) {
        Value iterableValue = iterable.evaluate(env);

        if (!(iterableValue instanceof ArrayValue)) {
            throw new RuntimeException("Can only iterate over arrays");
        }

        ArrayValue array = (ArrayValue) iterableValue;
        Environment loopEnv = new Environment(env);

        for (int i = 0; i < array.size(); i++) {
            Value element = array.get(i);
            loopEnv.define(variable, element);
            body.evaluate(loopEnv);
        }

        return UnitValue.INSTANCE;
    }

    /**
     * Returns the loop variable name.
     *
     * @return the variable name
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Returns the iterable expression.
     *
     * @return the iterable expression
     */
    public Expression getIterable() {
        return iterable;
    }

    /**
     * Returns the body expression.
     *
     * @return the body expression
     */
    public Expression getBody() {
        return body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFor(this);
    }
}