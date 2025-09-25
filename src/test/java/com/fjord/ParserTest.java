package com.fjord;

import com.fjord.ast.Expression;
import com.fjord.lex.Token;
import com.fjord.lex.Tokenizer;
import com.fjord.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for parsing basic syntax constructs. The goal of these tests
 * is to ensure that the parser can consume valid programs without
 * throwing exceptions. Detailed behavioural semantics are validated
 * separately in interpreter tests.
 */
public class ParserTest {

    @Test
    void testParseSimpleLet() {
        String source = "let x = 1 + 2; x + 3";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();
        Parser parser = new Parser(tokens);
        List<Expression> program = parser.parseProgram();
        // Expect two top‑level expressions: let and expression
        assertEquals(2, program.size());
    }

    @Test
    void testParseFunctionDefinition() {
        String source = "fn add(a: Int, b: Int): Int = a + b; add(4, 5)";
        Tokenizer tokenizer = new Tokenizer(source);
        List<Token> tokens = tokenizer.tokenize();
        Parser parser = new Parser(tokens);
        List<Expression> program = parser.parseProgram();
        assertEquals(2, program.size());
    }

    @Test
    void testParseIfAndBlock() {
        String source = "if 3 > 2 then { let x = 1; x + 1 } else 0";
        Tokenizer tokenizer = new Tokenizer(source);
        Parser parser = new Parser(tokenizer.tokenize());
        List<Expression> program = parser.parseProgram();
        assertEquals(1, program.size());
    }
}