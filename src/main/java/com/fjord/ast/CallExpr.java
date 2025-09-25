package com.fjord.ast;

import com.fjord.runtime.Environment;
import com.fjord.runtime.FunctionValue;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a function call expression. The callee must evaluate to a
 * {@link FunctionValue}. Arguments are evaluated left‑to‑right and bound
 * to the parameters in a fresh environment captured from the function
 * definition. The result of the call is the value of the function body.
 */
public final class CallExpr implements Expression {

    private final Expression callee;
    private final List<Expression> arguments;

    /**
     * Constructs a new function call expression.
     *
     * @param callee    the expression yielding a function
     * @param arguments the list of argument expressions
     */
    public CallExpr(Expression callee, List<Expression> arguments) {
        this.callee = callee;
        this.arguments = new ArrayList<>(arguments);
    }

    @Override
    public Value evaluate(Environment env) {
        Value calleeValue = callee.evaluate(env);
        // Built‑in functions implement the Callable interface.
        if (calleeValue instanceof com.fjord.runtime.Callable) {
            com.fjord.runtime.Callable callable = (com.fjord.runtime.Callable) calleeValue;
            // Evaluate arguments in the current environment, not the closure
            List<Value> argVals = new java.util.ArrayList<>();
            for (Expression argExpr : arguments) {
                argVals.add(argExpr.evaluate(env));
            }
            return callable.call(argVals);
        }
        if (calleeValue instanceof FunctionValue) {
            FunctionValue fn = (FunctionValue) calleeValue;
            if (arguments.size() != fn.getParameters().size()) {
                throw new IllegalStateException(
                    "Function expected " + fn.getParameters().size() + " arguments but got " + arguments.size());
            }
            // Create a new environment based on the function's closure
            Environment callEnv = new Environment(fn.getClosure());
            // Evaluate arguments and bind to parameters
            for (int i = 0; i < arguments.size(); i++) {
                String param = fn.getParameters().get(i);
                Value argVal = arguments.get(i).evaluate(env);
                callEnv.define(param, argVal);
            }
            Value result = fn.getBody().evaluate(callEnv);
            return result == null ? UnitValue.INSTANCE : result;
        }
        throw new IllegalStateException("Attempted to call a non‑function");
    }

    /**
     * Returns the function expression.
     *
     * @return the function expression
     */
    public Expression getFunction() {
        return callee;
    }

    /**
     * Returns the argument expressions.
     *
     * @return the list of argument expressions
     */
    public List<Expression> getArguments() {
        return new ArrayList<>(arguments);
    }
}