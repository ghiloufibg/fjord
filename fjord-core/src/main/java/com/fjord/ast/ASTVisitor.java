package com.fjord.ast;

/**
 * Visitor interface for AST traversal operations.
 * Enables clean separation of concerns between AST structure and operations.
 * This follows the Visitor design pattern, allowing new operations to be added
 * without modifying the AST node classes.
 *
 * <p>The visitor pattern is particularly useful for:
 * <ul>
 *   <li>Type checking and semantic analysis</li>
 *   <li>Code generation and bytecode emission</li>
 *   <li>Pretty printing and formatting</li>
 *   <li>Optimization passes</li>
 *   <li>Static analysis and linting</li>
 * </ul>
 *
 * <p>Example implementation:
 * <pre>{@code
 * public class TypeChecker implements ASTVisitor<Type> {
 *     @Override
 *     public Type visitBinaryExpr(BinaryExpr expr) {
 *         Type leftType = expr.getLeft().accept(this);
 *         Type rightType = expr.getRight().accept(this);
 *         // ... perform type checking logic
 *         return resultType;
 *     }
 *
 *     // ... other visit methods
 * }
 * }</pre>
 *
 * @param <T> the type of result produced by visiting nodes
 */
public interface ASTVisitor<T> {

    /**
     * Visits a binary expression node.
     *
     * @param expr the binary expression to visit
     * @return the result of visiting this node
     */
    T visitBinaryExpr(BinaryExpr expr);

    /**
     * Visits a literal expression node.
     *
     * @param expr the literal expression to visit
     * @return the result of visiting this node
     */
    T visitLiteralExpr(LiteralExpr expr);

    /**
     * Visits a variable expression node.
     *
     * @param expr the variable expression to visit
     * @return the result of visiting this node
     */
    T visitVariableExpr(VariableExpr expr);

    /**
     * Visits a function definition expression node.
     *
     * @param expr the function definition to visit
     * @return the result of visiting this node
     */
    T visitFunctionDefExpr(FunctionDefExpr expr);

    /**
     * Visits a function call expression node.
     *
     * @param expr the function call to visit
     * @return the result of visiting this node
     */
    T visitCallExpr(CallExpr expr);

    /**
     * Visits a let expression node (variable declaration).
     *
     * @param expr the let expression to visit
     * @return the result of visiting this node
     */
    T visitLetExpr(LetExpr expr);

    /**
     * Visits a block expression node.
     *
     * @param expr the block expression to visit
     * @return the result of visiting this node
     */
    T visitBlockExpr(BlockExpr expr);

    /**
     * Visits an if expression node.
     *
     * @param expr the if expression to visit
     * @return the result of visiting this node
     */
    T visitIfExpr(IfExpr expr);

    /**
     * Visits a while expression node.
     *
     * @param expr the while expression to visit
     * @return the result of visiting this node
     */
    T visitWhileExpr(WhileExpr expr);

    /**
     * Visits an assignment expression node.
     *
     * @param expr the assignment expression to visit
     * @return the result of visiting this node
     */
    T visitAssignmentExpr(AssignmentExpr expr);

    /**
     * Visits an array literal expression node.
     *
     * @param expr the array literal expression to visit
     * @return the result of visiting this node
     */
    T visitArrayLiteral(ArrayLiteralExpr expr);

    /**
     * Visits an array index expression node.
     *
     * @param expr the index expression to visit
     * @return the result of visiting this node
     */
    T visitIndex(IndexExpr expr);

    /**
     * Visits a for loop expression node.
     *
     * @param expr the for expression to visit
     * @return the result of visiting this node
     */
    T visitFor(ForExpr expr);

    /**
     * Visits a struct definition expression node.
     *
     * @param expr the struct definition to visit
     * @return the result of visiting this node
     */
    T visitStructDef(StructDefExpr expr);

    /**
     * Visits a struct literal expression node.
     *
     * @param expr the struct literal to visit
     * @return the result of visiting this node
     */
    T visitStructLiteral(StructLiteralExpr expr);

    /**
     * Visits a field access expression node.
     *
     * @param expr the field access to visit
     * @return the result of visiting this node
     */
    T visitFieldAccess(FieldAccessExpr expr);

    /**
     * Visits a lambda expression node.
     *
     * @param expr the lambda expression to visit
     * @return the result of visiting this node
     */
    T visitLambda(LambdaExpr expr);

    /**
     * Base implementation of the visitor interface that provides default
     * behavior for all visit methods. Concrete visitors can extend this
     * class and override only the methods they need.
     *
     * @param <T> the type of result produced by visiting nodes
     */
    abstract class BaseVisitor<T> implements ASTVisitor<T> {

        /**
         * Default implementation that throws an exception.
         * Subclasses should override this method for nodes they handle.
         */
        protected T defaultVisit() {
            throw new UnsupportedOperationException("Visit method not implemented");
        }

        @Override
        public T visitBinaryExpr(BinaryExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitLiteralExpr(LiteralExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitVariableExpr(VariableExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitFunctionDefExpr(FunctionDefExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitCallExpr(CallExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitLetExpr(LetExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitBlockExpr(BlockExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitIfExpr(IfExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitWhileExpr(WhileExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitAssignmentExpr(AssignmentExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitArrayLiteral(ArrayLiteralExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitIndex(IndexExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitFor(ForExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitStructDef(StructDefExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitStructLiteral(StructLiteralExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitFieldAccess(FieldAccessExpr expr) {
            return defaultVisit();
        }

        @Override
        public T visitLambda(LambdaExpr expr) {
            return defaultVisit();
        }
    }
}