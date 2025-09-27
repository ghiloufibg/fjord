package com.fjord.runtime;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents an array value in the Fjord runtime system.
 * Arrays are ordered collections of values that can be accessed by index.
 */
public final class ArrayValue implements Value {

    private final List<Value> elements;

    /**
     * Constructs a new array value.
     *
     * @param elements the list of values that form the array elements
     */
    public ArrayValue(List<Value> elements) {
        this.elements = new ArrayList<>(elements);
    }

    /**
     * Returns the element at the specified index.
     *
     * @param index the index of the element to return
     * @return the element at the specified index
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public Value get(int index) {
        return elements.get(index);
    }

    /**
     * Returns the number of elements in this array.
     *
     * @return the size of the array
     */
    public int size() {
        return elements.size();
    }

    /**
     * Returns a copy of the elements list.
     *
     * @return a list containing all elements
     */
    public List<Value> getElements() {
        return new ArrayList<>(elements);
    }

    /**
     * Adds an element to the end of this array.
     *
     * @param value the value to add
     */
    public void add(Value value) {
        elements.add(value);
    }

    /**
     * Sets the element at the specified index.
     *
     * @param index the index to set
     * @param value the value to set
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public void set(int index, Value value) {
        elements.set(index, value);
    }

    @Override
    public Object getValue() {
        return elements;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(elements.get(i).toString());
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || this.getClass() != obj.getClass()) return false;
        ArrayValue that = (ArrayValue) obj;
        return elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }
}