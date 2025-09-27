package com.fjord.runtime;

/**
 * Marker interface representing a runtime value in the Fjord interpreter.
 * Concrete implementations wrap Java primitives and objects. Each value
 * knows how to return its underlying Java representation via
 * {@link #getValue()}.
 */
public interface Value {
    /**
     * Returns the underlying Java object for this value. For example
     * an {@code IntValue} would return an {@link Integer} and a
     * {@code BoolValue} would return a {@link Boolean}.
     *
     * @return the wrapped value
     */
    Object getValue();
}