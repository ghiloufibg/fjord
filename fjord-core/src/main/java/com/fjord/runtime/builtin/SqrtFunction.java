package com.fjord.runtime.builtin;

import com.fjord.runtime.*;

import java.util.List;

/**
 * Built-in function that returns the square root of a number.
 * Always returns a floating-point result.
 */
public final class SqrtFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("sqrt expects exactly 1 argument, got " + arguments.size());
        }

        Value arg = arguments.get(0);
        double value;

        if (arg instanceof IntValue) {
            value = ((IntValue) arg).asInt();
        } else if (arg instanceof FloatValue) {
            value = ((FloatValue) arg).asDouble();
        } else {
            throw new IllegalArgumentException("sqrt expects a numeric argument, got " +
                                               arg.getClass().getSimpleName());
        }

        if (value < 0) {
            throw new IllegalArgumentException("sqrt of negative number: " + value);
        }

        return new FloatValue(Math.sqrt(value));
    }

    @Override
    public String toString() {
        return "<builtin function: sqrt>";
    }

    @Override
    public Object getValue() {
        return this;
    }
}