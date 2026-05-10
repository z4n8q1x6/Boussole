package tn.esprit.boussole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import tn.esprit.boussole.utils.MyBdConnexion;

public class CheckID {
    public static void main(String[] args) {
        try {
            Connection conn = MyBdConnexion.getinstance().getCnx();
            System.out.println("=== FRANCHISES ===");
            ResultSet rs1 = conn.createStatement().executeQuery("SELECT id, nom FROM franchise");
            while (rs1.next()) {
                System.out.println("ID: " + rs1.getInt("id") + " - " + rs1.getString("nom"));
            }
            
            System.out.println("\n=== BUDGETS ===");
            ResultSet rs2 = conn.createStatement().executeQuery("SELECT id, franchise_id, mois, annee, montant_cible, type_budget FROM budget_previsionnel");
            while (rs2.next()) {
                System.out.println("Budget ID: " + rs2.getInt("id") + 
                                   " | Franchise ID: " + rs2.getInt("franchise_id") + 
                                   " | Date: " + rs2.getInt("mois") + "/" + rs2.getInt("annee") + 
                                   " | Type: " + rs2.getString("type_budget") + 
                                   " | Montant: " + rs2.getDouble("montant_cible"));
            }
            
            System.out.println("\n=== UTILISATEUR ===");
            PreparedStatement ps3 = conn.prepareStatement("SELECT email, id_franchise FROM utilisateur WHERE email = ?");
            ps3.setString(1, "siwar.raouafi1@gmail.com");
            ResultSet rs3 = ps3.executeQuery();
            if (rs3.next()) {
                System.out.println("User email: " + rs3.getString("email") + " | Franchise ID: " + rs3.getInt("id_franchise"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
