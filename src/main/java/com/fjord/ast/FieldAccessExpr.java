package com.fjord.ast;

import com.fjord.lex.Token;
import com.fjord.runtime.Environment;
import com.fjord.runtime.StructValue;
import com.fjord.runtime.Value;

/**
 * AST node representing field access on a struct.
 * Example: person.name
 */
public class FieldAccessExpr extends ASTNode {
    private final Expression object;
    private final String fieldName;

    public FieldAccessExpr(Token token, Expression object, String fieldName) {
        super(token.getPosition());
        this.object = object;
        this.fieldName = fieldName;
    }

    public Expression getObject() {
        return object;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFieldAccess(this);
    }

    @Override
    public Value evaluate(Environment env) {
        Value objectValue = object.evaluate(env);

        if (!(objectValue instanceof StructValue)) {
            throw new RuntimeException("Cannot access field on non-struct value: " + objectValue);
        }

        StructValue structValue = (StructValue) objectValue;
        if (!structValue.hasField(fieldName)) {
            throw new RuntimeException("Struct '" + structValue.getTypeName() + "' has no field '" + fieldName + "'");
        }

        return structValue.getField(fieldName);
    }
}