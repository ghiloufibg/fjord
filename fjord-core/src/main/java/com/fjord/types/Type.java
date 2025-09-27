package com.fjord.types;

/**
 * Rich type system supporting inference and checking.
 * Enables better error detection and optimization opportunities.
 * This is the base class for all types in the Fjord language.
 *
 * <p>The type system supports:
 * <ul>
 *   <li>Primitive types: int, float, string, bool, unit</li>
 *   <li>Function types: (T1, T2, ...) -> T</li>
 *   <li>Type compatibility checking</li>
 *   <li>Type unification for inference</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * Type intType = Type.INT;
 * Type floatType = Type.FLOAT;
 * boolean compatible = intType.isCompatibleWith(floatType);
 * }</pre>
 */
public abstract class Type {

    /** Built-in primitive types */
    public static final Type INT = new IntType();
    public static final Type FLOAT = new FloatType();
    public static final Type STRING = new StringType();
    public static final Type BOOL = new BoolType();
    public static final Type UNIT = new UnitType();
    public static final Type ARRAY = new ArrayType();
    public static final Type ANY = new AnyType();

    /**
     * Checks if this type is compatible with another type.
     * Compatibility includes exact matches and valid conversions.
     *
     * @param other Type to check compatibility with
     * @return true if types are compatible
     */
    public abstract boolean isCompatibleWith(Type other);

    /**
     * Returns the name of this type for display purposes.
     *
     * @return human-readable type name
     */
    public abstract String getTypeName();

    /**
     * Checks if this type is a numeric type (int or float).
     *
     * @return true if this is a numeric type
     */
    public boolean isNumeric() {
        return this == INT || this == FLOAT;
    }

    /**
     * Checks if this type is a primitive type.
     *
     * @return true if this is a primitive type
     */
    public boolean isPrimitive() {
        return this == INT || this == FLOAT || this == STRING || this == BOOL || this == UNIT;
    }

    /**
     * Attempts to find a common type between this type and another.
     * Used for type inference in expressions with multiple operands.
     *
     * @param other the other type
     * @return the common type, or null if no common type exists
     */
    public Type getCommonType(Type other) {
        if (this.equals(other)) {
            return this;
        }

        // Numeric promotion: int + float = float
        if ((this == INT && other == FLOAT) || (this == FLOAT && other == INT)) {
            return FLOAT;
        }

        return null;
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public String toString() {
        return getTypeName();
    }

    /**
     * Integer type representing 32-bit signed integers.
     */
    public static final class IntType extends Type {
        @Override
        public boolean isCompatibleWith(Type other) {
            // Int is compatible with int and float (for promotion)
            return other == INT || other == FLOAT;
        }

        @Override
        public String getTypeName() {
            return "int";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof IntType;
        }

        @Override
        public int hashCode() {
            return "int".hashCode();
        }
    }

    /**
     * Floating-point type representing 64-bit double precision numbers.
     */
    public static final class FloatType extends Type {
        @Override
        public boolean isCompatibleWith(Type other) {
            // Float is compatible with float and int (for demotion)
            return other == FLOAT || other == INT;
        }

        @Override
        public String getTypeName() {
            return "float";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FloatType;
        }

        @Override
        public int hashCode() {
            return "float".hashCode();
        }
    }

    /**
     * String type for text values.
     */
    public static final class StringType extends Type {
        @Override
        public boolean isCompatibleWith(Type other) {
            return other == STRING;
        }

        @Override
        public String getTypeName() {
            return "string";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof StringType;
        }

        @Override
        public int hashCode() {
            return "string".hashCode();
        }
    }

    /**
     * Boolean type for true/false values.
     */
    public static final class BoolType extends Type {
        @Override
        public boolean isCompatibleWith(Type other) {
            return other == BOOL;
        }

        @Override
        public String getTypeName() {
            return "bool";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof BoolType;
        }

        @Override
        public int hashCode() {
            return "bool".hashCode();
        }
    }

    /**
     * Unit type representing the absence of a meaningful value.
     * Used for statements and functions that don't return a value.
     */
    public static final class UnitType extends Type {
        @Override
        public boolean isCompatibleWith(Type other) {
            return other == UNIT;
        }

        @Override
        public String getTypeName() {
            return "unit";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof UnitType;
        }

        @Override
        public int hashCode() {
            return "unit".hashCode();
        }
    }

    /**
     * Array type representing collections of values.
     */
    public static final class ArrayType extends Type {
        @Override
        public boolean isCompatibleWith(Type other) {
            return other == ARRAY;
        }

        @Override
        public String getTypeName() {
            return "array";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ArrayType;
        }

        @Override
        public int hashCode() {
            return "array".hashCode();
        }
    }

    /**
     * Any type representing any value (used for generic functions).
     */
    public static final class AnyType extends Type {
        @Override
        public boolean isCompatibleWith(Type other) {
            return true;
        }

        @Override
        public String getTypeName() {
            return "any";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AnyType;
        }

        @Override
        public int hashCode() {
            return "any".hashCode();
        }
    }
}