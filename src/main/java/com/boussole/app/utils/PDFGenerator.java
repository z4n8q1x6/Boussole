package com.boussole.app.utils;

import com.boussole.app.models.AlerteIA;
import com.boussole.app.services.AlerteIAService;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

public class PDFGenerator {

  public static String generateAlertePDF(Stage parentStage) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save Alertes PDF");
    fileChooser.setInitialFileName("alertes.pdf");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
    File file = fileChooser.showSaveDialog(parentStage);

    if (file == null) {
      return "";
    }

    String filename = file.getAbsolutePath();
    Document document = new Document(PageSize.A3, 18, 18, 36, 36);

    try {
      PdfWriter.getInstance(document, new FileOutputStream(filename));
      document.open();
      PdfPTable table = new PdfPTable(5);
      table.setWidthPercentage(100);
      table.setWidths(new float[] {1f, 1.5f, 1f, 1f, 5.5f});

      // headers
      Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
      String[] headers = {"Franchise", "Type", "Gravité", "Date", "Message"};
      for (String header : headers) {
        PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        table.addCell(cell);
      }

      // data
      AlerteIAService service = new AlerteIAService();
      ObservableList<AlerteIA> alerteIAs = service.getAll();
      Font dataFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
      for (AlerteIA a : alerteIAs) {
        PdfPCell cell;

        cell = new PdfPCell(new Phrase(String.valueOf(a.getId()), dataFont));
        cell.setPadding(5);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(a.getType_alerte(), dataFont));
        cell.setPadding(5);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(String.valueOf(a.getScore_gravite()), dataFont));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase((a.getDate_detection().toString()), dataFont));
        cell.setPadding(5);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(a.getMessage(), dataFont));
        cell.setPadding(5);
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
