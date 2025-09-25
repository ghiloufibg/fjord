package com.fjord.runtime.builtin;

import com.fjord.runtime.Callable;
import com.fjord.runtime.Environment;
import com.fjord.runtime.StringValue;
import com.fjord.runtime.Value;

import java.util.List;

/**
 * Built-in function that converts any value to its string representation.
 * This function provides type conversion capabilities for the Fjord language.
 *
 * <p>Usage in Fjord code:
 * <pre>{@code
 * let x = 42
 * let str = toString(x)  // "42"
 * }</pre>
 */
public final class ToStringFunction implements Callable {

    /**
     * Converts the given value to a string.
     *
     * @param arguments list containing exactly one argument of any type
     * @return StringValue containing the string representation
     * @throws IllegalArgumentException if argument count is incorrect
     */
    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("toString expects exactly 1 argument, got " + arguments.size());
        }

        Value arg = arguments.get(0);
        Object value = arg.getValue();

        // Convert to string representation
        String stringValue;
        if (value == null) {
            stringValue = "null";
        } else {
            stringValue = value.toString();
        }

        return new StringValue(stringValue);
    }

    @Override
    public String toString() {
        return "<builtin function: toString>";
    }

    @Override
    public Object getValue() {
        return this;
    }
}