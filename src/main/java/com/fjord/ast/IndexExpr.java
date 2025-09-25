package com.fjord.ast;

import com.fjord.error.SourcePosition;
import com.fjord.runtime.ArrayValue;
import com.fjord.runtime.Environment;
import com.fjord.runtime.IntValue;
import com.fjord.runtime.Value;

/**
 * Represents an array indexing expression in the abstract syntax tree.
 * Index expressions access elements of arrays using square bracket notation,
 * e.g., array[0] or items[i + 1].
 */
public final class IndexExpr extends ASTNode implements Expression {

    private final Expression array;
    private final Expression index;

    /**
     * Constructs a new index expression.
     *
     * @param array the expression that evaluates to an array
     * @param index the expression that evaluates to the index
     */
    public IndexExpr(Expression array, Expression index) {
        super(null); // TODO: Pass actual source position from parser
        this.array = array;
        this.index = index;
    }

    @Override
    public Value evaluate(Environment env) {
        Value arrayValue = array.evaluate(env);
        Value indexValue = index.evaluate(env);

        if (!(arrayValue instanceof ArrayValue)) {
            throw new RuntimeException("Cannot index non-array value");
        }

        if (!(indexValue instanceof IntValue)) {
            throw new RuntimeException("Array index must be an integer");
        }

        ArrayValue arr = (ArrayValue) arrayValue;
        int idx = (Integer) indexValue.getValue();

        if (idx < 0 || idx >= arr.size()) {
            throw new RuntimeException("Array index out of bounds: " + idx);
        }

        return arr.get(idx);
    }

    /**
     * Returns the array expression.
     *
     * @return the array expression
     */
    public Expression getArray() {
        return array;
    }

    /**
     * Returns the index expression.
     *
     * @return the index expression
     */
    public Expression getIndex() {
        return index;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitIndex(this);
    }
}