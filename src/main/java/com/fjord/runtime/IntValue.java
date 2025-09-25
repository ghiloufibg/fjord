package com.fjord.runtime;

/**
 * Represents an immutable integer value in the Fjord interpreter. Integers
 * correspond to the {@code Int} type in the Fjord language and map to
 * {@link Integer} at runtime.
 */
public final class IntValue implements Value {

    private final int value;

    /**
     * Constructs a new integer value.
     *
     * @param value the integer to wrap
     */
    public IntValue(int value) {
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    /**
     * Returns the primitive integer represented by this value.
     *
     * @return the integer
     */
    public int asInt() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}