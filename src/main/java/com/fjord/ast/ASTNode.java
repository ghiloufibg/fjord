package com.fjord.ast;

import com.fjord.error.SourcePosition;
import com.fjord.runtime.Environment;
import com.fjord.runtime.Value;

/**
 * Base AST node with visitor support for extensible operations.
 * All AST nodes should inherit from this class to support the visitor pattern
 * while maintaining backward compatibility with the existing evaluation system.
 *
 * <p>This class provides:
 * <ul>
 *   <li>Source position tracking for better error reporting</li>
 *   <li>Visitor pattern support for extensible operations</li>
 *   <li>Backward compatibility with existing evaluation</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MyExpr extends ASTNode {
 *     public MyExpr(SourcePosition position) {
 *         super(position);
 *     }
 *
 *     @Override
 *     public <T> T accept(ASTVisitor<T> visitor) {
 *         // Dispatch to appropriate visitor method
 *         return visitor.visitMyExpr(this);
 *     }
 *
 *     @Override
 *     public Value evaluate(Environment env) {
 *         // Existing evaluation logic
 *         return someValue;
 *     }
 * }
 * }</pre>
 */
public abstract class ASTNode implements Expression {

    /** Source position where this node was parsed from */
    public final SourcePosition position;

    /**
     * Creates a new AST node with the given source position.
     *
     * @param position the source position of this node
     */
    protected ASTNode(SourcePosition position) {
        this.position = position;
    }

    /**
     * Accepts a visitor and dispatches to the appropriate visit method.
     * This is the core of the visitor pattern implementation.
     *
     * @param visitor the visitor to accept
     * @param <T>     the type of result produced by the visitor
     * @return the result of the visitor operation
     */
    public abstract <T> T accept(ASTVisitor<T> visitor);

    /**
     * Returns the source position of this node.
     * Useful for error reporting and debugging.
     *
     * @return the source position
     */
    public SourcePosition getPosition() {
        return position;
    }

    /**
     * Evaluates this node in the given environment.
     * This maintains backward compatibility with the existing interpreter.
     *
     * @param env the environment providing variable and function bindings
     * @return the resulting {@link Value} of the evaluation
     */
    @Override
    public abstract Value evaluate(Environment env);
}