package com.fjord.runtime.builtin;

import com.fjord.runtime.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in function for filtering arrays.
 * Signature: filter(array, predicate) -> array
 */
public class FilterFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 2) {
            throw new RuntimeException("filter expects exactly 2 arguments: array and predicate function");
        }

        Value arrayArg = arguments.get(0);
        Value predicateArg = arguments.get(1);

        if (!(arrayArg instanceof ArrayValue)) {
            throw new RuntimeException("First argument to filter must be an array");
        }

        if (!(predicateArg instanceof FunctionValue)) {
            throw new RuntimeException("Second argument to filter must be a function");
        }

        ArrayValue array = (ArrayValue) arrayArg;
        FunctionValue predicate = (FunctionValue) predicateArg;

        // Check that the predicate takes exactly one parameter
        if (predicate.getParameters().size() != 1) {
            throw new RuntimeException("Filter predicate function must take exactly one parameter");
        }

        List<Value> resultElements = new ArrayList<>();
        Environment closure = predicate.getClosure();

        for (Value element : array.getElements()) {
            // Create new environment for function call
            Environment callEnv = new Environment(closure);
            callEnv.define(predicate.getParameters().get(0), element);

            // Call the predicate with the element
            Value result = predicate.getBody().evaluate(callEnv);

            // Check if result is truthy
            if (isTruthy(result)) {
                resultElements.add(element);
            }
        }

        return new ArrayValue(resultElements);
    }

    private boolean isTruthy(Value value) {
        if (value instanceof BoolValue) {
            return ((BoolValue) value).asBoolean();
        }
        if (value instanceof IntValue) {
            return ((IntValue) value).asInt() != 0;
        }
        if (value instanceof FloatValue) {
            return ((FloatValue) value).asDouble() != 0.0;
        }
        if (value instanceof StringValue) {
            return !((StringValue) value).asString().isEmpty();
        }
        return value != null;
    }

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public String toString() {
        return "<builtin filter>";
    }
}