package com.fjord.ide.analysis;

import com.fjord.ast.Expression;
import com.fjord.lex.Token;
import com.fjord.lex.TokenType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleSymbolAnalyzer {

    public static class SymbolInfo {
        public final String name;
        public final Symbol.SymbolType type;
        public final int line;
        public final int column;

        public SymbolInfo(String name, Symbol.SymbolType type, int line, int column) {
            this.name = name;
            this.type = type;
            this.line = line;
            this.column = column;
        }
    }

    public List<SymbolInfo> analyzeTokens(List<Token> tokens) {
        List<SymbolInfo> symbols = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.getType() == TokenType.IDENTIFIER) {
                // Check if this is a function definition
                if (i > 0 && tokens.get(i - 1).getType() == TokenType.FN) {
                    symbols.add(new SymbolInfo(
                        token.getLexeme(),
                        Symbol.SymbolType.FUNCTION,
                        token.getPosition().line,
                        token.getPosition().column
                    ));
                }
                // Check if this is a variable definition
                else if (i > 0 && tokens.get(i - 1).getType() == TokenType.LET) {
                    symbols.add(new SymbolInfo(
                        token.getLexeme(),
                        Symbol.SymbolType.VARIABLE,
                        token.getPosition().line,
                        token.getPosition().column
                    ));
                }
                // Check if this is a struct definition
                else if (i > 0 && tokens.get(i - 1).getType() == TokenType.STRUCT) {
                    symbols.add(new SymbolInfo(
                        token.getLexeme(),
                        Symbol.SymbolType.STRUCT,
                        token.getPosition().line,
                        token.getPosition().column
                    ));
                }
            }
        }

        return symbols;
    }

    public List<Symbol> analyzeFile(File file, String content) {
        List<Symbol> symbols = new ArrayList<>();

        try {
            com.fjord.lex.Tokenizer tokenizer = new com.fjord.lex.Tokenizer(content);
            List<Token> tokens = tokenizer.tokenize();

            List<SymbolInfo> symbolInfos = analyzeTokens(tokens);

            for (SymbolInfo info : symbolInfos) {
                symbols.add(new Symbol(
                    info.name,
                    info.type,
                    new com.fjord.error.SourcePosition(info.line, info.column, 0),
                    file
                ));
            }

        } catch (Exception e) {
            // If analysis fails, return empty list
        }

        return symbols;
    }

    public List<Symbol> getFileSymbols(File file) {
        try {
            String content = java.nio.file.Files.readString(file.toPath());
            return analyzeFile(file, content);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Symbol findSymbolByName(String name) {
        // TODO: Implement symbol lookup by name
        return null;
    }

    public List<SymbolReference> findReferences(Symbol symbol) {
        // TODO: Implement reference finding
        return new ArrayList<>();
    }

    public List<Symbol> searchSymbols(String query) {
        // TODO: Implement symbol search
        return new ArrayList<>();
    }
}