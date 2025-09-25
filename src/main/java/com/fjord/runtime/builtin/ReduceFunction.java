package com.fjord.runtime.builtin;

import com.fjord.runtime.*;
import java.util.List;

/**
 * Built-in function for reducing arrays.
 * Signature: reduce(array, accumulator, function) -> value
 */
public class ReduceFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 3) {
            throw new RuntimeException("reduce expects exactly 3 arguments: array, initial value, and reducer function");
        }

        Value arrayArg = arguments.get(0);
        Value initialArg = arguments.get(1);
        Value reducerArg = arguments.get(2);

        if (!(arrayArg instanceof ArrayValue)) {
            throw new RuntimeException("First argument to reduce must be an array");
        }

        if (!(reducerArg instanceof FunctionValue)) {
            throw new RuntimeException("Third argument to reduce must be a function");
        }

        ArrayValue array = (ArrayValue) arrayArg;
        FunctionValue reducer = (FunctionValue) reducerArg;

        // Check that the reducer takes exactly two parameters (accumulator, element)
        if (reducer.getParameters().size() != 2) {
            throw new RuntimeException("Reducer function must take exactly two parameters: accumulator and element");
        }

        Value accumulator = initialArg;
        Environment closure = reducer.getClosure();

        for (Value element : array.getElements()) {
            // Create new environment for function call
            Environment callEnv = new Environment(closure);
            callEnv.define(reducer.getParameters().get(0), accumulator);
            callEnv.define(reducer.getParameters().get(1), element);

            // Call the reducer with accumulator and element
            accumulator = reducer.getBody().evaluate(callEnv);
        }

        return accumulator;
    }

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public String toString() {
        return "<builtin reduce>";
    }
}