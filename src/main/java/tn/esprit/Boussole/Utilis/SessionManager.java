package tn.esprit.Boussole.Utilis;

/**
 * Classe SessionManager : Implémente le pattern Singleton
 * Gère les informations de session utilisateur de manière centralisée
 */
public class SessionManager {

    private static SessionManager instance;

    private int idUtilisateur;
    private int idFranchise;
    private String role;

    /**
     * Constructeur privé pour empêcher l'instantiation directe
     */
    private SessionManager() {
        this.idUtilisateur = 0;
        this.idFranchise = 0;
        this.role = null;
    }

    /**
     * Retourne l'instance unique de SessionManager
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ===== GETTERS =====

    public int getIdUtilisateur() {
        return idUtilisateur;
    }

    public int getIdFranchise() {
        return idFranchise;
    }

    public String getRole() {
        return role;
    }

    // ===== SETTERS =====

    public void setIdUtilisateur(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public void setIdFranchise(int idFranchise) {
        this.idFranchise = idFranchise;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Réinitialise la session (appel lors de la déconnexion)
     */
    public void cleanSession() {
        this.idUtilisateur = 0;
        this.idFranchise = 0;
        this.role = null;
    }

    /**
     * Retourne true si la session est active (idFranchise > 0)
     */
    public boolean isSessionActive() {
        return idFranchise > 0;
    }

    @Override
    public String toString() {
        return "SessionManager{" +
                "idUtilisateur=" + idUtilisateur +
                ", idFranchise=" + idFranchise +
                ", role='" + role + '\'' +
                '}';
    }
}

