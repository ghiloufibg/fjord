package com.fjord.ast;

import com.fjord.error.SourcePosition;
import com.fjord.lex.Token;
import com.fjord.runtime.Environment;
import com.fjord.runtime.FunctionValue;
import com.fjord.runtime.Value;
import java.util.List;

/**
 * AST node representing a lambda expression (anonymous function).
 * Example: fn(x, y) -> x + y
 */
public class LambdaExpr extends ASTNode {
    private final List<String> parameters;
    private final Expression body;

    public LambdaExpr(Token token, List<String> parameters, Expression body) {
        super(token.getPosition());
        this.parameters = parameters;
        this.body = body;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public Expression getBody() {
        return body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLambda(this);
    }

    @Override
    public Value evaluate(Environment env) {
        // Create a function value that captures the current environment
        return new FunctionValue(parameters, body, env);
    }
}