import entity.Pret;
import entity.StatutPret;
import service.PretService;

public class TestPret {
    public static void main(String[] args) throws Exception {

        PretService service = new PretService();

        Pret p = new Pret();
        p.setMontantDemande(15000);
        p.setDureeMois(36);
        p.setTaux(8.5f);
        p.setStatut(StatutPret.EN_ATTENTE);
        p.setMotif("Extension activité");

        // Ajoute le prêt dans la base
        service.ajouterPret(p);

        // MODIFICATION ICI : On utilise le nouveau nom de la méthode
        service.getAllPrets().forEach(System.out::println);
    }
}