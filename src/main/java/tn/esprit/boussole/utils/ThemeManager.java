package tn.esprit.boussole.utils;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public class ThemeManager {
    private static final String THEME_KEY = "app_theme";
    private static final String DARK_THEME = "/styles/dash.css";
    private static final String LIGHT_THEME = "/styles/light.css";
    
    private static final Preferences prefs = Preferences.userRoot().node(ThemeManager.class.getName());

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        
        String currentTheme = prefs.get(THEME_KEY, DARK_THEME);
        scene.getStylesheets().clear();
        scene.getStylesheets().add(ThemeManager.class.getResource(currentTheme).toExternalForm());
    }

    public static void toggleTheme(Scene scene) {
        if (scene == null) return;

        String currentTheme = prefs.get(THEME_KEY, DARK_THEME);
        String newTheme = currentTheme.equals(DARK_THEME) ? LIGHT_THEME : DARK_THEME;
        
        prefs.put(THEME_KEY, newTheme);
        
        scene.getStylesheets().clear();
        scene.getStylesheets().add(ThemeManager.class.getResource(newTheme).toExternalForm());
    }
    
    public static boolean isDarkMode() {
        return prefs.get(THEME_KEY, DARK_THEME).equals(DARK_THEME);
    }
}