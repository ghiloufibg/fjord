package com.fjord.runtime.builtin;

import com.fjord.runtime.*;

import java.util.List;

/**
 * Built-in function that converts string values to floating-point numbers.
 */
public final class ToFloatFunction implements Callable {

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("toFloat expects exactly 1 argument, got " + arguments.size());
        }

        Value arg = arguments.get(0);
        if (!(arg instanceof StringValue)) {
            throw new IllegalArgumentException("toFloat expects a string argument, got " +
                                               arg.getClass().getSimpleName());
        }

        String str = ((StringValue) arg).getValue().toString();
        try {
            double value = Double.parseDouble(str.trim());
            return new FloatValue(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert '" + str + "' to float");
        }
    }

    @Override
    public String toString() {
        return "<builtin function: toFloat>";
    }

    @Override
    public Object getValue() {
        return this;
    }
}