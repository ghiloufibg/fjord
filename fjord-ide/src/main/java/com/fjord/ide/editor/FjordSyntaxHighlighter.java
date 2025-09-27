package com.fjord.ide.editor;

import com.fjord.lex.Token;
import com.fjord.lex.TokenType;
import com.fjord.lex.Tokenizer;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FjordSyntaxHighlighter {

    public void highlight(CodeArea codeArea) {
        String text = codeArea.getText();
        StyleSpans<Collection<String>> highlighting = computeHighlighting(text);
        codeArea.setStyleSpans(0, highlighting);
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        try {
            Tokenizer tokenizer = new Tokenizer(text);
            List<Token> tokens = tokenizer.tokenize();

            int lastEnd = 0;

            for (Token token : tokens) {
                // Add gap before token if needed
                if (token.getPosition().offset > lastEnd) {
                    spansBuilder.add(Collections.emptyList(),
                                   token.getPosition().offset - lastEnd);
                }

                // Add styled span for the token
                String styleClass = getStyleClass(token.getType());
                if (styleClass != null) {
                    spansBuilder.add(Collections.singleton(styleClass), token.getLexeme().length());
                } else {
                    spansBuilder.add(Collections.emptyList(), token.getLexeme().length());
                }

                lastEnd = token.getPosition().offset + token.getLexeme().length();
            }

            // Add any remaining text
            if (lastEnd < text.length()) {
                spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
            }

        } catch (Exception e) {
            // If tokenization fails, return no highlighting
            spansBuilder.add(Collections.emptyList(), text.length());
        }

        return spansBuilder.create();
    }

    private String getStyleClass(TokenType tokenType) {
        return switch (tokenType) {
            case LET, MUT, FN, IF, THEN, ELSE, WHILE, DO, FOR, IN, STRUCT -> "keyword";
            case BOOL_LITERAL -> "boolean-literal";
            case INTEGER_LITERAL, FLOAT_LITERAL -> "number-literal";
            case STRING_LITERAL -> "string-literal";
            case IDENTIFIER -> "identifier";
            case PLUS, MINUS, STAR, SLASH, PERCENT,
                 EQEQ, BANGEQ, LT, LTE, GT, GTE,
                 EQUAL, ARROW -> "operator";
            case LPAREN, RPAREN, LBRACE, RBRACE,
                 LBRACKET, RBRACKET -> "punctuation";
            case SEMICOLON, COMMA, DOT, COLON -> "delimiter";
            default -> null;
        };
    }
}