package tn.esprit.boussole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import tn.esprit.boussole.utils.MyBdConnexion;

public class TestSQL {
    public static void main(String[] args) {
        try {
            Connection conn = MyBdConnexion.getinstance().getCnx();
            System.out.println("Connected to DB.");

            String sql = "SELECT * FROM budget_previsionnel";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") +
                                   ", Mois: " + rs.getInt("mois") +
                                   ", Annee: " + rs.getInt("annee") +
                                   ", Franchise: " + rs.getInt("franchise_id") +
                                   ", Type: " + rs.getString("type_budget") +
                                   ", Montant: " + rs.getDouble("montant_cible"));
            }
            
            System.out.println("--- Sum Test ---");
            String sumSql = "SELECT " +
                         "COALESCE(SUM(CASE WHEN type_budget='LIMITE_DEPENSE' THEN montant_cible ELSE 0 END), 0) as limite_totale, " +
                         "COALESCE(SUM(CASE WHEN type_budget='OBJECTIF_REVENU' THEN montant_cible ELSE 0 END), 0) as objectif_total " +
                         "FROM budget_previsionnel WHERE (franchise_id = 1 OR franchise_id = 0) AND mois = 5 AND annee = 2026";
            PreparedStatement ps2 = conn.prepareStatement(sumSql);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                System.out.println("Limite 5/2026: " + rs2.getDouble("limite_totale"));
                System.out.println("Objectif 5/2026: " + rs2.getDouble("objectif_total"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
