package com.fjord.types;

import java.util.Map;
import java.util.Objects;

/**
 * Type representing a struct with named fields.
 */
public class StructType extends Type {
    private final String name;
    private final Map<String, Type> fields;

    public StructType(String name, Map<String, Type> fields) {
        this.name = name;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public Map<String, Type> getFields() {
        return fields;
    }

    public Type getFieldType(String fieldName) {
        return fields.get(fieldName);
    }

    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    @Override
    public boolean isCompatibleWith(Type other) {
        if (other instanceof StructType) {
            StructType otherStruct = (StructType) other;
            return this.name.equals(otherStruct.name);
        }
        return false;
    }

    @Override
    public String getTypeName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StructType that = (StructType) obj;
        return Objects.equals(name, that.name) && Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fields);
    }

    @Override
    public String toString() {
        return "struct " + name;
    }
}