package com.fjord.runtime.builtin;

import com.fjord.runtime.*;

import java.util.List;

/**
 * Built-in function that returns the absolute value of a number.
 * Supports both integer and floating-point arguments.
 */
public final class AbsFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("abs expects exactly 1 argument, got " + arguments.size());
        }

        Value arg = arguments.get(0);

        if (arg instanceof IntValue) {
            int value = ((IntValue) arg).asInt();
            return new IntValue(Math.abs(value));
        } else if (arg instanceof FloatValue) {
            double value = ((FloatValue) arg).asDouble();
            return new FloatValue(Math.abs(value));
        } else {
            throw new IllegalArgumentException("abs expects a numeric argument, got " +
                                               arg.getClass().getSimpleName());
        }
    }

    @Override
    public String toString() {
        return "<builtin function: abs>";
    }

    @Override
    public Object getValue() {
        return this;
    }
}