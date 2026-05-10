package tn.esprit.boussole;

import tn.esprit.boussole.service.ServiceBudgetPrevisionnel;
import tn.esprit.boussole.models.budget_previsionnel;
import java.util.List;
import java.time.LocalDate;

public class ScratchTest {
    public static void main(String[] args) {
        try {
            ServiceBudgetPrevisionnel service = new ServiceBudgetPrevisionnel();
            List<budget_previsionnel> budgets = service.getAllByFranchise(1);
            System.out.println("Success! Found " + budgets.size() + " budgets for franchise 1 (including global).");
            
            int currentMonth = LocalDate.now().getMonthValue();
            int currentYear = LocalDate.now().getYear();
            System.out.println("Current Month: " + currentMonth + ", Current Year: " + currentYear);

            for (budget_previsionnel b : budgets) {
                System.out.println("Budget ID: " + b.getId() + 
                                   ", Mois: " + b.getMois() + 
                                   ", Annee: " + b.getAnnee() + 
                                   ", Type: " + b.getType_budget() + 
                                   ", Montant: " + b.getMontantCible() + 
                                   ", FranchiseID: " + b.getFranchiseId());
            }
        } catch (Exception e) {
            System.err.println("Exception occurred:");
            e.printStackTrace();
        }
    }
}
