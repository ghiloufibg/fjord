package com.fjord.runtime.builtin;

import com.fjord.runtime.*;

import java.util.List;

/**
 * Built-in function that converts string values to integers.
 * Provides type conversion from string to int with error handling.
 */
public final class ToIntFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("toInt expects exactly 1 argument, got " + arguments.size());
        }

        Value arg = arguments.get(0);
        if (!(arg instanceof StringValue)) {
            throw new IllegalArgumentException("toInt expects a string argument, got " +
                                               arg.getClass().getSimpleName());
        }

        String str = ((StringValue) arg).getValue().toString();
        try {
            int value = Integer.parseInt(str.trim());
            return new IntValue(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert '" + str + "' to integer");
        }
    }

    @Override
    public String toString() {
        return "<builtin function: toInt>";
    }

    @Override
    public Object getValue() {
        return this;
    }
}