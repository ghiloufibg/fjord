package com.fjord.ast;

import com.fjord.lex.Token;
import com.fjord.runtime.Environment;
import com.fjord.runtime.StructValue;
import com.fjord.runtime.Value;
import java.util.HashMap;
import java.util.Map;

/**
 * AST node representing a struct literal/instantiation.
 * Example: Person { name: "John", age: 30 }
 */
public class StructLiteralExpr extends ASTNode {
    private final String structName;
    private final Map<String, Expression> fieldValues;

    public StructLiteralExpr(Token token, String structName, Map<String, Expression> fieldValues) {
        super(token.getPosition());
        this.structName = structName;
        this.fieldValues = fieldValues;
    }

    public String getStructName() {
        return structName;
    }

    public Map<String, Expression> getFieldValues() {
        return fieldValues;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitStructLiteral(this);
    }

    @Override
    public Value evaluate(Environment env) {
        // Evaluate all field expressions
        Map<String, Value> evaluatedFields = new HashMap<>();
        for (Map.Entry<String, Expression> entry : fieldValues.entrySet()) {
            String fieldName = entry.getKey();
            Expression fieldExpr = entry.getValue();
            Value fieldValue = fieldExpr.evaluate(env);
            evaluatedFields.put(fieldName, fieldValue);
        }

        return new StructValue(structName, evaluatedFields);
    }
}