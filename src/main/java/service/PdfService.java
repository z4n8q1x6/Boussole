package service;

import entity.Pret;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.DeviceRgb;

import java.io.FileOutputStream;
import java.util.List;

public class PdfService {

    public void genererRapportPrets(List<Pret> prets) {
        // Le fichier sera créé sur ton bureau
        String dest = System.getProperty("user.home") + "/Desktop/Rapport_Boussole_Prets.pdf";

        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(dest));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Titre du PDF stylisé (Couleur bleu foncé #2c3e50)
            document.add(new Paragraph("BOUSSOLE - RAPPORT DES PRÊTS")
                    .setBold().setFontSize(20).setFontColor(new DeviceRgb(44, 62, 80)));

            document.add(new Paragraph("Généré le : " + java.time.LocalDate.now() + "\n\n"));

            // Création d'un tableau de 5 colonnes avec largeurs relatives
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 1}));
            table.setWidth(UnitValue.createPercentValue(100));

            // En-têtes du tableau (Couleur bleu clair #3498db)
            String[] headers = {"Motif", "Montant", "Statut", "Taux", "Mois"};
            for (String h : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(h))
                        .setBackgroundColor(new DeviceRgb(52, 152, 219))
                        .setFontColor(DeviceRgb.WHITE));
            }

            // Ajout des données avec correction des noms de méthodes
            for (Pret p : prets) {
                // Colonne Motif
                table.addCell(new Cell().add(new Paragraph(p.getMotif())));

                // Colonne Montant (Correction : getMontantDemande)
                table.addCell(new Cell().add(new Paragraph(p.getMontantDemande() + " DT")));

                // Colonne Statut (Correction : conversion de l'Enum en String via .name())
                String statutTexte = (p.getStatut() != null) ? p.getStatut().name() : "N/A";
                table.addCell(new Cell().add(new Paragraph(statutTexte)));

                // Colonne Taux (Correction : getTaux)
                table.addCell(new Cell().add(new Paragraph(p.getTaux() + "%")));

                // Colonne Durée
                table.addCell(new Cell().add(new Paragraph(String.valueOf(p.getDureeMois()))));
            }

            document.add(table);
            document.close();
            System.out.println("PDF généré avec succès sur le bureau !");

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du PDF : " + e.getMessage());
            e.printStackTrace();
        }
    }
}