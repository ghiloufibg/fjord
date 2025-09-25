package com.fjord.ast;

import com.fjord.error.SourcePosition;
import com.fjord.lex.Token;
import com.fjord.runtime.Environment;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;
import java.util.List;

/**
 * AST node representing a struct definition.
 * Example: struct Person { name: String, age: Int }
 */
public class StructDefExpr extends ASTNode {
    private final String name;
    private final List<FieldDef> fields;

    public StructDefExpr(Token token, String name, List<FieldDef> fields) {
        super(token.getPosition());
        this.name = name;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public List<FieldDef> getFields() {
        return fields;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitStructDef(this);
    }

    @Override
    public Value evaluate(Environment env) {
        // Struct definitions don't produce a value when evaluated
        return UnitValue.INSTANCE;
    }

    /**
     * Represents a field definition in a struct.
     */
    public static class FieldDef {
        private final String name;
        private final String typeName;

        public FieldDef(String name, String typeName) {
            this.name = name;
            this.typeName = typeName;
        }

        public String getName() {
            return name;
        }

        public String getTypeName() {
            return typeName;
        }
    }
}