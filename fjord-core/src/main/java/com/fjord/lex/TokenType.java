package com.fjord.lex;

/**
 * Enumeration of the lexical token types recognised by the Fjord lexer.
 * <p>
 * Tokens are the smallest meaningful units produced by the tokenizer.
 * Each token has a type and an optional textual value. During parsing
 * the type informs the grammar how to proceed.
 * </p>
 */
public enum TokenType {
    /** End of input marker. */
    EOF,
    /** Identifier, including keywords if not matched earlier. */
    IDENTIFIER,
    /** Integer literal (e.g. 123). */
    INTEGER_LITERAL,
    /** Floating‑point literal (e.g. 3.14). */
    FLOAT_LITERAL,
    /** String literal enclosed in double quotes. */
    STRING_LITERAL,
    /** Boolean literal (true or false). */
    BOOL_LITERAL,
    /** Keyword for immutable declarations. */
    LET,
    /** Keyword for mutable variable declarations. */
    MUT,
    /** Keyword introducing function definitions. */
    FN,
    /** Keyword for conditional expressions. */
    IF,
    /** Keyword used in conditional expressions. */
    THEN,
    /** Keyword used in conditional expressions. */
    ELSE,
    /** Keyword for while loop expressions. */
    WHILE,
    /** Keyword used in while loop expressions. */
    DO,
    /** Keyword for for loop expressions. */
    FOR,
    /** Keyword used in for loop expressions. */
    IN,
    /** Keyword for struct definitions. */
    STRUCT,
    /** Left parenthesis '(' character. */
    LPAREN,
    /** Right parenthesis ')' character. */
    RPAREN,
    /** Left brace '{' character. */
    LBRACE,
    /** Right brace '}' character. */
    RBRACE,
    /** Left bracket '[' character. */
    LBRACKET,
    /** Right bracket ']' character. */
    RBRACKET,
    /** Comma ',' used to separate arguments. */
    COMMA,
    /** Colon ':' used to separate identifiers from type annotations. */
    COLON,
    /** Semicolon ';' used to separate statements within blocks. */
    SEMICOLON,
    /** Equals '=' used for assignments and type inference. */
    EQUAL,
    /** Plus '+' operator. */
    PLUS,
    /** Minus '-' operator. */
    MINUS,
    /** Multiplication '*' operator. */
    STAR,
    /** Division '/' operator. */
    SLASH,
    /** Modulo '%' operator. */
    PERCENT,
    /** Greater-than '>' operator. */
    GT,
    /** Less-than '<' operator. */
    LT,
    /** Greater-than or equal '>=' operator. */
    GTE,
    /** Less-than or equal '<=' operator. */
    LTE,
    /** Equality '==' operator. */
    EQEQ,
    /** Inequality '!=' operator. */
    BANGEQ,
    /** Dot '.' used for field access. */
    DOT,
    /** Arrow '->' used in lambda expressions. */
    ARROW;
}