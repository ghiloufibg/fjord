package com.fjord.runtime.builtin;

import com.fjord.runtime.ArrayValue;
import com.fjord.runtime.Callable;
import com.fjord.runtime.IntValue;
import com.fjord.runtime.Value;
import com.fjord.types.FunctionType;
import com.fjord.types.Type;

import java.util.List;

/**
 * Built-in function that gets an element from an array at a specific index.
 * Usage: get(array, index) -> Any
 */
public class GetFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 2) {
            throw new RuntimeException("get() expects exactly 2 arguments, got " + arguments.size());
        }

        Value arrayArg = arguments.get(0);
        Value indexArg = arguments.get(1);

        if (!(arrayArg instanceof ArrayValue)) {
            throw new RuntimeException("get() first argument must be an array");
        }

        if (!(indexArg instanceof IntValue)) {
            throw new RuntimeException("get() second argument must be an integer");
        }

        ArrayValue array = (ArrayValue) arrayArg;
        int index = (Integer) indexArg.getValue();

        if (index < 0 || index >= array.size()) {
            throw new RuntimeException("Array index out of bounds: " + index);
        }

        return array.get(index);
    }

    @Override
    public Object getValue() {
        return this;
    }
}