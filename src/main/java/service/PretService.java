package service;

import dao.MensualiteDAO;
import dao.PretDAO;
import entity.Mensualite;
import entity.Pret;
import entity.StatutPret;
import util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

// Imports pour le PDF (iText 7)
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.UnitValue;
import java.io.FileOutputStream;

public class PretService {

    private PretDAO pretDAO = new PretDAO();
    private MensualiteDAO mensualiteDAO = new MensualiteDAO();
    private MailService mailService = new MailService();

    // =========================================================
    // NOUVELLES MÉTHODES DE CALCUL POUR LE CHATBOT
    // =========================================================

    /**
     * Calcule la somme totale des montants pour un statut précis
     */
    public double getMontantTotalParStatut(StatutPret statut) {
        return getAllPrets().stream()
                .filter(p -> p.getStatut() == statut)
                .mapToDouble(Pret::getMontantDemande)
                .sum();
    }

    /**
     * Compte le nombre de prêts pour un statut précis
     */
    public long countPretsParStatut(StatutPret statut) {
        return getAllPrets().stream()
                .filter(p -> p.getStatut() == statut)
                .count();
    }

    // =========================================================
    // MÉTHODES EXISTANTES
    // =========================================================

    public void ajouterPret(Pret pret) throws Exception {
        pretDAO.insert(pret);
        genererMensualites(pret);
    }

    public void genererMensualites(Pret p) throws Exception {
        double montantMensuel = p.getMontantDemande() / p.getDureeMois();
        for (int i = 1; i <= p.getDureeMois(); i++) {
            Mensualite m = new Mensualite();
            m.setDateEcheance(java.sql.Date.valueOf(java.time.LocalDate.now().plusMonths(i)));
            m.setMontant(montantMensuel);
            m.setEstPaye(false);
            mensualiteDAO.insert(m, p.getId());
        }
        System.out.println("Échéancier généré pour le prêt ID: " + p.getId());
    }

    public void genererRapportPDF(List<Pret> liste, String cheminFichier) throws Exception {
        PdfWriter writer = new PdfWriter(new FileOutputStream(cheminFichier));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("BOUSSOLE - RAPPORT DES PRÊTS").setBold().setFontSize(18));
        document.add(new Paragraph("Liste extraite le : " + java.time.LocalDate.now()));
        document.add(new Paragraph("\n"));

        float[] columnWidths = {3, 2, 2, 1, 2};
        Table table = new Table(UnitValue.createPointArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(new Cell().add(new Paragraph("Motif")));
        table.addHeaderCell(new Cell().add(new Paragraph("Montant (DT)")));
        table.addHeaderCell(new Cell().add(new Paragraph("Durée (Mois)")));
        table.addHeaderCell(new Cell().add(new Paragraph("Taux")));
        table.addHeaderCell(new Cell().add(new Paragraph("Statut")));

        for (Pret p : liste) {
            table.addCell(new Cell().add(new Paragraph(p.getMotif())));
            table.addCell(new Cell().add(new Paragraph(String.format("%.2f", p.getMontantDemande()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(p.getDureeMois()))));
            table.addCell(new Cell().add(new Paragraph(p.getTaux() + "%")));
            table.addCell(new Cell().add(new Paragraph(p.getStatut().toString())));
        }

        document.add(table);
        document.close();
    }

    public void modifierPret(Pret p) throws Exception {
        pretDAO.update(p);
    }

    public void supprimerPret(int id) throws Exception {
        mensualiteDAO.deleteByPretId(id);
        pretDAO.delete(id);
    }

    public List<Pret> getAllPrets() {
        List<Pret> liste = new ArrayList<>();
        String sql = "SELECT * FROM pret";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Pret p = new Pret();
                p.setId(rs.getInt("id"));
                p.setMotif(rs.getString("motif"));
                p.setMontantDemande(rs.getDouble("montant_demande"));
                p.setDureeMois(rs.getInt("duree_mois"));
                p.setTaux(rs.getFloat("taux"));

                String statutStr = rs.getString("statut");
                if (statutStr != null) {
                    p.setStatut(StatutPret.valueOf(statutStr));
                }
                liste.add(p);
            }
        } catch (Exception e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
        return liste;
    }

    public List<Mensualite> getAllMensualitesGlobales() throws Exception {
        return mensualiteDAO.getAll();
    }

    public List<Mensualite> getMensualitesByPret(int pretId) {
        try {
            return mensualiteDAO.getByPret(pretId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void marquerMensualiteCommePayee(Mensualite m) throws Exception {
        m.setEstPaye(true);
        mensualiteDAO.update(m);
    }

    public void envoyerAccuseReceptionPaiement(Mensualite m, String motifPret) {
        try {
            mailService.envoyerEmailPaiement(m, motifPret);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Pret> searchPret(Double min, Double max, StatutPret statut, String motif) throws Exception {
        return pretDAO.search(min, max, statut, motif);
    }
}