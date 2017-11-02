package com.adobe.acs.epic.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ReportUtil {

    public static <T> void exportSpreadsheet(OutputStream target, Collection<T> data, String[] header, Function<T, Object>... getters) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        addSheet(null, workbook, data, header, getters);
        workbook.write(target);
    }

    public static <T> void addSheet(String title, XSSFWorkbook workbook, Collection<T> data, String[] header, Function<T, Object>... getters) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle cellStyle = createCellStyle(workbook);        
        XSSFSheet sheet = title != null ? workbook.createSheet(title) : workbook.createSheet();
        sheet.setDisplayRowColHeadings(false);
        sheet.createFreezePane(0, 1);
        if (data.isEmpty()) {
            sheet.createRow(0).createCell(0).setCellValue("There were no entries");
            return;
        }
        int headerRows = 0;
        if (header != null) {
            headerRows++;
            XSSFRow headerRow = sheet.createRow(0);
            headerRow.setRowStyle(headerStyle);
            for (int col = 0; col < header.length; col++) {
                XSSFCell cell = headerRow.createCell(col);
                cell.setCellValue(header[col]);
                cell.setCellStyle(headerStyle);
            }
        }
        Iterator<T> iter = data.iterator();
        for (int r = 0; r < data.size(); r++) {
            XSSFRow row = sheet.createRow(r + headerRows);
            T rowData = iter.next();
            for (int col = 0; col < getters.length; col++) {
                Object val = getters[col].apply(rowData);
                if (val == null) {
                    continue;
                }
                XSSFCell cell = row.createCell(col);
                cell.setCellStyle(cellStyle);
                if (val instanceof Number) {
                    Number n = (Number) val;
                    cell.setCellValue(((Number) val).doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(val));
                }
            }
        }
        ReportUtil.autosizeColumns(sheet, getters.length);
        sheet.setAutoFilter(new CellRangeAddress(0, data.size() + 1, 0, getters.length));
    }

    private static XSSFCellStyle createCellStyle(Workbook workbook) {
        XSSFCellStyle xstyle = (XSSFCellStyle) workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        xstyle.setFont(font);
        return xstyle;
    }

    public static CellStyle createHeaderStyle(Workbook workbook) {
        XSSFCellStyle headerStyle = createCellStyle(workbook);
        XSSFColor header = new XSSFColor(new byte[]{(byte) 79, (byte) 129, (byte) 189});
        headerStyle.setFillForegroundColor(header);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.getFont().setColor(IndexedColors.WHITE.index);
        return headerStyle;
    }

    public static int MIN_COL_WIDTH = 6 << 8;
    public static int MAX_COL_WIDTH = 120 << 8;
    
    public static void autosizeColumns(Sheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++) {
            sheet.autoSizeColumn(i);
            int cw = (int) (sheet.getColumnWidth(i) * 0.8);
            // increase width to accommodate drop-down arrow in the header
            sheet.setColumnWidth(i, Math.max(Math.min(cw, MAX_COL_WIDTH), MIN_COL_WIDTH));
        }
    }

    private ReportUtil() {
        // Utility class has no public constructor
    }
}
