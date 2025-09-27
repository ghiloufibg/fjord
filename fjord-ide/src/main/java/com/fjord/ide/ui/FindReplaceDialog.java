package com.fjord.ide.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FindReplaceDialog {
    private final Stage stage;
    private final TextField findField;
    private final TextField replaceField;
    private final CheckBox caseSensitiveBox;
    private final CheckBox regexBox;
    private final CheckBox wholeWordBox;
    private final Label statusLabel;

    private CodeArea codeArea;
    private int lastSearchIndex = 0;

    public FindReplaceDialog(Stage parentStage) {
        stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.initOwner(parentStage);
        stage.setTitle("Find and Replace");
        stage.setResizable(false);

        // Create UI elements
        findField = new TextField();
        findField.setPromptText("Find...");
        findField.setPrefWidth(300);

        replaceField = new TextField();
        replaceField.setPromptText("Replace with...");
        replaceField.setPrefWidth(300);

        caseSensitiveBox = new CheckBox("Case sensitive");
        regexBox = new CheckBox("Regular expression");
        wholeWordBox = new CheckBox("Whole word");

        Button findNextButton = new Button("Find Next");
        Button findPrevButton = new Button("Find Previous");
        Button replaceButton = new Button("Replace");
        Button replaceAllButton = new Button("Replace All");
        Button closeButton = new Button("Close");

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red;");

        // Layout
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Find:"), 0, 0);
        grid.add(findField, 1, 0, 2, 1);

        grid.add(new Label("Replace:"), 0, 1);
        grid.add(replaceField, 1, 1, 2, 1);

        HBox optionsBox = new HBox(10);
        optionsBox.getChildren().addAll(caseSensitiveBox, regexBox, wholeWordBox);
        grid.add(optionsBox, 0, 2, 3, 1);

        HBox buttonBox = new HBox(5);
        buttonBox.getChildren().addAll(findNextButton, findPrevButton, replaceButton, replaceAllButton, closeButton);
        grid.add(buttonBox, 0, 3, 3, 1);

        grid.add(statusLabel, 0, 4, 3, 1);

        // Event handlers
        findNextButton.setOnAction(e -> findNext());
        findPrevButton.setOnAction(e -> findPrevious());
        replaceButton.setOnAction(e -> replaceNext());
        replaceAllButton.setOnAction(e -> replaceAll());
        closeButton.setOnAction(e -> stage.hide());

        findField.setOnAction(e -> findNext());
        findField.textProperty().addListener((obs, oldText, newText) -> {
            lastSearchIndex = 0;
            statusLabel.setText("");
        });

        Scene scene = new Scene(grid);
        stage.setScene(scene);
    }

    public void show(CodeArea codeArea) {
        this.codeArea = codeArea;
        lastSearchIndex = codeArea.getCaretPosition();

        // Pre-fill with selected text if any
        String selectedText = codeArea.getSelectedText();
        if (!selectedText.isEmpty()) {
            findField.setText(selectedText);
        }

        stage.show();
        findField.requestFocus();
        findField.selectAll();
    }

    private void findNext() {
        if (codeArea == null || findField.getText().isEmpty()) {
            return;
        }

        String searchText = findField.getText();
        String content = codeArea.getText();

        try {
            Pattern pattern = createPattern(searchText);
            Matcher matcher = pattern.matcher(content);

            if (matcher.find(lastSearchIndex)) {
                highlightMatch(matcher.start(), matcher.end());
                lastSearchIndex = matcher.end();
                statusLabel.setText("");
            } else {
                // Search from beginning
                if (matcher.find(0)) {
                    highlightMatch(matcher.start(), matcher.end());
                    lastSearchIndex = matcher.end();
                    statusLabel.setText("Wrapped to beginning");
                } else {
                    statusLabel.setText("Not found");
                }
            }
        } catch (PatternSyntaxException e) {
            statusLabel.setText("Invalid regex: " + e.getMessage());
        }
    }

    private void findPrevious() {
        if (codeArea == null || findField.getText().isEmpty()) {
            return;
        }

        String searchText = findField.getText();
        String content = codeArea.getText();

        try {
            Pattern pattern = createPattern(searchText);
            Matcher matcher = pattern.matcher(content);

            int foundStart = -1;
            int foundEnd = -1;

            // Find the last match before current position
            while (matcher.find() && matcher.start() < lastSearchIndex - 1) {
                foundStart = matcher.start();
                foundEnd = matcher.end();
            }

            if (foundStart >= 0) {
                highlightMatch(foundStart, foundEnd);
                lastSearchIndex = foundStart;
                statusLabel.setText("");
            } else {
                // Search from end
                matcher.reset();
                while (matcher.find()) {
                    foundStart = matcher.start();
                    foundEnd = matcher.end();
                }

                if (foundStart >= 0) {
                    highlightMatch(foundStart, foundEnd);
                    lastSearchIndex = foundStart;
                    statusLabel.setText("Wrapped to end");
                } else {
                    statusLabel.setText("Not found");
                }
            }
        } catch (PatternSyntaxException e) {
            statusLabel.setText("Invalid regex: " + e.getMessage());
        }
    }

    private void replaceNext() {
        if (codeArea == null || findField.getText().isEmpty()) {
            return;
        }

        String selectedText = codeArea.getSelectedText();
        String findText = findField.getText();

        try {
            Pattern pattern = createPattern(findText);

            if (!selectedText.isEmpty() && pattern.matcher(selectedText).matches()) {
                // Replace selected text
                codeArea.replaceSelection(replaceField.getText());
            }

            // Find next occurrence
            findNext();
        } catch (PatternSyntaxException e) {
            statusLabel.setText("Invalid regex: " + e.getMessage());
        }
    }

    private void replaceAll() {
        if (codeArea == null || findField.getText().isEmpty()) {
            return;
        }

        String content = codeArea.getText();
        String findText = findField.getText();
        String replaceText = replaceField.getText();

        try {
            Pattern pattern = createPattern(findText);
            String newContent = pattern.matcher(content).replaceAll(replaceText);

            int replacements = content.length() - newContent.length() +
                             (newContent.length() - content.length());

            codeArea.replaceText(newContent);
            statusLabel.setText("Replaced " + countMatches(pattern, content) + " occurrences");
        } catch (PatternSyntaxException e) {
            statusLabel.setText("Invalid regex: " + e.getMessage());
        }
    }

    private Pattern createPattern(String searchText) throws PatternSyntaxException {
        String patternText = searchText;

        if (!regexBox.isSelected()) {
            patternText = Pattern.quote(searchText);
        }

        if (wholeWordBox.isSelected()) {
            patternText = "\\b" + patternText + "\\b";
        }

        int flags = caseSensitiveBox.isSelected() ? 0 : Pattern.CASE_INSENSITIVE;
        return Pattern.compile(patternText, flags);
    }

    private void highlightMatch(int start, int end) {
        codeArea.selectRange(start, end);
        codeArea.requestFollowCaret();
    }

    private int countMatches(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}