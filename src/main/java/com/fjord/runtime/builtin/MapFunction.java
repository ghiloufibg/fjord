package com.fjord.runtime.builtin;

import com.fjord.runtime.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in function for mapping over arrays.
 * Signature: map(array, function) -> array
 */
public class MapFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 2) {
            throw new RuntimeException("map expects exactly 2 arguments: array and function");
        }

        Value arrayArg = arguments.get(0);
        Value functionArg = arguments.get(1);

        if (!(arrayArg instanceof ArrayValue)) {
            throw new RuntimeException("First argument to map must be an array");
        }

        if (!(functionArg instanceof FunctionValue)) {
            throw new RuntimeException("Second argument to map must be a function");
        }

        ArrayValue array = (ArrayValue) arrayArg;
        FunctionValue function = (FunctionValue) functionArg;

        // Check that the function takes exactly one parameter
        if (function.getParameters().size() != 1) {
            throw new RuntimeException("Map function must take exactly one parameter");
        }

        List<Value> resultElements = new ArrayList<>();
        Environment closure = function.getClosure();

        for (Value element : array.getElements()) {
            // Create new environment for function call
            Environment callEnv = new Environment(closure);
            callEnv.define(function.getParameters().get(0), element);

            // Call the function with the element
            Value result = function.getBody().evaluate(callEnv);
            resultElements.add(result);
        }

        return new ArrayValue(resultElements);
    }

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public String toString() {
        return "<builtin map>";
    }
}