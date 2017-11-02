package com.adobe.acs.epic.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Data utilities
 */
public class DataUtil {

    static final private SimpleDateFormat dateFormat = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z");

    private DataUtil() {
        // Utility class, no constructor
    }

    public static Date parseDate(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        } else {
            try {
                return dateFormat.parse(str);
            } catch (ParseException ex) {
                Logger.getLogger(DataUtil.class.getName()).log(Level.SEVERE, null, ex);
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
        Date d1 = DataUtil.parseDate(String.valueOf(o1));
        Date d2 = DataUtil.parseDate(String.valueOf(o2));
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
