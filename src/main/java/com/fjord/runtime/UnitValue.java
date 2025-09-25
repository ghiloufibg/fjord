package com.fjord.runtime;

/**
 * Represents the single value of the {@code Unit} type. The unit type is
 * analogous to {@code void} in Java but as a proper value. It has exactly
 * one inhabitant which this class models. Callers should use
 * {@link #INSTANCE} rather than instantiating new objects.
 */
public final class UnitValue implements Value {

    /** The singleton instance of unit. */
    public static final UnitValue INSTANCE = new UnitValue();

    private UnitValue() {
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public String toString() {
        return "()";
    }
}