package tn.esprit.boussole.utils;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import tn.esprit.boussole.models.AlerteIA;
import tn.esprit.boussole.service.AlerteIAService;

public class PDFGenerator {

  /**
   * Generate PDF and save to user-selected location (original behavior).
   * @param parentStage The parent stage for file chooser dialog
   * @return "generated" on success, error message otherwise
   */
  public static String generateAlertePDF(Stage parentStage) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save Alertes PDF");
    fileChooser.setInitialFileName("alertes.pdf");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
    File file = fileChooser.showSaveDialog(parentStage);
    if (file == null) return "";

    return generatePDFContent(file.getAbsolutePath());
  }

  /**
   * Generate PDF and save to a temporary file.
   * This is used for cloud upload flow.
   * @return File object if successful, null otherwise
   */
  public static File generateAlertePDFToTemp() {
    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      File tempFile = File.createTempFile("rapport_" + timestamp, ".pdf");
      String result = generatePDFContent(tempFile.getAbsolutePath());
      if (result.equals("generated")) {
        return tempFile;
      } else {
        System.err.println("Failed to generate PDF: " + result);
        tempFile.delete();
        return null;
      }
    } catch (Exception e) {
      System.err.println("Error creating temp file: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Core PDF generation logic.
   * @param filename The absolute path where the PDF will be saved
   * @return "generated" on success, error message otherwise
   */
  private static String generatePDFContent(String filename) {
    Document document = new Document(PageSize.A2.rotate(), 30, 30, 40, 40);

    try {
      PdfWriter.getInstance(document, new FileOutputStream(filename));
      document.open();

      PdfPTable table = new PdfPTable(5);
      table.setWidthPercentage(100);
      table.setWidths(new float[] {1f, 2f, 1f, 1.8f, 6f});

      // Headers
      Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD);
      String[] headers = {"Franchise", "Type", "Gravité", "Date", "Message"};
      for (String header : headers) {
        PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        table.addCell(cell);
      }

      // Data
      AlerteIAService service = new AlerteIAService();
      ObservableList<AlerteIA> alerteIAs = service.getAll();
      Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

      for (AlerteIA a : alerteIAs) {
        PdfPCell cell;

        cell = new PdfPCell(new Phrase(String.valueOf(a.getFranchiseId()), dataFont));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(a.getType_alerte(), dataFont));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(String.valueOf(a.getScore_gravite()), dataFont));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(a.getDate_detection().toString(), dataFont));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        table.addCell(cell);

        // Message — wrap and limit length
        String message = a.getMessage();
        if (message != null && message.length() > 600) {
          message = message.substring(0, 600) + "...";
        }
        cell = new PdfPCell(new Phrase(message, dataFont));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        table.addCell(cell);
      }

      document.add(table);
      document.close();
    } catch (Exception e) {
      return e.getMessage();
    }
    return "generated";
  }
}
