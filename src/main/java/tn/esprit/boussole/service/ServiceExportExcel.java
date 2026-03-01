package tn.esprit.boussole.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tn.esprit.boussole.models.transaction;

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
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subtitleStyle = createSubtitleStyle(workbook);
            CellStyle totalRowStyle = createTotalRowStyle(workbook);

            // Titre Principal
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Journal des Opérations - Franchise");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));
            titleRow.setHeightInPoints(30);

            // Date de génération
            Row dateRow = sheet.createRow(1);
            Cell genDateCell = dateRow.createCell(0);
            genDateCell.setCellValue("Généré le: " + java.time.LocalDate.now().toString());
            genDateCell.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 3));

            // Ligne vide
            sheet.createRow(2);

            // En-tête du tableau
            Row headerRow = sheet.createRow(3);
            String[] columns = {"Date", "Type", "Description", "Montant"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplissage des données
            int rowNum = 4;
            double totalRecettes = 0;
            double totalDepenses = 0;

            for (transaction t : liste) {
                Row row = sheet.createRow(rowNum++);

                // Date
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(t.getDate().toString());
                cell0.setCellStyle(dateStyle);

                // Type
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(t.getType() != null ? t.getType().name() : "");
                cell1.setCellStyle(defaultStyle);

                // Description
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(t.getDescription());
                cell2.setCellStyle(defaultStyle);

                // Montant
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(t.getMontant());
                
                if (t.getType() == transaction.Type.DEPENSE) {
                    cell3.setCellStyle(depenseStyle);
                    totalDepenses += t.getMontant();
                } else if (t.getType() == transaction.Type.RECETTE) {
                    cell3.setCellStyle(recetteStyle);
                    totalRecettes += t.getMontant();
                } else {
                    cell3.setCellStyle(montantStyle);
                }
            }

            // Ligne vide avant les totaux
            sheet.createRow(rowNum++);

            // Row: Total Recettes
            Row rowRec = sheet.createRow(rowNum++);
            Cell cellR1 = rowRec.createCell(2);
            cellR1.setCellValue("Total Recettes:");
            cellR1.setCellStyle(totalRowStyle);
            Cell cellR2 = rowRec.createCell(3);
            cellR2.setCellValue(totalRecettes);
            cellR2.setCellStyle(recetteStyle);

            // Row: Total Dépenses
            Row rowDep = sheet.createRow(rowNum++);
            Cell cellD1 = rowDep.createCell(2);
            cellD1.setCellValue("Total Charges:");
            cellD1.setCellStyle(totalRowStyle);
            Cell cellD2 = rowDep.createCell(3);
            cellD2.setCellValue(totalDepenses);
            cellD2.setCellStyle(depenseStyle);

            // Row: Solde Final
            Row rowSolde = sheet.createRow(rowNum++);
            Cell cellS1 = rowSolde.createCell(2);
            cellS1.setCellValue("Solde Net:");
            cellS1.setCellStyle(totalRowStyle);
            Cell cellS2 = rowSolde.createCell(3);
            cellS2.setCellValue(totalRecettes - totalDepenses);
            cellS2.setCellStyle(createSoldeStyle(workbook, totalRecettes - totalDepenses));

            // Ajustement automatique des colonnes
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            // Add extra width to Montant column to fit totals gracefully
            sheet.setColumnWidth(3, sheet.getColumnWidth(3) + 2000);

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

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubtitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTotalRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createSoldeStyle(Workbook workbook, double solde) {
        CellStyle style = createMontantStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        if (solde > 0) {
            font.setColor(IndexedColors.GREEN.getIndex());
        } else if (solde < 0) {
            font.setColor(IndexedColors.RED.getIndex());
        } else {
            font.setColor(IndexedColors.BLACK.getIndex());
        }
        style.setFont(font);
        
        // Add top border to emphasize total
        style.setBorderTop(BorderStyle.DOUBLE);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }
}
