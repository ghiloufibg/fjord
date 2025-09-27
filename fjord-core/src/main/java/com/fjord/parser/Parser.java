package com.fjord.parser;

import com.fjord.ast.*;
import com.fjord.lex.Token;
import com.fjord.lex.TokenType;
import com.fjord.runtime.BoolValue;
import com.fjord.runtime.FloatValue;
import com.fjord.runtime.IntValue;
import com.fjord.runtime.StringValue;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs syntactic analysis of a linear token stream to produce an
 * abstract syntax tree. The grammar supported by this parser is a
 * simplified subset of the Fjord language as described in the project
 * README. It supports immutable declarations, function definitions,
 * conditional expressions, blocks, arithmetic and comparison operators,
 * and function calls.
 */
public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    /**
     * Constructs a new parser for the given list of tokens.
     *
     * @param tokens the token stream to parse
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Parses a sequence of top‑level statements until EOF. Top‑level
     * statements may be function definitions, immutable declarations or
     * expressions. A list of expressions is returned; function definitions
     * and declarations evaluate to the unit value.
     *
     * @return a list of {@link Expression}s representing the program
     */
    public List<Expression> parseProgram() {
        List<Expression> program = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            if (match(TokenType.FN)) {
                program.add(parseFunctionDefinition());
            } else if (match(TokenType.LET)) {
                program.add(parseLetDeclaration());
            } else if (match(TokenType.STRUCT)) {
                program.add(parseStructDefinition());
            } else {
                program.add(parseExpression());
            }

            // Handle optional semicolon separators
            if (match(TokenType.SEMICOLON)) {
                // Continue to next statement after semicolon
                continue;
            }

            // If no semicolon, check if we're at EOF
            // If not at EOF and no semicolon, there might be a syntax error,
            // but we'll let the next iteration handle it
            if (!check(TokenType.EOF)) {
                // Only continue if there's clearly another statement starting
                // This prevents infinite loops when encountering unexpected tokens
                TokenType nextType = peek().getType();
                if (nextType != TokenType.FN && nextType != TokenType.LET && nextType != TokenType.STRUCT &&
                    nextType != TokenType.IDENTIFIER && nextType != TokenType.INTEGER_LITERAL &&
                    nextType != TokenType.FLOAT_LITERAL && nextType != TokenType.STRING_LITERAL &&
                    nextType != TokenType.BOOL_LITERAL && nextType != TokenType.LPAREN &&
                    nextType != TokenType.LBRACE && nextType != TokenType.IF && nextType != TokenType.WHILE &&
                    nextType != TokenType.FOR && nextType != TokenType.LBRACKET) {
                    // Unexpected token - break to avoid infinite loop
                    break;
                }
            }
        }
        return program;
    }

    /**
     * Parses a function definition. Assumes the leading {@code fn} keyword
     * has already been consumed.
     */
    private Expression parseFunctionDefinition() {
        Token nameTok = consume(TokenType.IDENTIFIER, "Expected function name");
        consume(TokenType.LPAREN, "Expected '(' after function name");
        // parse parameters: name [: type] separated by commas
        List<String> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token paramName = consume(TokenType.IDENTIFIER, "Expected parameter name");
                // optional type annotation
                if (match(TokenType.COLON)) {
                    // skip over type identifier
                    consume(TokenType.IDENTIFIER, "Expected type name after ':'");
                }
                parameters.add(paramName.getLexeme());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expected ')' after parameter list");
        // optional return type
        if (match(TokenType.COLON)) {
            // skip type name
            consume(TokenType.IDENTIFIER, "Expected return type after ':'");
        }
        consume(TokenType.EQUAL, "Expected '=' after function signature");
        Expression body = parseExpression();
        return new FunctionDefExpr(nameTok.getLexeme(), parameters, body);
    }

    /**
     * Parses a variable declaration. Assumes the leading {@code let}
     * keyword has already been consumed.
     */
    private Expression parseLetDeclaration() {
        boolean isMutable = false;
        if (match(TokenType.MUT)) {
            isMutable = true;
        }
        Token nameTok = consume(TokenType.IDENTIFIER, "Expected variable name after 'let' or 'let mut'");
        String typeName = null;
        if (match(TokenType.COLON)) {
            Token typeTok = consume(TokenType.IDENTIFIER, "Expected type name after ':'");
            typeName = typeTok.getLexeme();
        }
        consume(TokenType.EQUAL, "Expected '=' after variable name");
        Expression init = parseExpression();
        return new LetExpr(nameTok.getLexeme(), typeName, init, isMutable);
    }

    /**
     * Parses an expression using a precedence climbing algorithm. Lowest
     * precedence is handled last.
     */
    private Expression parseExpression() {
        return parseAssignment();
    }

    private Expression parseAssignment() {
        Expression expr = parseEquality();

        if (match(TokenType.EQUAL)) {
            Expression value = parseAssignment(); // Right associative
            if (expr instanceof VariableExpr) {
                String name = ((VariableExpr) expr).getName();
                return new AssignmentExpr(name, value);
            }
            throw error(previous(), "Invalid assignment target");
        }

        return expr;
    }

    private Expression parseEquality() {
        Expression expr = parseComparison();
        while (match(TokenType.EQEQ) || match(TokenType.BANGEQ)) {
            TokenType operator = previous().getType();
            Expression right = parseComparison();
            expr = new BinaryExpr(expr, operator, right);
        }
        return expr;
    }

    private Expression parseComparison() {
        Expression expr = parseTerm();
        while (match(TokenType.GT) || match(TokenType.GTE) || match(TokenType.LT) || match(TokenType.LTE)) {
            TokenType operator = previous().getType();
            Expression right = parseTerm();
            expr = new BinaryExpr(expr, operator, right);
        }
        return expr;
    }

    private Expression parseTerm() {
        Expression expr = parseFactor();
        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            TokenType operator = previous().getType();
            Expression right = parseFactor();
            expr = new BinaryExpr(expr, operator, right);
        }
        return expr;
    }

    private Expression parseFactor() {
        Expression expr = parseUnary();
        while (match(TokenType.STAR) || match(TokenType.SLASH) || match(TokenType.PERCENT)) {
            TokenType operator = previous().getType();
            Expression right = parseUnary();
            expr = new BinaryExpr(expr, operator, right);
        }
        return expr;
    }

    private Expression parseUnary() {
        if (match(TokenType.MINUS)) {
            // unary minus: negate numeric operand
            TokenType operator = previous().getType();
            Expression right = parseUnary();
            // represent unary minus as 0 - expr
            return new BinaryExpr(new LiteralExpr(new IntValue(0)), operator, right);
        }
        return parseCall();
    }

    private Expression parseCall() {
        Expression expr = parsePrimary();
        while (true) {
            if (match(TokenType.LPAREN)) {
                List<Expression> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "Expected ')' after arguments");
                expr = new CallExpr(expr, args);
            } else if (match(TokenType.LBRACKET)) {
                Expression index = parseExpression();
                consume(TokenType.RBRACKET, "Expected ']' after array index");
                expr = new IndexExpr(expr, index);
            } else if (match(TokenType.DOT)) {
                Token fieldName = consume(TokenType.IDENTIFIER, "Expected field name after '.'");
                expr = new FieldAccessExpr(previous(), expr, fieldName.getLexeme());
            } else {
                break;
            }
        }
        return expr;
    }

    private Expression parsePrimary() {
        if (match(TokenType.INTEGER_LITERAL)) {
            String lexeme = previous().getLexeme();
            return new LiteralExpr(new IntValue(Integer.parseInt(lexeme)));
        }
        if (match(TokenType.FLOAT_LITERAL)) {
            String lexeme = previous().getLexeme();
            return new LiteralExpr(new FloatValue(Double.parseDouble(lexeme)));
        }
        if (match(TokenType.STRING_LITERAL)) {
            String lexeme = previous().getLexeme();
            return new LiteralExpr(new StringValue(lexeme));
        }
        if (match(TokenType.BOOL_LITERAL)) {
            String lexeme = previous().getLexeme();
            boolean b = lexeme.equals("true");
            return new LiteralExpr(new BoolValue(b));
        }
        if (match(TokenType.IDENTIFIER)) {
            String identifier = previous().getLexeme();

            // Check if this is a struct literal: Identifier { ... }
            if (check(TokenType.LBRACE)) {
                advance(); // consume '{'
                Map<String, Expression> fieldValues = new HashMap<>();

                if (!check(TokenType.RBRACE)) {
                    do {
                        Token fieldName = consume(TokenType.IDENTIFIER, "Expected field name");
                        consume(TokenType.COLON, "Expected ':' after field name");
                        Expression value = parseExpression();
                        fieldValues.put(fieldName.getLexeme(), value);
                    } while (match(TokenType.COMMA));
                }

                consume(TokenType.RBRACE, "Expected '}' after struct fields");
                return new StructLiteralExpr(previous(), identifier, fieldValues);
            }

            // Otherwise it's just a variable expression
            return new VariableExpr(identifier);
        }
        if (match(TokenType.LPAREN)) {
            Expression expr = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after expression");
            return expr;
        }
        if (match(TokenType.LBRACE)) {
            return parseBlock();
        }
        if (match(TokenType.IF)) {
            return parseIf();
        }
        if (match(TokenType.WHILE)) {
            return parseWhile();
        }
        if (match(TokenType.FOR)) {
            return parseFor();
        }
        if (match(TokenType.LBRACKET)) {
            return parseArrayLiteral();
        }
        if (match(TokenType.FN)) {
            return parseLambda();
        }
        throw error(peek(), "Unexpected token: " + peek().getType());
    }

    private Expression parseIf() {
        Expression condition = parseExpression();
        consume(TokenType.THEN, "Expected 'then' after if condition");
        Expression thenBranch = parseExpression();
        consume(TokenType.ELSE, "Expected 'else' after then branch");
        Expression elseBranch = parseExpression();
        return new IfExpr(condition, thenBranch, elseBranch);
    }

    private Expression parseWhile() {
        Expression condition = parseExpression();
        consume(TokenType.DO, "Expected 'do' after while condition");
        Expression body = parseExpression();
        return new WhileExpr(condition, body);
    }

    private Expression parseFor() {
        Token variable = consume(TokenType.IDENTIFIER, "Expected variable name after 'for'");
        consume(TokenType.IN, "Expected 'in' after for variable");
        Expression iterable = parseExpression();
        consume(TokenType.DO, "Expected 'do' after for iterable");
        Expression body = parseExpression();
        return new ForExpr(variable.getLexeme(), iterable, body);
    }

    private Expression parseArrayLiteral() {
        List<Expression> elements = new ArrayList<>();
        if (!check(TokenType.RBRACKET)) {
            do {
                elements.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RBRACKET, "Expected ']' after array elements");
        return new ArrayLiteralExpr(elements);
    }

    private Expression parseBlock() {
        List<Expression> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            if (match(TokenType.SEMICOLON)) {
                continue; // skip empty statements
            }
            if (match(TokenType.LET)) {
                statements.add(parseLetDeclaration());
            } else if (match(TokenType.FN)) {
                statements.add(parseFunctionDefinition());
            } else {
                statements.add(parseExpression());
            }
            if (match(TokenType.SEMICOLON)) {
                continue;
            }
        }
        consume(TokenType.RBRACE, "Expected '}' to close block");
        return new BlockExpr(statements);
    }

    // Utility parsing methods

    /** Returns true and consumes the token if it matches any of the given types. */
    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    /** Returns true if the current token has the given type. */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    /** Consumes and returns the current token if it matches the expected type. */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    /**
     * Parses a struct definition. Assumes the leading 'struct' keyword
     * has already been consumed.
     * Grammar: struct Name { field1: Type1, field2: Type2, ... }
     */
    private Expression parseStructDefinition() {
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected struct name");
        String structName = nameToken.getLexeme();

        consume(TokenType.LBRACE, "Expected '{' after struct name");

        List<StructDefExpr.FieldDef> fields = new ArrayList<>();
        if (!check(TokenType.RBRACE)) {
            do {
                Token fieldName = consume(TokenType.IDENTIFIER, "Expected field name");
                consume(TokenType.COLON, "Expected ':' after field name");
                Token fieldType = consume(TokenType.IDENTIFIER, "Expected field type");

                fields.add(new StructDefExpr.FieldDef(fieldName.getLexeme(), fieldType.getLexeme()));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RBRACE, "Expected '}' after struct fields");
        return new StructDefExpr(nameToken, structName, fields);
    }

    /**
     * Parses a lambda expression. Assumes the leading 'fn' keyword
     * has already been consumed.
     * Grammar: fn(param1, param2, ...) -> expression
     */
    private Expression parseLambda() {
        Token fnToken = previous(); // The 'fn' token

        consume(TokenType.LPAREN, "Expected '(' after 'fn'");

        List<String> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token paramName = consume(TokenType.IDENTIFIER, "Expected parameter name");
                parameters.add(paramName.getLexeme());

                // Skip optional type annotation for now
                if (match(TokenType.COLON)) {
                    consume(TokenType.IDENTIFIER, "Expected type name after ':'");
                }
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RPAREN, "Expected ')' after parameters");
        consume(TokenType.ARROW, "Expected '->' after lambda parameters");

        Expression body = parseExpression();

        return new LambdaExpr(fnToken, parameters, body);
    }

    /** Returns and advances over the current token. */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /** Returns true if we've consumed the EOF token. */
    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    /** Returns the current token without consuming it. */
    private Token peek() {
        return tokens.get(current);
    }

    /** Returns the most recently consumed token. */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /** Creates a parse error with the given message and token. */
    private RuntimeException error(Token token, String message) {
        String location = token.getPosition() != null
            ? "line " + token.getPosition().line + ", column " + token.getPosition().column
            : "unknown location";
        return new IllegalStateException("[" + location + "] Error at '" + token.getLexeme() + "': " + message);
    }
}