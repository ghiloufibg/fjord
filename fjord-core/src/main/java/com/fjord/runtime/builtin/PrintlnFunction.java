package com.fjord.runtime.builtin;

import com.fjord.runtime.BoolValue;
import com.fjord.runtime.Callable;
import com.fjord.runtime.IntValue;
import com.fjord.runtime.FloatValue;
import com.fjord.runtime.StringValue;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;

import java.util.List;

/**
 * Implements a simple println built‑in function. The function accepts a
 * single argument of any type, converts it to a string representation
 * and writes it to standard output followed by a newline. The return
 * value is unit. If more than one argument is supplied a runtime
 * exception is thrown.
 */
public class PrintlnFunction implements Callable {
    @Override
    public Value getValue() {
        return this;
    }

    @Override
    public Value call(List<Value> arguments) {
        if (arguments.size() != 1) {
            throw new IllegalStateException("println expects 1 argument but got " + arguments.size());
        }
        Value arg = arguments.get(0);
        Object val = arg.getValue();
        System.out.println(val);
        return UnitValue.INSTANCE;
    }

    @Override
    public String toString() {
        return "<builtin println>";
    }
}