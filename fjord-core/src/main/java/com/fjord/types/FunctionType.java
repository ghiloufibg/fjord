package com.fjord.types;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents function types in the Fjord type system.
 * Function types have the form (T1, T2, ..., Tn) -> TReturn.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Function type: (int, int) -> int
 * FunctionType addType = new FunctionType(List.of(Type.INT, Type.INT), Type.INT);
 *
 * // Function type: () -> string
 * FunctionType helloType = new FunctionType(List.of(), Type.STRING);
 * }</pre>
 */
public final class FunctionType extends Type {

    /** Parameter types in order */
    private final List<Type> parameterTypes;

    /** Return type */
    private final Type returnType;

    /**
     * Creates a function type with the given parameter and return types.
     *
     * @param parameterTypes ordered list of parameter types
     * @param returnType     the return type
     */
    public FunctionType(List<Type> parameterTypes, Type returnType) {
        this.parameterTypes = List.copyOf(parameterTypes);
        this.returnType = Objects.requireNonNull(returnType, "Return type cannot be null");
    }

    /**
     * Returns the parameter types of this function.
     *
     * @return unmodifiable list of parameter types
     */
    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Returns the return type of this function.
     *
     * @return the return type
     */
    public Type getReturnType() {
        return returnType;
    }

    /**
     * Returns the number of parameters this function accepts.
     *
     * @return parameter count
     */
    public int getParameterCount() {
        return parameterTypes.size();
    }

    @Override
    public boolean isCompatibleWith(Type other) {
        if (!(other instanceof FunctionType)) {
            return false;
        }

        FunctionType otherFunc = (FunctionType) other;

        // Function types are compatible if they have the same parameter count,
        // compatible parameter types, and compatible return types
        if (this.parameterTypes.size() != otherFunc.parameterTypes.size()) {
            return false;
        }

        // Check parameter type compatibility
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (!parameterTypes.get(i).isCompatibleWith(otherFunc.parameterTypes.get(i))) {
                return false;
            }
        }

        // Check return type compatibility
        return returnType.isCompatibleWith(otherFunc.returnType);
    }

    @Override
    public String getTypeName() {
        if (parameterTypes.isEmpty()) {
            return "() -> " + returnType.getTypeName();
        }

        String params = parameterTypes.stream()
                .map(Type::getTypeName)
                .collect(Collectors.joining(", "));

        return "(" + params + ") -> " + returnType.getTypeName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FunctionType that = (FunctionType) obj;
        return Objects.equals(parameterTypes, that.parameterTypes) &&
               Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterTypes, returnType);
    }

    /**
     * Creates a function type with no parameters.
     *
     * @param returnType the return type
     * @return function type with no parameters
     */
    public static FunctionType nullary(Type returnType) {
        return new FunctionType(List.of(), returnType);
    }

    /**
     * Creates a function type with one parameter.
     *
     * @param paramType  the parameter type
     * @param returnType the return type
     * @return unary function type
     */
    public static FunctionType unary(Type paramType, Type returnType) {
        return new FunctionType(List.of(paramType), returnType);
    }

    /**
     * Creates a function type with two parameters.
     *
     * @param param1Type the first parameter type
     * @param param2Type the second parameter type
     * @param returnType the return type
     * @return binary function type
     */
    public static FunctionType binary(Type param1Type, Type param2Type, Type returnType) {
        return new FunctionType(List.of(param1Type, param2Type), returnType);
    }
}