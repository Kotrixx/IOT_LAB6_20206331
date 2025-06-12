package com.example.lab6_20206331.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    // Formatos de fecha comunes
    public static final String FORMAT_DD_MM_YYYY = "dd/MM/yyyy";
    public static final String FORMAT_DD_MM_YYYY_HH_MM = "dd/MM/yyyy HH:mm";
    public static final String FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String FORMAT_DD_MMM_YYYY = "dd MMM yyyy";
    public static final String FORMAT_FULL_DATE = "EEEE, dd MMMM yyyy";
    public static final String FORMAT_MONTH_YEAR = "MMMM yyyy";
    public static final String FORMAT_FIRESTORE = "yyyy-MM-dd'T'HH:mm:ss";

    // Formatters
    private static final SimpleDateFormat FORMATTER_DD_MM_YYYY =
            new SimpleDateFormat(FORMAT_DD_MM_YYYY, new Locale("es", "PE"));

    private static final SimpleDateFormat FORMATTER_DD_MM_YYYY_HH_MM =
            new SimpleDateFormat(FORMAT_DD_MM_YYYY_HH_MM, new Locale("es", "PE"));

    private static final SimpleDateFormat FORMATTER_DD_MMM_YYYY =
            new SimpleDateFormat(FORMAT_DD_MMM_YYYY, new Locale("es", "PE"));

    private static final SimpleDateFormat FORMATTER_FULL_DATE =
            new SimpleDateFormat(FORMAT_FULL_DATE, new Locale("es", "PE"));

    private static final SimpleDateFormat FORMATTER_MONTH_YEAR =
            new SimpleDateFormat(FORMAT_MONTH_YEAR, new Locale("es", "PE"));

    // ================= MÉTODOS DE FORMATEO =================

    /**
     * Formatea fecha como dd/MM/yyyy
     */
    public static String formatDate(Date date) {
        if (date == null) return "";
        return FORMATTER_DD_MM_YYYY.format(date);
    }

    /**
     * Formatea fecha como dd/MM/yyyy HH:mm
     */
    public static String formatDateTime(Date date) {
        if (date == null) return "";
        return FORMATTER_DD_MM_YYYY_HH_MM.format(date);
    }

    /**
     * Formatea fecha como dd MMM yyyy (ej: 15 May 2024)
     */
    public static String formatDateShort(Date date) {
        if (date == null) return "";
        return FORMATTER_DD_MMM_YYYY.format(date);
    }

    /**
     * Formatea fecha completa (ej: Lunes, 15 Mayo 2024)
     */
    public static String formatDateFull(Date date) {
        if (date == null) return "";
        return FORMATTER_FULL_DATE.format(date);
    }

    /**
     * Formatea mes y año (ej: Mayo 2024)
     */
    public static String formatMonthYear(Date date) {
        if (date == null) return "";
        return FORMATTER_MONTH_YEAR.format(date);
    }

    /**
     * Formatea fecha con formato personalizado
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null || pattern == null) return "";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, new Locale("es", "PE"));
        return formatter.format(date);
    }

    // ================= MÉTODOS DE PARSING =================

    /**
     * Convierte string dd/MM/yyyy a Date
     */
    public static Date parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return null;

        try {
            return FORMATTER_DD_MM_YYYY.parse(dateString.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Convierte string con formato personalizado a Date
     */
    public static Date parseDate(String dateString, String pattern) {
        if (dateString == null || pattern == null) return null;

        try {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern, new Locale("es", "PE"));
            return formatter.parse(dateString.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    // ================= MÉTODOS DE COMPARACIÓN =================

    /**
     * Verifica si dos fechas son del mismo día
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Verifica si dos fechas son del mismo mes y año
     */
    public static boolean isSameMonth(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    /**
     * Verifica si una fecha está en el mes y año especificados
     */
    public static boolean isInMonth(Date date, int month, int year) {
        if (date == null) return false;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal.get(Calendar.YEAR) == year &&
                cal.get(Calendar.MONTH) == (month - 1); // Calendar.MONTH es 0-based
    }

    /**
     * Verifica si una fecha es hoy
     */
    public static boolean isToday(Date date) {
        return isSameDay(date, new Date());
    }

    /**
     * Verifica si una fecha es ayer
     */
    public static boolean isYesterday(Date date) {
        if (date == null) return false;

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        return isSameDay(date, yesterday.getTime());
    }

    /**
     * Verifica si una fecha es de esta semana
     */
    public static boolean isThisWeek(Date date) {
        if (date == null) return false;

        Calendar now = Calendar.getInstance();
        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTime(date);

        return now.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == dateCalendar.get(Calendar.WEEK_OF_YEAR);
    }

    // ================= MÉTODOS DE CÁLCULO =================

    /**
     * Obtiene el primer día del mes
     */
    public static Date getFirstDayOfMonth(int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // month es 1-based, Calendar.MONTH es 0-based
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Obtiene el último día del mes
     */
    public static Date getLastDayOfMonth(int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // month es 1-based, Calendar.MONTH es 0-based
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * Obtiene el rango de fechas para un mes específico
     */
    public static DateRange getMonthRange(int month, int year) {
        Date start = getFirstDayOfMonth(month, year);
        Date end = getLastDayOfMonth(month, year);
        return new DateRange(start, end);
    }

    /**
     * Añade días a una fecha
     */
    public static Date addDays(Date date, int days) {
        if (date == null) return null;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    /**
     * Añade meses a una fecha
     */
    public static Date addMonths(Date date, int months) {
        if (date == null) return null;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, months);
        return cal.getTime();
    }

    /**
     * Calcula la diferencia en días entre dos fechas
     */
    public static long getDaysBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) return 0;

        long diffInMillies = Math.abs(endDate.getTime() - startDate.getTime());
        return diffInMillies / (24 * 60 * 60 * 1000);
    }

    // ================= MÉTODOS UTILITARIOS =================

    /**
     * Obtiene la fecha actual sin hora (solo día/mes/año)
     */
    public static Date getCurrentDateOnly() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Remueve la parte de tiempo de una fecha
     */
    public static Date removeTime(Date date) {
        if (date == null) return null;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Obtiene el mes actual (1-12)
     */
    public static int getCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.MONTH) + 1; // Calendar.MONTH es 0-based
    }

    /**
     * Obtiene el año actual
     */
    public static int getCurrentYear() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR);
    }

    /**
     * Obtiene el nombre del mes en español
     */
    public static String getMonthName(int month) {
        String[] meses = {
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };

        if (month >= 1 && month <= 12) {
            return meses[month - 1];
        }
        return "Mes desconocido";
    }

    /**
     * Formatea fecha relativa (hoy, ayer, etc.)
     */
    public static String getRelativeDateString(Date date) {
        if (date == null) return "";

        if (isToday(date)) {
            return "Hoy";
        } else if (isYesterday(date)) {
            return "Ayer";
        } else if (isThisWeek(date)) {
            return formatDate(date, "EEEE"); // Día de la semana
        } else {
            return formatDateShort(date);
        }
    }

    // ================= CLASE AUXILIAR PARA RANGOS =================

    public static class DateRange {
        private final Date startDate;
        private final Date endDate;

        public DateRange(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public Date getStartDate() {
            return startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public boolean contains(Date date) {
            if (date == null || startDate == null || endDate == null) {
                return false;
            }
            return !date.before(startDate) && !date.after(endDate);
        }

        @Override
        public String toString() {
            return formatDate(startDate) + " - " + formatDate(endDate);
        }
    }
}