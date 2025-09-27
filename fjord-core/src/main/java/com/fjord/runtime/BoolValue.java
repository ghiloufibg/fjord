package com.fjord.runtime;

/**
 * Represents an immutable boolean value in the Fjord interpreter. Booleans
 * correspond to the {@code Bool} type in the language and map to
 * {@link Boolean} at runtime.
 */
public final class BoolValue implements Value {

    private final boolean value;

    /**
     * Constructs a new boolean value.
     *
     * @param value the boolean to wrap
     */
    public BoolValue(boolean value) {
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    /**
     * Returns the primitive boolean represented by this value.
     *
     * @return the boolean
     */
    public boolean asBoolean() {
        return value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}