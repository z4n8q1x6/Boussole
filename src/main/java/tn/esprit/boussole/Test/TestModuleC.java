package tn.esprit.boussole.Test;

import tn.esprit.boussole.models.transaction;
import tn.esprit.boussole.models.transaction.Type;
import tn.esprit.boussole.models.budget_previsionnel;
import tn.esprit.boussole.service.ServiceTransaction;
import tn.esprit.boussole.service.ServiceBudgetPrevisionnel;
import tn.esprit.boussole.service.ServiceBilan;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TestModuleC {
    public static void main (String[] args) throws SQLException {

        System.out.println("--- ETAPE 1: INITIALISATION ---");
        ServiceTransaction serviceTransaction = new ServiceTransaction();
        ServiceBudgetPrevisionnel serviceBudget = new ServiceBudgetPrevisionnel();
        ServiceBilan serviceBilan = new ServiceBilan();

        System.out.println("--- ETAPE 2: TEST TRANSACTION (INSERT) ---");
        // Préparer une date en Octobre 2025 pour correspondre au test du bilan
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2025);
        cal.set(Calendar.MONTH, Calendar.OCTOBER);
        cal.set(Calendar.DAY_OF_MONTH, 5);
        Date dateOct2025 = cal.getTime();

        transaction t1 = new transaction(dateOct2025, 5000.0, Type.RECETTE, "Vente produit", 1);
        serviceTransaction.insertone(t1);
        System.out.println("Inséré transaction RECETTE id=" + t1.getId() + " montant=" + t1.getMontant());

        transaction t2 = new transaction(dateOct2025, 1500.0, Type.DEPENSE, "Achat fournitures", 1);
        serviceTransaction.insertone(t2);
        System.out.println("Inséré transaction DEPENSE id=" + t2.getId() + " montant=" + t2.getMontant());

        System.out.println("--- ETAPE 3: LISTE TRANSACTIONS POUR FRANCHISE 1 ---");
        List<transaction> txList = serviceTransaction.getAllByFranchise(1);
        for (transaction tx : txList) {
            System.out.println("Transaction{id=" + tx.getId() + ", date=" + tx.getDate() + ", type=" + tx.getType() + ", montant=" + tx.getMontant() + ", desc='" + tx.getDescription() + "'}");
        }

        System.out.println("--- ETAPE 4: CALCUL SOLDE ---");
        double solde = serviceTransaction.calculerSolde(1);
        System.out.println("Solde calculé pour franchise 1: " + solde);
        System.out.println("(Attendu ~3500.0 si les deux transactions ont été insérées)");

        System.out.println("--- ETAPE 5: TEST BUDGET ---");
        budget_previsionnel b = new budget_previsionnel(10, 2025, 2000.0, budget_previsionnel.TypeBudget.LIMITE_DEPENSE, "MARKETING", 1);
        System.out.println("Ajout du budget initial...");
        serviceBudget.add(b);
        System.out.println("Budget après ajout: " + b);

        System.out.println("Essai d'ajout d'un budget identique (doit updater si déjà présent)...");
        budget_previsionnel bDuplicate = new budget_previsionnel(10, 2025, 2000.0, budget_previsionnel.TypeBudget.LIMITE_DEPENSE, "MARKETING", 1);
        serviceBudget.add(bDuplicate);
        System.out.println("Opération duplicate terminée.");

        System.out.println("--- ETAPE 6: TEST BILAN ---");
        serviceBilan.genererBilan(10, 2025, 1);
        System.out.println("Bilan généré avec succès");

        System.out.println("--- FIN DU SCENARIO ---");
    }
}
