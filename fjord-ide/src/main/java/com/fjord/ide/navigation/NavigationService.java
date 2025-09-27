package com.fjord.ide.navigation;

import com.fjord.ide.analysis.Symbol;
import com.fjord.ide.analysis.SimpleSymbolAnalyzer;
import com.fjord.ide.analysis.SymbolReference;
import com.fjord.error.SourcePosition;
import com.fjord.lex.Token;
import com.fjord.lex.TokenType;
import com.fjord.lex.Tokenizer;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class NavigationService {
    private final SimpleSymbolAnalyzer symbolAnalyzer;

    public NavigationService(SimpleSymbolAnalyzer symbolAnalyzer) {
        this.symbolAnalyzer = symbolAnalyzer;
    }

    public Optional<NavigationTarget> goToDefinition(File file, String content, int line, int column) {
        try {
            // Find the symbol at the cursor position
            String symbolName = getSymbolAtPosition(content, line, column);
            if (symbolName == null) {
                return Optional.empty();
            }

            // Find the symbol definition
            Symbol symbol = symbolAnalyzer.findSymbolByName(symbolName);
            if (symbol != null) {
                return Optional.of(new NavigationTarget(
                    symbol.getFile(),
                    symbol.getDefinition().line,
                    symbol.getDefinition().column,
                    symbol.getName(),
                    NavigationTarget.TargetType.DEFINITION
                ));
            }

            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Go-to-definition failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<NavigationTarget> findReferences(File file, String content, int line, int column) {
        try {
            String symbolName = getSymbolAtPosition(content, line, column);
            if (symbolName == null) {
                return List.of();
            }

            Symbol symbol = symbolAnalyzer.findSymbolByName(symbolName);
            if (symbol == null) {
                return List.of();
            }

            List<SymbolReference> references = symbolAnalyzer.findReferences(symbol);
            return references.stream()
                    .map(ref -> new NavigationTarget(
                        ref.getFile(),
                        ref.getPosition().line,
                        ref.getPosition().column,
                        ref.getContext(),
                        ref.isDefinition() ? NavigationTarget.TargetType.DEFINITION : NavigationTarget.TargetType.REFERENCE
                    ))
                    .toList();
        } catch (Exception e) {
            System.err.println("Find references failed: " + e.getMessage());
            return List.of();
        }
    }

    public Optional<NavigationTarget> findNextReference(File file, String content, int line, int column) {
        List<NavigationTarget> references = findReferences(file, content, line, column);

        // Find the next reference after the current position
        return references.stream()
                .filter(ref -> ref.getFile().equals(file))
                .filter(ref -> ref.getLine() > line || (ref.getLine() == line && ref.getColumn() > column))
                .findFirst()
                .or(() -> references.stream().findFirst()); // Wrap to beginning if no next found
    }

    public Optional<NavigationTarget> findPreviousReference(File file, String content, int line, int column) {
        List<NavigationTarget> references = findReferences(file, content, line, column);

        // Find the previous reference before the current position
        return references.stream()
                .filter(ref -> ref.getFile().equals(file))
                .filter(ref -> ref.getLine() < line || (ref.getLine() == line && ref.getColumn() < column))
                .reduce((first, second) -> second) // Get the last one
                .or(() -> references.stream().reduce((first, second) -> second)); // Wrap to end if no previous found
    }

    public List<Symbol> findSymbolsInScope(File file, String content, int line, int column) {
        // Find all symbols that are in scope at the given position
        List<Symbol> fileSymbols = symbolAnalyzer.getFileSymbols(file);

        return fileSymbols.stream()
                .filter(symbol -> isSymbolInScope(symbol, line, column))
                .toList();
    }

    private boolean isSymbolInScope(Symbol symbol, int line, int column) {
        // Simple scope check - can be enhanced with proper scope analysis
        SourcePosition symbolPos = symbol.getDefinition();

        // If symbol is defined before the current position, it's likely in scope
        return symbolPos.line < line || (symbolPos.line == line && symbolPos.column < column);
    }

    private String getSymbolAtPosition(String content, int line, int column) {
        try {
            String[] lines = content.split("\n");
            if (line < 1 || line > lines.length) {
                return null;
            }

            String currentLine = lines[line - 1];
            if (column < 1 || column > currentLine.length()) {
                return null;
            }

            // Find word boundaries around the cursor position
            int start = column - 1;
            int end = column - 1;

            // Move start backwards to find word start
            while (start > 0 && (Character.isLetterOrDigit(currentLine.charAt(start - 1)) ||
                                currentLine.charAt(start - 1) == '_')) {
                start--;
            }

            // Move end forwards to find word end
            while (end < currentLine.length() && (Character.isLetterOrDigit(currentLine.charAt(end)) ||
                                                 currentLine.charAt(end) == '_')) {
                end++;
            }

            if (start < end) {
                return currentLine.substring(start, end);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Optional<String> getHoverInfo(File file, String content, int line, int column) {
        try {
            String symbolName = getSymbolAtPosition(content, line, column);
            if (symbolName == null) {
                return Optional.empty();
            }

            Symbol symbol = symbolAnalyzer.findSymbolByName(symbolName);
            if (symbol == null) {
                return Optional.empty();
            }

            StringBuilder info = new StringBuilder();
            info.append("**").append(symbol.getName()).append("**");

            if (symbol.getSignature() != null) {
                info.append("\n```fjord\n");
                info.append(symbol.getName()).append(symbol.getSignature());
                info.append("\n```");
            }

            info.append("\n*").append(symbol.getType()).append("*");

            if (symbol.getFile() != null) {
                info.append("\n\nDefined in: ").append(symbol.getFile().getName());
                info.append(" at line ").append(symbol.getDefinition().line);
            }

            if (symbol.getDocumentation() != null) {
                info.append("\n\n").append(symbol.getDocumentation());
            }

            return Optional.of(info.toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Find all symbols matching a pattern (for search)
    public List<Symbol> searchSymbols(String pattern) {
        return symbolAnalyzer.searchSymbols(pattern);
    }
}