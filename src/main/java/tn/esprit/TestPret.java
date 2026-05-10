package tn.esprit;

import tn.esprit.boussole.models.Pret;
import tn.esprit.boussole.models.StatutPret;
import tn.esprit.boussole.service.PretService;
import java.util.List;

public class TestPret {
    public static void main(String[] args) {
        try {
            PretService service = new PretService();

            Pret p = new Pret();
            p.setMontantDemande(15000);
            p.setDureeMois(36);
            p.setTaux(8.5f);
            p.setStatut(StatutPret.EN_ATTENTE);
            p.setMotif("Extension activité");

            // 1. On utilise le nom de méthode défini dans l'interface CRUD
            service.insertone(p);
            System.out.println("Prêt ajouté avec succès !");

            // 2. On utilise selectAll() et on précise le type pour éviter l'ambiguïté
            List<Pret> liste = service.selectAll();
            liste.forEach((Pret pret) -> System.out.println(pret));

        } catch (Exception e) {
            System.err.println("Erreur lors du test : " + e.getMessage());
            e.printStackTrace();
        }
    }
}