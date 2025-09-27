package com.fjord.ide.ui;

import javafx.scene.Scene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class ThemeManager {
    private static final String THEME_PREF_KEY = "current_theme";
    private static final String DEFAULT_THEME = "light";

    public enum Theme {
        LIGHT("light", "/styles/fjord-ide-light.css"),
        DARK("dark", "/styles/fjord-ide-dark.css");

        private final String name;
        private final String cssFile;

        Theme(String name, String cssFile) {
            this.name = name;
            this.cssFile = cssFile;
        }

        public String getName() { return name; }
        public String getCssFile() { return cssFile; }

        public static Theme fromName(String name) {
            for (Theme theme : values()) {
                if (theme.name.equals(name)) {
                    return theme;
                }
            }
            return LIGHT;
        }
    }

    private final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private Theme currentTheme;
    private Scene scene;

    public ThemeManager() {
        String themeName = prefs.get(THEME_PREF_KEY, DEFAULT_THEME);
        currentTheme = Theme.fromName(themeName);
    }

    public void attachToScene(Scene scene) {
        this.scene = scene;
        applyTheme(currentTheme);
    }

    public void setTheme(Theme theme) {
        if (theme != currentTheme) {
            currentTheme = theme;
            prefs.put(THEME_PREF_KEY, theme.getName());
            applyTheme(theme);
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void toggleTheme() {
        Theme newTheme = currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT;
        setTheme(newTheme);
    }

    private void applyTheme(Theme theme) {
        if (scene == null) return;

        // Clear existing stylesheets
        scene.getStylesheets().clear();

        // Add new theme stylesheet
        try {
            String cssResource = theme.getCssFile();
            String cssUrl = getClass().getResource(cssResource).toExternalForm();
            scene.getStylesheets().add(cssUrl);
        } catch (Exception e) {
            System.err.println("Failed to load theme CSS: " + e.getMessage());
            // Fallback to default if custom theme fails
            if (theme != Theme.LIGHT) {
                applyTheme(Theme.LIGHT);
            }
        }
    }

    public boolean isDarkTheme() {
        return currentTheme == Theme.DARK;
    }

    public boolean isLightTheme() {
        return currentTheme == Theme.LIGHT;
    }
}