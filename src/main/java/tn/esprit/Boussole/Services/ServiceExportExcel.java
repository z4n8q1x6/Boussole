package tn.esprit.Boussole.Services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tn.esprit.Boussole.Models.transaction;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ServiceExportExcel {

    public void exporterTransactions(List<transaction> liste, String cheminFichier) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");

            // Creation des styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle montantStyle = createMontantStyle(workbook);
            CellStyle depenseStyle = createDepenseStyle(workbook);
            CellStyle recetteStyle = createRecetteStyle(workbook);
            CellStyle defaultStyle = workbook.createCellStyle();
            defaultStyle.setAlignment(HorizontalAlignment.LEFT);

            // En-tête
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Date", "Type", "Description", "Montant"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplissage des données
            int rowNum = 1;
            for (transaction t : liste) {
                Row row = sheet.createRow(rowNum++);

                // Date
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(t.getDate().toString());
                dateCell.setCellStyle(dateStyle);

                // Type
                Cell typeCell = row.createCell(1);
                typeCell.setCellValue(t.getType() != null ? t.getType().name() : "");
                typeCell.setCellStyle(defaultStyle);

                // Description
                Cell descCell = row.createCell(2);
                descCell.setCellValue(t.getDescription());
                descCell.setCellStyle(defaultStyle);

                // Montant
                Cell montantCell = row.createCell(3);
                montantCell.setCellValue(t.getMontant());
                
                // Style conditionnel pour Montant
                if (t.getType() == transaction.Type.DEPENSE) {
                    montantCell.setCellStyle(depenseStyle);
                } else if (t.getType() == transaction.Type.RECETTE) {
                    montantCell.setCellStyle(recetteStyle);
                } else {
                    montantCell.setCellStyle(montantStyle);
                }
            }

            // Ajustement automatique des colonnes
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Ecriture dans le fichier
            try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
                workbook.write(fileOut);
            }
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // Fond Bleu Nuit (#0F172A approx. ou DARK_BLUE)
        // POI uses indexed colors or custom byte rgb
        // Using XSSF custom color
        org.apache.poi.xssf.usermodel.XSSFCellStyle xssfStyle = (org.apache.poi.xssf.usermodel.XSSFCellStyle) style;
        xssfStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte)15, (byte)23, (byte)42}, null));
        xssfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createMontantStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00 \\\"TND\\\""));
        return style;
    }

    private CellStyle createDepenseStyle(Workbook workbook) {
        CellStyle style = createMontantStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createRecetteStyle(Workbook workbook) {
        CellStyle style = createMontantStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.GREEN.getIndex());
        style.setFont(font);
        return style;
    }
}
