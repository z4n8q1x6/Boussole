package tn.esprit.boussole.utils;

/**
 * Configuration pour l'envoi d'emails
 */
public class EmailConfig {

    private String smtpHost;
    private String smtpPort;
    private String username;
    private String password;
    private boolean debug;

    /**
     * Constructeur privé
     */
    public EmailConfig(String smtpHost, String smtpPort, String username, String password, boolean debug) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.debug = debug;
    }

    /**
     * Configuration Gmail (pour envoi réel)
     */
    public static EmailConfig getGmailConfig(String email, String motDePasseApp) {
        return new EmailConfig(
                "smtp.gmail.com",    // Serveur Gmail
                "587",                 // Port TLS
                email,                 // Votre email
                motDePasseApp,         // Mot de passe d'application
                false                  // debug désactivé
        );
    }

    /**
     * Configuration Gmail par défaut (si vous voulez utiliser des constantes)
     */
    public static EmailConfig getGmailConfig() {
        return new EmailConfig(
                "smtp.gmail.com",
                "587",
                "azizjlassi235@gmail.com",
                "zckamzqldkxsicyg",
                false
        );
    }

    /**
     * Configuration pour Mailtrap (tests)
     */
    public static EmailConfig getMailtrapConfig() {
        return new EmailConfig(
                "smtp.mailtrap.io",
                "2525",
                "votre-username-mailtrap",
                "votre-password-mailtrap",
                true
        );
    }

    /**
     * Configuration personnalisée (CELLE QUE VOUS UTILISEZ)
     */
    public static EmailConfig getCustomConfig(String host, String port, String user, String pwd, boolean debug) {
        return new EmailConfig(host, port, user, pwd, debug);
    }

    /**
     * Configuration par défaut (utilise Gmail)
     */
    public static EmailConfig getDefaultConfig() {
        return getGmailConfig();
    }

    // Getters
    public String getSmtpHost() { return smtpHost; }
    public String getSmtpPort() { return smtpPort; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isDebug() { return debug; }

    @Override
    public String toString() {
        return "EmailConfig{" +
                "smtpHost='" + smtpHost + '\'' +
                ", smtpPort='" + smtpPort + '\'' +
                ", username='" + username + '\'' +
                ", debug=" + debug +
                '}';
    }
}