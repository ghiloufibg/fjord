package com.fjord;

import com.fjord.ast.Expression;
import com.fjord.lex.Token;
import com.fjord.lex.Tokenizer;
import com.fjord.parser.Parser;
import com.fjord.runtime.Environment;
import com.fjord.runtime.UnitValue;
import com.fjord.runtime.Value;
import com.fjord.runtime.builtin.PrintlnFunction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for evaluating programs through the interpreter. These tests
 * execute small snippets of Fjord code and verify the resulting values.
 */
public class InterpreterTest {

    private Value evaluate(String source) {
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();
        Parser parser = new Parser(tokens);
        List<Expression> program = parser.parseProgram();
        Environment env = new Environment();
        env.define("println", new PrintlnFunction());
        Value last = UnitValue.INSTANCE;
        for (Expression expr : program) {
            last = expr.evaluate(env);
        }
        return last;
    }

    @Test
    void testLetAndArithmetic() {
        Value result = evaluate("let x = 1 + 2; x + 3");
        assertEquals(6, result.getValue());
    }

    @Test
    void testFunctionDefinitionAndCall() {
        Value result = evaluate("fn add(a: Int, b: Int): Int = a + b; add(4, 5)");
        assertEquals(9, result.getValue());
    }

    @Test
    void testIfExpression() {
        Value result = evaluate("if 3 > 2 then 1 else 0");
        assertEquals(1, result.getValue());
    }

    @Test
    void testBlockExpression() {
        Value result = evaluate("let x = 1; { let y = x + 1; y * 2 }");
        assertEquals(4, result.getValue());
    }

    @Test
    void testPrintlnReturnsUnit() {
        Value result = evaluate("println(\"Hello\")");
        assertEquals(UnitValue.INSTANCE, result);
    }
}