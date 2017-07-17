package com.adobe.acs.epic;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Data utilities
 */
public class DataUtils {

    static final private SimpleDateFormat dateFormat = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z");

    private DataUtils() {
        // Utility class, no constructor
    }

    public static Date parseDate(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        } else {
            try {
                return dateFormat.parse(str);
            } catch (ParseException ex) {
                Logger.getLogger(DataUtils.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }

    public static String getHumanSize(Object val) {
        if (val == null) {
            return null;
        }
        Long bytes = (val instanceof Long) ? (Long) val : Long.parseLong(String.valueOf(val));
        if (bytes < 1024) {
            return bytes + " b";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "kmgtpe".charAt(exp - 1);
        return String.format("%.1f %cb", bytes / Math.pow(1024, exp), pre);
    }

    public static int compareDates(Object o1, Object o2) {
        Date d1 = DataUtils.parseDate(String.valueOf(o1));
        Date d2 = DataUtils.parseDate(String.valueOf(o2));
        if (d1 == null && d2 == null) {
            return 0;
        } else if (d1 == null) {
            return 1;
        } else if (d2 == null) {
            return -1;
        } else {
            return d2.compareTo(d1);
        }
    }

    public static <T> void exportSpreadsheet(OutputStream target, Collection<T> data, String[] header, Function<T, Object>... getters) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet();
        int headerRows = 0;
        if (header != null) {
            headerRows++;
            XSSFRow headerRow = sheet.createRow(0);
            for (int col = 0; col < header.length; col++) {
                XSSFCell cell = headerRow.createCell(col);
                cell.setCellValue(header[col]);
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
                if (val instanceof Number) {
                    Number n = (Number) val;
                    cell.setCellValue(((Number) val).doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(val));
                }
            }
        }
        workbook.write(target);
    }

    public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                    public T next() {
                        return e.nextElement();
                    }

                    public boolean hasNext() {
                        return e.hasMoreElements();
                    }
                }, Spliterator.ORDERED), false);
    }

    public static String summarizeUserDateCombo(String date, String user) {
        if (date == null || date.isEmpty()) {
            return "N/A";
        } else if (user == null || user.isEmpty()) {
            user = "Unknown";
        }
        return date + " by " + user;
    }
}
