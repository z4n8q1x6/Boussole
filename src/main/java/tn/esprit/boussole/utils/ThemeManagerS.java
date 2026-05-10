package tn.esprit.boussole.utils;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.scene.Scene;
import javafx.application.Application;

/**
 * Singleton pour gérer le thème global de l'application (Dark / Light).
 * Utilise AtlantaFX pour appliquer des thèmes modernes.
 */
public class ThemeManagerS {

    private static ThemeManagerS instance;
    private String currentTheme = "DARK"; // Thème par défaut

    private ThemeManagerS() {}

    public static ThemeManagerS getInstance() {
        if (instance == null) {
            instance = new ThemeManagerS();
        }
        return instance;
    }

    /**
     * Applique le thème spécifié à l'application et à la scène fournie.
     * @param theme "DARK" ou "LIGHT"
     * @param scene La scène actuelle (optionnelle)
     */
    public void setTheme(String theme, Scene scene) {
        this.currentTheme = theme;
        if ("LIGHT".equalsIgnoreCase(theme)) {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }
        
        if (scene != null) {
            applyCurrentTheme(scene);
        }
    }

    /**
     * Méthode de compatibilité pour setTheme sans scène.
     */
    public void setTheme(String theme) {
        setTheme(theme, null);
    }

    /**
     * Bascule entre le mode sombre et le mode clair pour une scène donnée.
     */
    public void toggleTheme(Scene scene) {
        if ("DARK".equalsIgnoreCase(currentTheme)) {
            setTheme("LIGHT", scene);
        } else {
            setTheme("DARK", scene);
        }
    }

    /**
     * Applique les classes CSS de thème (.theme-dark/.theme-light) à la racine de la scène.
     * Doit être appelé lors du chargement de chaque nouvelle scène.
     */
    public void applyCurrentTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        
        scene.getRoot().getStyleClass().remove("theme-dark");
        scene.getRoot().getStyleClass().remove("theme-light");
        
        if ("DARK".equalsIgnoreCase(currentTheme)) {
            scene.getRoot().getStyleClass().add("theme-dark");
        } else {
            scene.getRoot().getStyleClass().add("theme-light");
        }
    }

    /**
     * Retourne le thème actuellement actif.
     * @return "DARK" ou "LIGHT"
     */
    public String getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Vérifie si le thème actuel est le mode sombre.
     */
    public boolean isDark() {
        return "DARK".equalsIgnoreCase(currentTheme);
    }
}
