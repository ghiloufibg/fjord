package com.fjord.runtime.builtin;

import com.fjord.runtime.ArrayValue;
import com.fjord.runtime.Callable;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;
import com.fjord.types.FunctionType;
import com.fjord.types.Type;

import java.util.List;

/**
 * Built-in function that adds an element to the end of an array.
 * Usage: push(array, element) -> Unit
 */
public class PushFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 2) {
            throw new RuntimeException("push() expects exactly 2 arguments, got " + arguments.size());
        }

        Value arrayArg = arguments.get(0);
        Value element = arguments.get(1);

        if (!(arrayArg instanceof ArrayValue)) {
            throw new RuntimeException("push() first argument must be an array");
        }

        ArrayValue array = (ArrayValue) arrayArg;
        array.add(element);

        return UnitValue.INSTANCE;
    }

    @Override
    public Object getValue() {
        return this;
    }
}