package com.fjord.runtime;

/**
 * Represents an immutable floating‑point value in the Fjord interpreter.
 * Floats correspond to the {@code Float} type in the Fjord language and map
 * to {@link Double} at runtime.
 */
public final class FloatValue implements Value {

    private final double value;

    /**
     * Constructs a new floating‑point value.
     *
     * @param value the double to wrap
     */
    public FloatValue(double value) {
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    /**
     * Returns the primitive double represented by this value.
     *
     * @return the double
     */
    public double asDouble() {
        return value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }
}