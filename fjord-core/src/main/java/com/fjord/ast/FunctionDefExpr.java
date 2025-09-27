package com.fjord.ast;

import com.fjord.runtime.Environment;
import com.fjord.runtime.FunctionValue;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;

import java.util.List;

/**
 * Represents a function definition. A function has a name, a list of
 * parameters and a body expression. Evaluation of a function definition
 * captures the current environment and binds the resulting function value
 * to its name. The return value of the definition is the unit value.
 */
public final class FunctionDefExpr implements Expression {

    private final String name;
    private final List<String> parameters;
    private final Expression body;

    /**
     * Constructs a new function definition expression.
     *
     * @param name       the function name
     * @param parameters the ordered list of parameter names
     * @param body       the body expression
     */
    public FunctionDefExpr(String name, List<String> parameters, Expression body) {
        this.name = name;
        this.parameters = List.copyOf(parameters);
        this.body = body;
    }

    @Override
    public Value evaluate(Environment env) {
        // Capture the current environment for closure
        FunctionValue fn = new FunctionValue(parameters, body, new Environment(env));
        env.define(name, fn);
        return UnitValue.INSTANCE;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the function parameters.
     *
     * @return the list of parameter names
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * Returns the function body.
     *
     * @return the body expression
     */
    public Expression getBody() {
        return body;
    }
}