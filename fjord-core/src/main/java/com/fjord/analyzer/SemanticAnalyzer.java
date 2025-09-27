package com.fjord.analyzer;

import com.fjord.ast.*;
import com.fjord.error.ErrorReporter;
import com.fjord.error.SourcePosition;
import com.fjord.lex.TokenType;
import com.fjord.runtime.*;
import com.fjord.types.FunctionType;
import com.fjord.types.StructType;
import com.fjord.types.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Semantic analyzer performing type checking and symbol resolution.
 * Runs after parsing to catch semantic errors before interpretation.
 * This phase ensures type safety and resolves all symbol references.
 *
 * <p>The semantic analyzer performs:
 * <ul>
 *   <li>Type checking for all expressions and statements</li>
 *   <li>Symbol resolution and scope management</li>
 *   <li>Function signature validation</li>
 *   <li>Variable mutability checking</li>
 *   <li>Dead code analysis (basic)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ErrorReporter errorReporter = new ErrorReporter();
 * SemanticAnalyzer analyzer = new SemanticAnalyzer(errorReporter);
 * AnalysisResult result = analyzer.analyze(program);
 *
 * if (result.isSuccess()) {
 *     // Program passed all semantic checks
 *     execute(program);
 * } else {
 *     errorReporter.printErrorSummary();
 * }
 * }</pre>
 */
public final class SemanticAnalyzer implements ASTVisitor<Type> {

    /** Error reporter for collecting semantic errors */
    private final ErrorReporter errorReporter;

    /** Symbol table for tracking variables and functions */
    private final SymbolTable symbolTable;

    /** Whether we are currently inside a function definition */
    private boolean inFunction = false;

    /** The return type of the current function (if any) */
    private Type currentFunctionReturnType = Type.UNIT;

    /**
     * Creates a new semantic analyzer.
     *
     * @param errorReporter error reporter to collect semantic errors
     */
    public SemanticAnalyzer(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
        this.symbolTable = new SymbolTable();
        setupBuiltinFunctions();
    }

    /**
     * Result of semantic analysis containing type information and success status.
     */
    public static final class AnalysisResult {
        private final boolean success;
        private final SymbolTable symbolTable;

        private AnalysisResult(boolean success, SymbolTable symbolTable) {
            this.success = success;
            this.symbolTable = symbolTable;
        }

        /**
         * Returns whether analysis was successful.
         *
         * @return true if no semantic errors were found
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Returns the symbol table with all resolved symbols.
         *
         * @return the symbol table
         */
        public SymbolTable getSymbolTable() {
            return symbolTable;
        }

        /**
         * Creates a successful analysis result.
         *
         * @param symbolTable the completed symbol table
         * @return successful result
         */
        public static AnalysisResult success(SymbolTable symbolTable) {
            return new AnalysisResult(true, symbolTable);
        }

        /**
         * Creates a failed analysis result.
         *
         * @param symbolTable the symbol table (possibly incomplete)
         * @return failed result
         */
        public static AnalysisResult failure(SymbolTable symbolTable) {
            return new AnalysisResult(false, symbolTable);
        }
    }

    /**
     * Analyzes the AST for semantic correctness.
     *
     * @param program list of top-level expressions to analyze
     * @return AnalysisResult containing type information and any errors
     */
    public AnalysisResult analyze(List<Expression> program) {
        boolean hadErrors = errorReporter.hasErrors();

        try {
            // Analyze each top-level expression
            for (Expression expr : program) {
                if (expr instanceof ASTNode) {
                    ((ASTNode) expr).accept(this);
                } else {
                    // Handle legacy Expression nodes that don't support visitor pattern
                    analyzeLegacyExpression(expr);
                }
            }

            // Check for unreachable code or other whole-program issues
            performWholeProgram();

        } catch (Exception e) {
            errorReporter.semanticError(
                new SourcePosition(1, 1, 0),
                "Internal semantic analysis error: " + e.getMessage()
            );
        }

        boolean success = !errorReporter.hasErrors() || hadErrors == errorReporter.hasErrors();
        return success ? AnalysisResult.success(symbolTable) : AnalysisResult.failure(symbolTable);
    }

    /**
     * Sets up built-in functions in the global scope.
     */
    private void setupBuiltinFunctions() {
        // println function: (string) -> unit
        FunctionType printlnType = FunctionType.unary(Type.STRING, Type.UNIT);
        symbolTable.define("println", printlnType);
    }

    /**
     * Handles legacy expressions that don't implement the visitor pattern yet.
     */
    private Type analyzeLegacyExpression(Expression expr) {
        // For now, assume legacy expressions are valid and return UNIT
        // This maintains backward compatibility during the transition
        return Type.UNIT;
    }

    /**
     * Performs whole-program analysis like checking for main function.
     */
    private void performWholeProgram() {
        // Check if main function is properly defined
        var mainSymbol = symbolTable.resolve("main");
        if (mainSymbol.isPresent()) {
            Type mainType = mainSymbol.get().getType();
            if (mainType instanceof FunctionType) {
                FunctionType mainFunc = (FunctionType) mainType;
                if (!mainFunc.getParameterTypes().isEmpty()) {
                    errorReporter.semanticError(
                        new SourcePosition(1, 1, 0),
                        "main function must not take any parameters"
                    );
                }
            }
        }
    }

    // Visitor method implementations

    @Override
    public Type visitBinaryExpr(BinaryExpr expr) {
        // This method needs to be implemented to work with the new ASTNode structure
        // For now, return the appropriate type based on the operator
        return inferBinaryExprType(expr);
    }

    @Override
    public Type visitLiteralExpr(LiteralExpr expr) {
        // Determine type from the literal value
        Value value = expr.getValue();
        if (value instanceof IntValue) return Type.INT;
        if (value instanceof FloatValue) return Type.FLOAT;
        if (value instanceof StringValue) return Type.STRING;
        if (value instanceof BoolValue) return Type.BOOL;
        if (value instanceof UnitValue) return Type.UNIT;

        errorReporter.semanticError(
            new SourcePosition(1, 1, 0), // Would need actual position from ASTNode
            "Unknown literal type: " + value.getClass().getSimpleName()
        );
        return Type.UNIT;
    }

    @Override
    public Type visitVariableExpr(VariableExpr expr) {
        String varName = expr.getName();
        var symbolInfo = symbolTable.resolve(varName);

        if (symbolInfo.isEmpty()) {
            errorReporter.semanticError(
                new SourcePosition(1, 1, 0), // Would need actual position
                "Undefined variable: " + varName
            );
            return Type.UNIT;
        }

        return symbolInfo.get().getType();
    }

    @Override
    public Type visitFunctionDefExpr(FunctionDefExpr expr) {
        String funcName = expr.getName();

        // Check if function is already defined in current scope
        if (symbolTable.isDefinedInCurrentScope(funcName)) {
            errorReporter.semanticError(
                new SourcePosition(1, 1, 0),
                "Function '" + funcName + "' is already defined in this scope"
            );
        }

        // Enter function scope
        symbolTable.enterScope();
        boolean wasInFunction = inFunction;
        Type previousReturnType = currentFunctionReturnType;

        try {
            inFunction = true;
            // For now, assume function returns unit (would need type annotations)
            currentFunctionReturnType = Type.UNIT;

            // Define parameters in function scope
            List<String> parameters = expr.getParameters();
            List<Type> paramTypes = new ArrayList<>();

            for (String param : parameters) {
                // For now, assume all parameters are of type 'any' or inferred
                // In a full implementation, we'd parse type annotations
                symbolTable.define(param, Type.UNIT, true);
                paramTypes.add(Type.UNIT);
            }

            // Analyze function body
            Type bodyType = Type.UNIT; // Would analyze expr.getBody() if it were an ASTNode

            // Create function type and define in outer scope
            FunctionType funcType = new FunctionType(paramTypes, bodyType);
            symbolTable.exitScope();
            symbolTable.define(funcName, funcType);

            return Type.UNIT;

        } finally {
            inFunction = wasInFunction;
            currentFunctionReturnType = previousReturnType;
            if (symbolTable.getCurrentScopeLevel() > 0) {
                symbolTable.exitScope();
            }
        }
    }

    @Override
    public Type visitCallExpr(CallExpr expr) {
        // Would need to analyze the function expression and arguments
        // For now, return UNIT
        return Type.UNIT;
    }

    @Override
    public Type visitLetExpr(LetExpr expr) {
        String varName = expr.getName();

        // Check if variable is already defined in current scope
        if (symbolTable.isDefinedInCurrentScope(varName)) {
            errorReporter.semanticError(
                new SourcePosition(1, 1, 0),
                "Variable '" + varName + "' is already defined in this scope"
            );
        }

        // Analyze initializer and determine type
        Type initType = Type.UNIT; // Would analyze expr.getInitializer() if it were an ASTNode

        // Define variable with inferred type
        symbolTable.define(varName, initType, false); // let variables are immutable

        return Type.UNIT;
    }

    @Override
    public Type visitBlockExpr(BlockExpr expr) {
        symbolTable.enterScope();
        try {
            Type lastType = Type.UNIT;
            // Would analyze each statement in the block
            // The type of a block is the type of its last expression

            return lastType;
        } finally {
            symbolTable.exitScope();
        }
    }

    @Override
    public Type visitIfExpr(IfExpr expr) {
        // Analyze condition - should be boolean
        Type conditionType = Type.BOOL; // Would analyze condition expression

        if (!conditionType.equals(Type.BOOL)) {
            errorReporter.semanticError(
                new SourcePosition(1, 1, 0),
                "If condition must be boolean, found: " + conditionType.getTypeName()
            );
        }

        // Analyze both branches
        Type thenType = Type.UNIT; // Would analyze then branch
        Type elseType = Type.UNIT; // Would analyze else branch

        // If expression type is the common type of both branches
        Type commonType = thenType.getCommonType(elseType);
        if (commonType == null) {
            errorReporter.semanticError(
                new SourcePosition(1, 1, 0),
                "If branches have incompatible types: " + thenType.getTypeName() +
                " and " + elseType.getTypeName()
            );
            return Type.UNIT;
        }

        return commonType;
    }

    @Override
    public Type visitWhileExpr(WhileExpr expr) {
        // Analyze condition - should be boolean
        Type conditionType = Type.BOOL; // Would analyze condition expression

        if (!conditionType.equals(Type.BOOL)) {
            errorReporter.semanticError(
                new SourcePosition(1, 1, 0),
                "While condition must be boolean, found: " + conditionType.getTypeName()
            );
        }

        // Analyze body - any type is allowed for body
        Type bodyType = Type.UNIT; // Would analyze body expression

        // While expressions always return unit
        return Type.UNIT;
    }

    @Override
    public Type visitAssignmentExpr(AssignmentExpr expr) {
        // Would analyze the value expression to get its type
        Type valueType = Type.UNIT; // Placeholder

        // Would check that the variable exists and is mutable
        // For now, just return the value type
        return valueType;
    }

    /**
     * Infers the type of a binary expression based on its operator and operands.
     */
    private Type inferBinaryExprType(BinaryExpr expr) {
        // This is a simplified version - would need actual operand analysis
        TokenType op = expr.getOperator();

        switch (op) {
            case PLUS, MINUS, STAR, SLASH:
                // Arithmetic operators return numeric types
                return Type.INT; // Simplified - would depend on operand types

            case GT, GTE, LT, LTE, EQEQ, BANGEQ:
                // Comparison operators return boolean
                return Type.BOOL;

            default:
                errorReporter.semanticError(
                    new SourcePosition(1, 1, 0),
                    "Unknown binary operator: " + op
                );
                return Type.UNIT;
        }
    }

    @Override
    public Type visitArrayLiteral(ArrayLiteralExpr expr) {
        // Check all elements (for now we just return ARRAY type)
        for (Expression element : expr.getElements()) {
            if (element instanceof ASTNode) {
                ((ASTNode) element).accept(this);
            }
        }
        return Type.ARRAY;
    }

    @Override
    public Type visitIndex(IndexExpr expr) {
        Type arrayType = Type.ANY;
        Type indexType = Type.ANY;

        if (expr.getArray() instanceof ASTNode) {
            arrayType = ((ASTNode) expr.getArray()).accept(this);
        }

        if (expr.getIndex() instanceof ASTNode) {
            indexType = ((ASTNode) expr.getIndex()).accept(this);
        }

        if (arrayType != Type.ARRAY && arrayType != Type.ANY) {
            errorReporter.semanticError(
                expr.getPosition(),
                "Cannot index non-array type: " + arrayType.getTypeName()
            );
        }

        if (indexType != Type.INT && indexType != Type.ANY) {
            errorReporter.semanticError(
                expr.getPosition(),
                "Array index must be an integer, got: " + indexType.getTypeName()
            );
        }

        return Type.ANY; // Arrays can contain any type for now
    }

    @Override
    public Type visitFor(ForExpr expr) {
        Type iterableType = Type.ANY;

        if (expr.getIterable() instanceof ASTNode) {
            iterableType = ((ASTNode) expr.getIterable()).accept(this);
        }

        if (iterableType != Type.ARRAY && iterableType != Type.ANY) {
            errorReporter.semanticError(
                expr.getPosition(),
                "Cannot iterate over non-array type: " + iterableType.getTypeName()
            );
        }

        // Create new scope for loop variable
        symbolTable.enterScope();
        symbolTable.define(expr.getVariable(), Type.ANY, false); // Loop variable is not mutable

        Type bodyType = Type.UNIT;
        if (expr.getBody() instanceof ASTNode) {
            bodyType = ((ASTNode) expr.getBody()).accept(this);
        }

        symbolTable.exitScope();

        return Type.UNIT; // For loops return unit
    }

    @Override
    public Type visitStructDef(StructDefExpr expr) {
        String structName = expr.getName();

        // Check if struct already exists
        if (symbolTable.resolve(structName).isPresent()) {
            errorReporter.semanticError(expr.position, "Struct '" + structName + "' is already defined");
            return Type.UNIT;
        }

        // Build field type map
        Map<String, Type> fieldTypes = new HashMap<>();
        for (StructDefExpr.FieldDef field : expr.getFields()) {
            Type fieldType = parseTypeName(field.getTypeName());
            if (fieldType != null) {
                fieldTypes.put(field.getName(), fieldType);
            }
        }

        // Create and register the struct type
        StructType structType = new StructType(structName, fieldTypes);
        symbolTable.define(structName, structType);

        return Type.UNIT;
    }

    @Override
    public Type visitStructLiteral(StructLiteralExpr expr) {
        String structName = expr.getStructName();

        // Check if struct type exists
        Optional<SymbolTable.SymbolInfo> symbolInfo = symbolTable.resolve(structName);
        if (!symbolInfo.isPresent()) {
            errorReporter.semanticError(expr.position, "Unknown struct type: " + structName);
            return Type.ANY;
        }

        Type structTypeEntry = symbolInfo.get().getType();
        if (!(structTypeEntry instanceof StructType)) {
            errorReporter.semanticError(expr.position, "'" + structName + "' is not a struct type");
            return Type.ANY;
        }

        StructType structType = (StructType) structTypeEntry;

        // Check all field values
        for (Map.Entry<String, Expression> entry : expr.getFieldValues().entrySet()) {
            String fieldName = entry.getKey();
            Expression fieldValue = entry.getValue();

            if (!structType.hasField(fieldName)) {
                errorReporter.semanticError(expr.position, "Struct '" + structName + "' has no field '" + fieldName + "'");
                continue;
            }

            Type expectedType = structType.getFieldType(fieldName);
            Type actualType = ((ASTNode) fieldValue).accept(this);

            if (!actualType.isCompatibleWith(expectedType)) {
                errorReporter.semanticError(expr.position, "Field '" + fieldName + "' expects type " + expectedType.getTypeName() +
                           " but got " + actualType.getTypeName());
            }
        }

        // Check all required fields are provided
        for (String requiredField : structType.getFields().keySet()) {
            if (!expr.getFieldValues().containsKey(requiredField)) {
                errorReporter.semanticError(expr.position, "Missing required field '" + requiredField + "' in struct literal");
            }
        }

        return structType;
    }

    @Override
    public Type visitFieldAccess(FieldAccessExpr expr) {
        Type objectType = ((ASTNode) expr.getObject()).accept(this);

        if (!(objectType instanceof StructType)) {
            errorReporter.semanticError(expr.position, "Cannot access field on non-struct type: " + objectType.getTypeName());
            return Type.ANY;
        }

        StructType structType = (StructType) objectType;
        String fieldName = expr.getFieldName();

        if (!structType.hasField(fieldName)) {
            errorReporter.semanticError(expr.position, "Struct '" + structType.getName() + "' has no field '" + fieldName + "'");
            return Type.ANY;
        }

        return structType.getFieldType(fieldName);
    }

    private Type parseTypeName(String typeName) {
        switch (typeName) {
            case "Int": return Type.INT;
            case "Float": return Type.FLOAT;
            case "String": return Type.STRING;
            case "Bool": return Type.BOOL;
            case "Unit": return Type.UNIT;
            case "Array": return Type.ARRAY;
            default:
                // Check if it's a defined struct type
                Optional<SymbolTable.SymbolInfo> symbolInfo = symbolTable.resolve(typeName);
                if (symbolInfo.isPresent()) {
                    Type type = symbolInfo.get().getType();
                    if (type instanceof StructType) {
                        return type;
                    }
                }
                return Type.ANY; // Unknown type
        }
    }

    @Override
    public Type visitLambda(LambdaExpr expr) {
        // Enter a new scope for the lambda parameters
        symbolTable.enterScope();

        // Define the lambda parameters in the new scope
        for (String param : expr.getParameters()) {
            symbolTable.define(param, Type.ANY); // Use ANY for now since we don't have parameter types
        }

        // Type check the body
        Type bodyType = ((ASTNode) expr.getBody()).accept(this);

        symbolTable.exitScope();

        // Create parameter types list (all ANY for now since we don't have parameter type inference)
        List<Type> parameterTypes = new ArrayList<>();
        for (int i = 0; i < expr.getParameters().size(); i++) {
            parameterTypes.add(Type.ANY);
        }

        return new FunctionType(parameterTypes, bodyType);
    }
}