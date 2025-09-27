package com.fjord.ast;

import com.fjord.lex.TokenType;
import com.fjord.runtime.BoolValue;
import com.fjord.runtime.Environment;
import com.fjord.runtime.FloatValue;
import com.fjord.runtime.IntValue;
import com.fjord.runtime.StringValue;
import com.fjord.runtime.Value;

/**
 * Represents a binary operation between two expressions. The operator
 * determines the semantics of the evaluation (arithmetic, comparison,
 * equality, etc.). Operands are evaluated from left to right.
 */
public final class BinaryExpr implements Expression {

    private final Expression left;
    private final TokenType operator;
    private final Expression right;

    /**
     * Constructs a new binary expression.
     *
     * @param left     the left operand
     * @param operator the operator token
     * @param right    the right operand
     */
    public BinaryExpr(Expression left, TokenType operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public Value evaluate(Environment env) {
        Value lVal = left.evaluate(env);
        Value rVal = right.evaluate(env);
        switch (operator) {
            case PLUS:
                return plus(lVal, rVal);
            case MINUS:
                return subtract(lVal, rVal);
            case STAR:
                return multiply(lVal, rVal);
            case SLASH:
                return divide(lVal, rVal);
            case PERCENT:
                return modulo(lVal, rVal);
            case GT:
                return compare(lVal, rVal, (c) -> c > 0);
            case GTE:
                return compare(lVal, rVal, (c) -> c >= 0);
            case LT:
                return compare(lVal, rVal, (c) -> c < 0);
            case LTE:
                return compare(lVal, rVal, (c) -> c <= 0);
            case EQEQ:
                return new BoolValue(equalsValues(lVal, rVal));
            case BANGEQ:
                return new BoolValue(!equalsValues(lVal, rVal));
            default:
                throw new IllegalStateException("Unsupported operator: " + operator);
        }
    }

    /**
     * Returns the left operand.
     *
     * @return the left expression
     */
    public Expression getLeft() {
        return left;
    }

    /**
     * Returns the right operand.
     *
     * @return the right expression
     */
    public Expression getRight() {
        return right;
    }

    /**
     * Returns the operator token type.
     *
     * @return the operator
     */
    public TokenType getOperator() {
        return operator;
    }

    private Value plus(Value a, Value b) {
        // If either is string, perform concatenation using toString
        if (a instanceof StringValue || b instanceof StringValue) {
            return new StringValue(a.getValue().toString() + b.getValue().toString());
        }
        if (a instanceof FloatValue || b instanceof FloatValue) {
            double x = asDouble(a);
            double y = asDouble(b);
            return new FloatValue(x + y);
        }
        // treat as int
        int x = asInt(a);
        int y = asInt(b);
        return new IntValue(x + y);
    }

    private Value subtract(Value a, Value b) {
        if (a instanceof FloatValue || b instanceof FloatValue) {
            return new FloatValue(asDouble(a) - asDouble(b));
        }
        return new IntValue(asInt(a) - asInt(b));
    }

    private Value multiply(Value a, Value b) {
        if (a instanceof FloatValue || b instanceof FloatValue) {
            return new FloatValue(asDouble(a) * asDouble(b));
        }
        return new IntValue(asInt(a) * asInt(b));
    }

    private Value divide(Value a, Value b) {
        if (a instanceof FloatValue || b instanceof FloatValue) {
            return new FloatValue(asDouble(a) / asDouble(b));
        }
        int divisor = asInt(b);
        return new IntValue(asInt(a) / divisor);
    }

    private Value modulo(Value a, Value b) {
        if (a instanceof FloatValue || b instanceof FloatValue) {
            return new FloatValue(asDouble(a) % asDouble(b));
        }
        int divisor = asInt(b);
        return new IntValue(asInt(a) % divisor);
    }

    private Value compare(Value a, Value b, java.util.function.IntPredicate predicate) {
        int cmp;
        if (a instanceof FloatValue || b instanceof FloatValue) {
            cmp = Double.compare(asDouble(a), asDouble(b));
        } else if (a instanceof StringValue || b instanceof StringValue) {
            cmp = a.getValue().toString().compareTo(b.getValue().toString());
        } else {
            cmp = Integer.compare(asInt(a), asInt(b));
        }
        return new BoolValue(predicate.test(cmp));
    }

    private boolean equalsValues(Value a, Value b) {
        Object x = a.getValue();
        Object y = b.getValue();
        return x == null ? y == null : x.equals(y);
    }

    private int asInt(Value v) {
        if (v instanceof IntValue) {
            return ((IntValue) v).asInt();
        }
        if (v instanceof FloatValue) {
            return (int) ((FloatValue) v).asDouble();
        }
        if (v instanceof BoolValue) {
            return ((BoolValue) v).asBoolean() ? 1 : 0;
        }
        throw new IllegalStateException("Cannot convert to int: " + v);
    }

    private double asDouble(Value v) {
        if (v instanceof FloatValue) {
            return ((FloatValue) v).asDouble();
        }
        if (v instanceof IntValue) {
            return ((IntValue) v).asInt();
        }
        if (v instanceof BoolValue) {
            return ((BoolValue) v).asBoolean() ? 1.0 : 0.0;
        }
        throw new IllegalStateException("Cannot convert to double: " + v);
    }
}