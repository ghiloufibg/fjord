package com.fjord.runtime;

/**
 * Represents an immutable string value in the Fjord interpreter. Strings
 * correspond to the {@code String} type in the language and map to
 * {@link String} at runtime.
 */
public final class StringValue implements Value {

    private final String value;

    /**
     * Constructs a new string value.
     *
     * @param value the string to wrap
     */
    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    /**
     * Returns the underlying Java string.
     *
     * @return the string
     */
    public String asString() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}