package tn.esprit.Boussole.Utilis;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

/**
 * Singleton pour gérer le thème global de l'application (Dark / Light).
 * Utilise AtlantaFX pour appliquer des thèmes modernes.
 */
public class ThemeManager {

    private static ThemeManager instance;
    private String currentTheme = "DARK"; // Thème par défaut

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Applique le thème spécifié à l'application.
     * @param theme "DARK" ou "LIGHT"
     */
    public void setTheme(String theme) {
        this.currentTheme = theme;
        if ("LIGHT".equalsIgnoreCase(theme)) {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }
    }

    /**
     * Bascule entre le mode sombre et le mode clair.
     */
    public void toggleTheme() {
        if ("DARK".equalsIgnoreCase(currentTheme)) {
            setTheme("LIGHT");
        } else {
            setTheme("DARK");
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
