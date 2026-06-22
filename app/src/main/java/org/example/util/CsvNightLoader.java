package org.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public final class CsvNightLoader {

    private CsvNightLoader() {
    }

    public static ArrayList<Night> load(Path csvPath) throws IOException {
        try (BufferedReader bufferedReader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            return load(bufferedReader);
        }
    }

    public static ArrayList<Night> load(Reader reader) throws IOException {
        ArrayList<Night> nights = new ArrayList<Night>();

        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            int lineNumber = 0;

            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }

                String[] columns = trimmedLine.split(",", -1);
                if (columns.length < 2) {
                    throw new IOException("Line " + lineNumber + " must contain a date and hours slept.");
                }

                try {
                    nights.add(parseNight(columns));
                } catch (RuntimeException ex) {
                    if (lineNumber == 1 && looksLikeHeader(columns)) {
                        continue;
                    }
                    throw new IOException("Could not parse CSV line " + lineNumber + ": " + trimmedLine, ex);
                }
            }
        }

        nights.sort(Comparator.comparingInt(Night::excelDateSerial));
        return nights;
    }

    private static Night parseNight(String[] columns) {
        int dateSerial = ExcelDateConverter.toExcelSerial(ExcelDateConverter.parseDateToken(columns[0]));
        double hoursSlept = Double.parseDouble(columns[1].trim());
        return new Night(dateSerial, hoursSlept);
    }

    private static boolean looksLikeHeader(String[] columns) {
        String firstColumn = columns[0].trim().toLowerCase(Locale.US);
        String secondColumn = columns[1].trim().toLowerCase(Locale.US);
        return firstColumn.contains("date") || firstColumn.contains("serial") || secondColumn.contains("hour");
    }
}
