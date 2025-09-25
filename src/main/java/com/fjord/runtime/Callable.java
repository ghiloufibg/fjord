package com.fjord.runtime;

import java.util.List;

/**
 * Represents a callable function at runtime. This marker is used for
 * built‑in functions which cannot be represented directly as user‑defined
 * {@link FunctionValue}s. A callable receives a list of evaluated
 * arguments and returns a {@link Value}.
 */
public interface Callable extends Value {
    /**
     * Invokes this callable with the provided list of argument values.
     *
     * @param arguments the arguments to the function
     * @return the result of the call
     */
    Value call(List<Value> arguments);
}