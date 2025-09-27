package com.fjord.runtime.builtin;

import com.fjord.runtime.ArrayValue;
import com.fjord.runtime.Callable;
import com.fjord.runtime.IntValue;
import com.fjord.runtime.Value;
import com.fjord.types.FunctionType;
import com.fjord.types.Type;

import java.util.List;

/**
 * Built-in function that returns the length of an array.
 * Usage: len(array) -> Int
 */
public class LenFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 1) {
            throw new RuntimeException("len() expects exactly 1 argument, got " + arguments.size());
        }

        Value arg = arguments.get(0);
        if (!(arg instanceof ArrayValue)) {
            throw new RuntimeException("len() can only be called on arrays");
        }

        ArrayValue array = (ArrayValue) arg;
        return new IntValue(array.size());
    }

    @Override
    public Object getValue() {
        return this;
    }
}