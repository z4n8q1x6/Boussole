package tn.esprit.boussole.services;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import tn.esprit.boussole.Utilis.MyBdConnexion;
import tn.esprit.boussole.models.bilan;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceBilan implements crud<bilan> {

    private final Connection cnx;

    public ServiceBilan() {
        this.cnx = MyBdConnexion.getinstance().getCnx();
    }

    @Override
    public void insertone(bilan b) {
        String sql = "INSERT INTO bilan (mois, annee, total_recettes, total_charges, resultat_net, franchise_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getMois());
            ps.setInt(2, b.getAnnee());
            ps.setDouble(3, b.getTotalRecettes());
            ps.setDouble(4, b.getTotalCharges());
            ps.setDouble(5, b.getResultatNet());
            ps.setInt(6, b.getFranchiseId());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    b.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void updateone(bilan b) {
        String sql = "UPDATE bilan SET mois = ?, annee = ?, total_recettes = ?, total_charges = ?, resultat_net = ?, franchise_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, b.getMois());
            ps.setInt(2, b.getAnnee());
            ps.setDouble(3, b.getTotalRecettes());
            ps.setDouble(4, b.getTotalCharges());
            ps.setDouble(5, b.getResultatNet());
            ps.setInt(6, b.getFranchiseId());
            ps.setInt(7, b.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void deleteone(bilan b) {
        String sql = "DELETE FROM bilan WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, b.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<bilan> selectAll() {
        List<bilan> list = new ArrayList<>();
        String sql = "SELECT * FROM bilan";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToBilan(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public void genererBilan(int mois, int annee, int franchiseId) {
        String sqlRecettes = "SELECT COALESCE(SUM(montant), 0.0) AS total FROM transaction WHERE MONTH(date) = ? AND YEAR(date) = ? AND type = 'RECETTE'";
        String sqlDepenses = "SELECT COALESCE(SUM(montant), 0.0) AS total FROM transaction WHERE MONTH(date) = ? AND YEAR(date) = ? AND type = 'DEPENSE'";

        if (franchiseId != 0) {
            sqlRecettes += " AND franchise_id = ?";
            sqlDepenses += " AND franchise_id = ?";
        }

        double totalRecettes = 0.0;
        double totalDepenses = 0.0;

        try (PreparedStatement psRec = cnx.prepareStatement(sqlRecettes);
             PreparedStatement psDep = cnx.prepareStatement(sqlDepenses)) {

            psRec.setInt(1, mois);
            psRec.setInt(2, annee);
            if (franchiseId != 0) psRec.setInt(3, franchiseId);
            
            try (ResultSet rs = psRec.executeQuery()) {
                if (rs.next()) {
                    double val = rs.getDouble("total");
                    if (!rs.wasNull()) {
                        totalRecettes = val;
                    }
                }
            }

            psDep.setInt(1, mois);
            psDep.setInt(2, annee);
            if (franchiseId != 0) psDep.setInt(3, franchiseId);
            
            try (ResultSet rs = psDep.executeQuery()) {
                if (rs.next()) {
                    double val = rs.getDouble("total");
                    if (!rs.wasNull()) {
                        totalDepenses = val;
                    }
                }
            }

            double resultatNet = totalRecettes - totalDepenses;

            bilan b = new bilan();
            b.setMois(mois);
            b.setAnnee(annee);
            b.setTotalRecettes(totalRecettes);
            b.setTotalCharges(totalDepenses);
            b.setResultatNet(resultatNet);
            b.setFranchiseId(franchiseId);

            insertone(b);
            System.out.println("Bilan généré : Mois=" + mois + ", Année=" + annee + ", Recettes=" + totalRecettes + ", Dépenses=" + totalDepenses);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<bilan> getHistorique(int franchiseId) {
        List<bilan> list = new ArrayList<>();
        String sql = "SELECT * FROM bilan ORDER BY annee DESC, mois DESC";
        if (franchiseId != 0) {
            sql = "SELECT * FROM bilan WHERE franchise_id = ? ORDER BY annee DESC, mois DESC";
        }

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            if (franchiseId != 0) ps.setInt(1, franchiseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBilan(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    private bilan mapRowToBilan(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int mois = rs.getInt("mois");
        int annee = rs.getInt("annee");
        double totalRecettes = rs.getDouble("total_recettes");
        double totalCharges = rs.getDouble("total_charges");
        double resultatNet = rs.getDouble("resultat_net");
        int franchiseId = rs.getInt("franchise_id");

        return new bilan(id, mois, annee, totalRecettes, totalCharges, resultatNet, franchiseId);
    }

    /**
     * Exporte un bilan au format PDF. Si iText est disponible, l'utilise pour un PDF propre
     * avec encodage correct des caractères ; sinon fallback vers un générateur minimal.
     */
    public void exporterBilanPDF(bilan b, String cheminFichier) throws IOException {
        // Si iText est disponible, faisons la génération complète
        try {
            Class.forName("com.itextpdf.text.Document");
            // iText present -> generate using iText
            try {
                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
                document.open();

                // Create BaseFont: prefer embedding a TTF (Arial) for full Unicode support
                BaseFont bf;
                String arialPath = "C:\\Windows\\Fonts\\arial.ttf"; // Windows path
                try {
                    java.io.File ar = new java.io.File(arialPath);
                    if (ar.exists()) {
                        bf = BaseFont.createFont(arialPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    } else {
                        // fallback to built-in Helvetica with WinAnsiEncoding
                        bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                    }
                } catch (Exception ex) {
                    bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                }
                Font titleFont = new Font(bf, 16, Font.BOLD);
                Font normalFont = new Font(bf, 12, Font.NORMAL);

                Paragraph title = new Paragraph("Bilan Financier - Mois " + b.getMois() + " / Année " + b.getAnnee(), titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);

                document.add(new Paragraph("\n"));

                Paragraph dateP = new Paragraph("Date de génération : " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date()), normalFont);
                dateP.setAlignment(Element.ALIGN_LEFT);
                document.add(dateP);

                document.add(new Paragraph("\n"));

                PdfPTable table = new PdfPTable(2);
                table.setWidthPercentage(100);

                PdfPCell h1 = new PdfPCell(new Paragraph("Libellé", titleFont));
                PdfPCell h2 = new PdfPCell(new Paragraph("Montant (TND)", titleFont));
                h1.setBackgroundColor(BaseColor.LIGHT_GRAY);
                h2.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(h1);
                table.addCell(h2);

                table.addCell(new PdfPCell(new Paragraph("Total Recettes", normalFont)));
                table.addCell(new PdfPCell(new Paragraph(String.format("%.2f", b.getTotalRecettes()), normalFont)));

                table.addCell(new PdfPCell(new Paragraph("Total Charges", normalFont)));
                table.addCell(new PdfPCell(new Paragraph(String.format("%.2f", b.getTotalCharges()), normalFont)));

                Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
                PdfPCell rLabel = new PdfPCell(new Paragraph("Résultat Net", bold));
                PdfPCell rValue = new PdfPCell(new Paragraph(String.format("%.2f", b.getResultatNet()), bold));
                rLabel.setBackgroundColor(BaseColor.YELLOW);
                rValue.setBackgroundColor(BaseColor.YELLOW);
                table.addCell(rLabel);
                table.addCell(rValue);

                document.add(table);
                document.close();
                System.out.println("✅ PDF iText généré : " + cheminFichier);
                return;
            } catch (DocumentException | IOException e) {
                System.err.println("Erreur iText : " + e.getMessage());
                // fallback to minimal generator
            }
        } catch (ClassNotFoundException e) {
            // iText not present, fallback
        }

        // Fallback: minimal generator
        writeSimplePdf(cheminFichier, b);
        System.out.println("✅ PDF minimal généré (fallback) : " + cheminFichier);
    }

    // Génère un PDF minimal conforme (sans dépendances externes)
    private void writeSimplePdf(String path, bilan b) throws IOException {
        // Use Windows-1252 (Cp1252) so accented chars map correctly for standard Type1 fonts
        Charset enc = Charset.forName("Cp1252");

        // Construire le contenu textuel à afficher (garder les accents)
        String title = "Bilan Financier - Mois " + b.getMois() + " / Année " + b.getAnnee();
        String dateStr = "Date de génération : " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
        String recettes = String.format("Total Recettes: %.2f", b.getTotalRecettes());
        String charges = String.format("Total Charges: %.2f", b.getTotalCharges());
        String resultat = String.format("Résultat Net: %.2f", b.getResultatNet());

        StringBuilder contentSb = new StringBuilder();
        contentSb.append("BT\n");
        contentSb.append("/F1 12 Tf\n");
        int startY = 800;
        int lineHeight = 14;
        contentSb.append(String.format("50 %d Td\n", startY));
        contentSb.append(String.format("(%s) Tj\n", escapePdfString(title)));
        contentSb.append(String.format("0 -%d Td\n", lineHeight));
        contentSb.append(String.format("(%s) Tj\n", escapePdfString(dateStr)));
        contentSb.append(String.format("0 -%d Td\n", lineHeight));
        contentSb.append(String.format("(%s) Tj\n", escapePdfString(recettes)));
        contentSb.append(String.format("0 -%d Td\n", lineHeight));
        contentSb.append(String.format("(%s) Tj\n", escapePdfString(charges)));
        contentSb.append(String.format("0 -%d Td\n", lineHeight));
        contentSb.append(String.format("(%s) Tj\n", escapePdfString(resultat)));
        contentSb.append("ET\n");
        byte[] contentBytes = contentSb.toString().getBytes(enc);

        // Build PDF object strings using encoding
        byte[] header = "%PDF-1.4\n%âãÏÓ\n".getBytes(enc);
        byte[] o1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(enc);
        byte[] o2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(enc);
        byte[] o3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n".getBytes(enc);
        byte[] o4 = "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n".getBytes(enc);
        String obj5Header = "5 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n";
        byte[] obj5 = concat(obj5Header.getBytes(enc), contentBytes, "\nendstream\nendobj\n".getBytes(enc));

        byte[][] objs = new byte[][]{o1, o2, o3, o4, obj5};

        // compute offsets: offsets for objects 1..N relative to file start
        int cur = header.length;
        int[] offsets = new int[objs.length];
        for (int i = 0; i < objs.length; i++) {
            offsets[i] = cur;
            cur += objs[i].length;
        }

        // build xref
        StringBuilder xrefBuilder = new StringBuilder();
        xrefBuilder.append("xref\n0 " + (objs.length + 1) + "\n");
        xrefBuilder.append(String.format("%010d %05d f\n", 0, 65535));
        for (int off : offsets) {
            xrefBuilder.append(String.format("%010d 00000 n\n", off));
        }

        String trailer = "trailer\n<< /Size " + (objs.length + 1) + " /Root 1 0 R >>\nstartxref\n" + cur + "\n%%EOF\n";

        // write everything in one pass
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(header);
            for (byte[] o : objs) fos.write(o);
            fos.write(xrefBuilder.toString().getBytes(enc));
            fos.write(trailer.getBytes(enc));
            fos.flush();
        }
    }


    // helper: concat byte arrays
    private static byte[] concat(byte[] a, byte[] b, byte[] c) {
        byte[] res = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        System.arraycopy(c, 0, res, a.length + b.length, c.length);
        return res;
    }

    // escape parentheses and backslashes in PDF string literals
    private static String escapePdfString(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    public java.util.Map<String, Double[]> getDonneesGraphique(int moisHistorique) {
        java.util.Map<String, Double[]> result = new java.util.LinkedHashMap<>();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        // Start from current month, go back moisHistorique-1 months
        for (int i = moisHistorique - 1; i >= 0; i--) {
            cal = (java.util.Calendar) java.util.Calendar.getInstance().clone();
            cal.add(java.util.Calendar.MONTH, -i);
            int month = cal.get(java.util.Calendar.MONTH) + 1; // 1-based
            int year = cal.get(java.util.Calendar.YEAR);
            String key = String.format("%02d/%d", month, year);

            double totalRecettes = 0.0;
            double totalDepenses = 0.0;

            String sqlRec = "SELECT COALESCE(SUM(montant),0.0) AS total FROM transaction WHERE MONTH(date) = ? AND YEAR(date) = ? AND type = 'RECETTE'";
            String sqlDep = "SELECT COALESCE(SUM(montant),0.0) AS total FROM transaction WHERE MONTH(date) = ? AND YEAR(date) = ? AND type = 'DEPENSE'";
            try (PreparedStatement psRec = cnx.prepareStatement(sqlRec);
                 PreparedStatement psDep = cnx.prepareStatement(sqlDep)) {
                psRec.setInt(1, month);
                psRec.setInt(2, year);
                try (ResultSet rs = psRec.executeQuery()) {
                    if (rs.next()) {
                        totalRecettes = rs.getDouble("total");
                    }
                }
                psDep.setInt(1, month);
                psDep.setInt(2, year);
                try (ResultSet rs = psDep.executeQuery()) {
                    if (rs.next()) {
                        totalDepenses = rs.getDouble("total");
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            result.put(key, new Double[]{totalRecettes, totalDepenses});
        }
        return result;
    }
}
