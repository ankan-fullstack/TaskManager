package parser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Validates and parses calendar dates in YYYY-MM-DD format.
 * Implements strict calendar validation including days-per-month and leap year checks.
 */
public final class DateValidator {

    private DateValidator() {
    }

    /**
     * Checks if the given string represents a valid calendar date in YYYY-MM-DD format.
     * Rejects impossible dates such as 2026-02-30 or 2023-02-29 (non-leap year).
     */
    public static boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }

        String trimmed = dateStr.trim();
        // Strict format check: 4 digits, '-', 2 digits, '-', 2 digits
        if (!trimmed.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return false;
        }

        try {
            String[] parts = trimmed.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            if (year < 1000 || year > 9999) {
                return false;
            }
            if (month < 1 || month > 12) {
                return false;
            }

            int maxDays = getDaysInMonth(year, month);
            return day >= 1 && day <= maxDays;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines whether a given year is a leap year.
     * Rule: Divisible by 4, but not 100, unless also divisible by 400.
     */
    public static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Returns the maximum valid days for a given month and year.
     */
    public static int getDaysInMonth(int year, int month) {
        switch (month) {
            case 1:  // January
            case 3:  // March
            case 5:  // May
            case 7:  // July
            case 8:  // August
            case 10: // October
            case 12: // December
                return 31;
            case 4:  // April
            case 6:  // June
            case 9:  // September
            case 11: // November
                return 30;
            case 2:  // February
                return isLeapYear(year) ? 29 : 28;
            default:
                return 0;
        }
    }

    /**
     * Formats a LocalDate into standard YYYY-MM-DD string.
     */
    public static String format(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Helper to get today's date formatted as YYYY-MM-DD.
     */
    public static String today() {
        return format(LocalDate.now());
    }
}
