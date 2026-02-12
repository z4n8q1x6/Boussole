package tn.esprit.chargesdepenses.test;

import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.models.enums.StatusValidation;
import tn.esprit.chargesdepenses.models.enums.TypeCharge;
import tn.esprit.chargesdepenses.services.ChargeService;
import tn.esprit.chargesdepenses.services.FournisseurService;
import tn.esprit.chargesdepenses.utils.MyBDConnexion;

import java.sql.SQLException;
import java.time.LocalDate;

public class Test {
    public static void main(String[] args) {
        // 1. On vérifie d'abord la connexion
        MyBDConnexion.getInstance();

        // 2. Création d'un objet Charge valide
        // Rappel : (titre, montant, date, type, image, franchiseId)
        Charge charge1 = new Charge(
                "Achat Fournitures",
                150.50,
                LocalDate.now(),
                TypeCharge.CHARGES_EXPLOITATIONS,
                "facture_001.png",
                1 // ATTENTION: l'ID de franchise 1 doit exister dans ta table franchise
        );

        ChargeService cs = new ChargeService();
        FournisseurService fs = new FournisseurService();

        try {
            // Test Insertion
            System.out.println("Insertion de la charge...");
            cs.insertOne(charge1);

            // Test Affichage
            System.out.println("Liste des charges en base :");
            System.out.println(cs.selectAll());

            // Test Fournisseur (Optionnel)
            // Fournisseur f1 = new Fournisseur("Alpha Tech", "MF12345", "71000000", 1);
            // fs.insertOne(f1);

        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }
}
