package com.fjord.runtime;

import com.fjord.ast.Expression;

import java.util.List;
import java.util.Objects;

/**
 * Represents a user‑defined function in the Fjord interpreter. Functions
 * capture their defining environment to support closures and hold onto
 * the parameter list and body expression. Invocation is handled by the
 * interpreter which creates a new environment and binds arguments.
 */
public final class FunctionValue implements Value {

    private final List<String> parameters;
    private final Expression body;
    private final Environment closure;

    /**
     * Constructs a new function value.
     *
     * @param parameters the ordered list of parameter names
     * @param body       the body expression of the function
     * @param closure    the defining environment captured at creation
     */
    public FunctionValue(List<String> parameters, Expression body, Environment closure) {
        this.parameters = List.copyOf(parameters);
        this.body = Objects.requireNonNull(body);
        this.closure = closure;
    }

    /** Returns the parameter names for this function. */
    public List<String> getParameters() {
        return parameters;
    }

    /** Returns the body expression of the function. */
    public Expression getBody() {
        return body;
    }

    /** Returns the captured defining environment. */
    public Environment getClosure() {
        return closure;
    }

    @Override
    public Object getValue() {
        // Functions are represented by themselves.
        return this;
    }

    @Override
    public String toString() {
        return "<fn>";
    }
}