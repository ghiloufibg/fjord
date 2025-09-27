package com.fjord.runtime;

import java.util.Map;
import java.util.Objects;

/**
 * Runtime value representing a struct instance.
 */
public class StructValue implements Value {
    private final String typeName;
    private final Map<String, Value> fields;

    public StructValue(String typeName, Map<String, Value> fields) {
        this.typeName = typeName;
        this.fields = fields;
    }

    public String getTypeName() {
        return typeName;
    }

    public Map<String, Value> getFields() {
        return fields;
    }

    public Value getField(String fieldName) {
        return fields.get(fieldName);
    }

    public void setField(String fieldName, Value value) {
        fields.put(fieldName, value);
    }

    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    @Override
    public Object getValue() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StructValue that = (StructValue) obj;
        return Objects.equals(typeName, that.typeName) && Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, fields);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(typeName).append(" { ");
        boolean first = true;
        for (Map.Entry<String, Value> entry : fields.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append(": ").append(entry.getValue().toString());
            first = false;
        }
        sb.append(" }");
        return sb.toString();
    }
}