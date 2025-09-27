package com.fjord.ast;

import com.fjord.error.SourcePosition;
import com.fjord.runtime.ArrayValue;
import com.fjord.runtime.Environment;
import com.fjord.runtime.Value;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents an array literal expression in the abstract syntax tree.
 * Array literals are enclosed in square brackets and contain zero or more
 * expressions separated by commas, e.g., [1, 2, 3] or ["hello", "world"].
 */
public final class ArrayLiteralExpr extends ASTNode implements Expression {

    private final List<Expression> elements;

    /**
     * Constructs a new array literal expression.
     *
     * @param elements the list of expressions that form the array elements
     */
    public ArrayLiteralExpr(List<Expression> elements) {
        super(null); // TODO: Pass actual source position from parser
        this.elements = elements;
    }

    @Override
    public Value evaluate(Environment env) {
        List<Value> values = new ArrayList<>();
        for (Expression element : elements) {
            values.add(element.evaluate(env));
        }
        return new ArrayValue(values);
    }

    /**
     * Returns the list of element expressions.
     *
     * @return the element expressions
     */
    public List<Expression> getElements() {
        return elements;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitArrayLiteral(this);
    }
}