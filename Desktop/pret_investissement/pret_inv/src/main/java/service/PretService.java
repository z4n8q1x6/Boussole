package service;

import dao.MensualiteDAO;
import dao.PretDAO;
import entity.Mensualite;
import entity.Pret;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

public class PretService {

    private PretDAO pretDAO = new PretDAO();
    private MensualiteDAO mensualiteDAO = new MensualiteDAO();

    /**
     * Ajoute un nouveau prêt et génère automatiquement l'échéancier des mensualités
     */
    public void ajouterPret(Pret pret) throws Exception {
        // 1. Insertion du prêt pour récupérer l'ID généré par la BDD
        pretDAO.insert(pret);

        // 2. Calcul du montant de chaque mensualité
        double montantMensuel = pret.getMontantDemande() / pret.getDureeMois();

        // 3. Génération des mensualités mois par mois
        Calendar cal = Calendar.getInstance();
        for (int i = 1; i <= pret.getDureeMois(); i++) {
            cal.add(Calendar.MONTH, 1); // Prochaine échéance dans 1 mois
            Mensualite m = new Mensualite();
            m.setDateEcheance(new java.sql.Date(cal.getTimeInMillis()));
            m.setMontant(montantMensuel);
            m.setEstPaye(false);

            // Liaison avec l'ID du prêt
            mensualiteDAO.insert(m, pret.getId());
        }
    }

    /**
     * Modifie les informations d'un prêt existant
     */
    public void modifierPret(Pret pret) throws Exception {
        // 1. Mise à jour des infos du prêt (Motif, Montant, Taux, etc.) dans la base de données
        pretDAO.update(pret);

        // 2. [Optionnel] Recalcul automatique des mensualités si les conditions changent
        /*
        mensualiteDAO.deleteByPretId(pret.getId()); // On efface l'ancien échéancier
        double nouveauMontantMensuel = pret.getMontantDemande() / pret.getDureeMois();
        Calendar cal = Calendar.getInstance();
        for (int i = 1; i <= pret.getDureeMois(); i++) {
            cal.add(Calendar.MONTH, 1);
            Mensualite m = new Mensualite();
            m.setDateEcheance(new java.sql.Date(cal.getTimeInMillis()));
            m.setMontant(nouveauMontantMensuel);
            m.setEstPaye(false);
            mensualiteDAO.insert(m, pret.getId());
        }
        */
    }

    /**
     * Alias pour modifierPret, utilisé par certaines parties du programme
     */
    public void updatePret(Pret pret) throws Exception {
        modifierPret(pret);
    }

    /**
     * Supprime un prêt et ses mensualités associées
     */
    public void supprimerPret(int id) throws Exception {
        // On supprime d'abord les mensualités pour respecter les contraintes d'intégrité (Foreign Key)
        mensualiteDAO.deleteByPretId(id);
        // Ensuite on supprime le prêt
        pretDAO.delete(id);
    }

    /**
     * Récupère la liste complète de tous les prêts
     */
    public List<Pret> getAllPrets() throws Exception {
        return pretDAO.getAll();
    }

    /**
     * Recherche avancée de prêts avec filtres
     */
    public List<Pret> searchPret(Double min, Double max, entity.StatutPret statut, String motif) throws Exception {
        return pretDAO.search(min, max, statut, motif);
    }

    // --- Gestion des Mensualités ---

    /**
     * Liste toutes les mensualités d'un prêt spécifique
     */
    public List<Mensualite> listerMensualitesParPret(int pretId) throws Exception {
        return mensualiteDAO.getByPret(pretId);
    }

    /**
     * Met à jour le statut d'une mensualité lors d'un paiement
     */
    public void marquerMensualiteCommePayee(Mensualite m) throws Exception {
        m.setEstPaye(true);
        mensualiteDAO.update(m);
    }
}