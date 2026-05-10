package tn.esprit.boussole.service;

import tn.esprit.boussole.models.Pret;
import tn.esprit.boussole.models.Mensualite;
import tn.esprit.boussole.models.StatutPret;
import tn.esprit.boussole.util.DBConnection;

// --- IMPORTS SPECIFIQUES ITEXT 7 ---
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.UnitValue;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PretService implements crud<Pret> {

    private Connection cnx;
    private MensualiteService mensualiteService = new MensualiteService();

    public PretService() {
        this.cnx = DBConnection.getConnection();
    }

    public List<Mensualite> getMensualitesByPret(int pretId) throws SQLException {
        return mensualiteService.getMensualitesByPret(pretId);
    }

    public void marquerMensualiteCommePayee(Mensualite m) throws SQLException {
        m.setEstPaye(true);
        mensualiteService.updateone(m);
    }

    public void modifierPret(Pret p) throws SQLException {
        updateone(p);
    }

    public void supprimerPret(int id) throws SQLException {
        Pret p = new Pret();
        p.setId(id);
        deleteone(p);
    }

    public void genererRapportPDF(List<Pret> liste, String path) throws Exception {
        PdfWriter writer = new PdfWriter(path);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Rapport des Prêts - Boussole")
                .setBold()
                .setFontSize(18));

        Table table = new Table(UnitValue.createPointArray(new float[] { 100f, 80f, 60f, 60f, 80f }));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(new Cell().add(new Paragraph("Motif")));
        table.addHeaderCell(new Cell().add(new Paragraph("Montant (DT)")));
        table.addHeaderCell(new Cell().add(new Paragraph("Durée")));
        table.addHeaderCell(new Cell().add(new Paragraph("Taux")));
        table.addHeaderCell(new Cell().add(new Paragraph("Statut")));

        for (Pret p : liste) {
            table.addCell(new Cell().add(new Paragraph(p.getMotif())));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(p.getMontantDemande()))));
            table.addCell(new Cell().add(new Paragraph(p.getDureeMois() + " mois")));
            table.addCell(new Cell().add(new Paragraph(p.getTaux() + "%")));
            table.addCell(new Cell().add(new Paragraph(p.getStatut() != null ? p.getStatut().toString() : "")));
        }

        document.add(table);
        document.close();
    }

    // --- LOGIQUE MÉTIER & STATS ---

    public double getMontantTotalParStatut(StatutPret statut) {
        String sql = "SELECT SUM(montant_demande) FROM pret WHERE statut = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, statut.name());
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public long countPretsParStatut(StatutPret statut) {
        String sql = "SELECT COUNT(*) FROM pret WHERE statut = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, statut.name());
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getLong(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Pret> getAllPrets() {
        return selectAll();
    }

    // --- IMPLEMENTATION INTERFACE CRUD ---

    @Override
    public void insertone(Pret pret) throws SQLException {
        String sql = "INSERT INTO pret (montant_demande, duree_mois, taux, statut, motif, date_demande, franchise_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDouble(1, pret.getMontantDemande());
            stmt.setInt(2, pret.getDureeMois());
            stmt.setFloat(3, pret.getTaux());
            stmt.setString(4, (pret.getStatut() != null) ? pret.getStatut().name() : "EN_ATTENTE");
            stmt.setString(5, pret.getMotif());
            stmt.setTimestamp(6, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            if (pret.getFranchiseId() != null && pret.getFranchiseId() > 0) {
                stmt.setInt(7, pret.getFranchiseId());
            } else {
                stmt.setInt(7, 1);
            }
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    pret.setId(rs.getInt(1));
            }
        }
        // ← genererMensualites supprimé ici — mensualités générées uniquement lors de
        // l'accord
    }

    @Override
    public void updateone(Pret pret) throws SQLException {
        String sql = "UPDATE pret SET montant_demande=?, duree_mois=?, taux=?, statut=?, motif=? WHERE id=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setDouble(1, pret.getMontantDemande());
            stmt.setInt(2, pret.getDureeMois());
            stmt.setFloat(3, pret.getTaux());
            stmt.setString(4, (pret.getStatut() != null) ? pret.getStatut().name() : "EN_ATTENTE");
            stmt.setString(5, pret.getMotif());
            stmt.setInt(6, pret.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteone(Pret pret) throws SQLException {
        mensualiteService.deleteByPretId(pret.getId());
        String sql = "DELETE FROM pret WHERE id=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, pret.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Pret> selectAll() {
        List<Pret> list = new ArrayList<>();
        String sql = "SELECT * FROM pret";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPret(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Pret> selectAll(Pret t) throws SQLException {
        return selectAll();
    }

    public void genererMensualites(Pret p) throws Exception {
        if (p.getDureeMois() <= 0)
            return;
        double montantMensuel = p.getMontantDemande() / p.getDureeMois();
        for (int i = 1; i <= p.getDureeMois(); i++) {
            Mensualite m = new Mensualite();
            m.setDateEcheance(java.sql.Date.valueOf(java.time.LocalDate.now().plusMonths(i)));
            m.setMontant(montantMensuel);
            m.setEstPaye(false);
            mensualiteService.insertWithPretId(m, p.getId());
        }
    }

    private Pret mapResultSetToPret(ResultSet rs) throws SQLException {
        Pret p = new Pret();
        p.setId(rs.getInt("id"));
        p.setMontantDemande(rs.getDouble("montant_demande"));
        p.setDureeMois(rs.getInt("duree_mois"));
        p.setTaux(rs.getFloat("taux"));
        String st = rs.getString("statut");
        if (st != null)
            p.setStatut(StatutPret.valueOf(st));
        p.setMotif(rs.getString("motif"));
        return p;
    }
}