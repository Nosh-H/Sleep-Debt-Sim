package org.example.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class ExcelDateConverter {
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 31);
    private static final LocalDate EXCEL_LEAP_BUG_CUTOFF = LocalDate.of(1900, 3, 1);

    private ExcelDateConverter() {
    }

    public static int toExcelSerial(LocalDate date) {
        int serial = (int) ChronoUnit.DAYS.between(EXCEL_EPOCH, date);
        if (!date.isBefore(EXCEL_LEAP_BUG_CUTOFF)) {
            serial += 1;
        }
        return serial;
    }

    public static LocalDate fromExcelSerial(int serial) {
        if (serial < 1) {
            throw new IllegalArgumentException("Excel serial dates must be 1 or greater.");
        }

        if (serial == 60) {
            throw new IllegalArgumentException("Serial 60 represents the non-existent Feb 29, 1900.");
        }

        LocalDate date = EXCEL_EPOCH.plusDays(serial);
        if (serial > 60) {
            date = date.minusDays(1);
        }
        return date;
    }

    public static LocalDate parseDateToken(String rawValue) {
        String trimmedValue = rawValue == null ? "" : rawValue.trim();
        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException("Date value cannot be blank.");
        }

        try {
            return fromExcelSerial(Integer.parseInt(trimmedValue));
        } catch (NumberFormatException ignored) {
            // Fall through to textual formats.
        }

        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US),
            DateTimeFormatter.ofPattern("M/d/uu", Locale.US)
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(trimmedValue, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }

        throw new IllegalArgumentException("Unsupported date format: " + rawValue);
    }

    public static String toDisplayString(int serial) {
        return fromExcelSerial(serial).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
