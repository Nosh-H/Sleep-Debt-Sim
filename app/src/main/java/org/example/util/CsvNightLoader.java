package org.example.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * CSV loader for Night records.
 *
 * <p>Reasons for using Apache Commons CSV: - RFC4180-compliant parsing (handles quoted fields,
 * embedded commas, escaped quotes) - Small, well-tested library with a stable API - Avoids fragile
 * String.split(",") parsing which breaks on quoted commas or locale variants
 *
 * <p>Parsing strategy (kept intentionally simple and well-documented): 1. Parse the input using
 * CSVParser with default format (comma-separated). This accepts quoted fields and embedded commas
 * correctly. 2. Iterate records in order. Skip entirely blank records. 3. For each record, require
 * at least two columns (date, hours). If fewer, throw IOException. 4. For the first record only: if
 * parsing fails and the first row looks like a header (e.g. contains the word "date" or "hour"),
 * skip it. This preserves legacy behavior while still parsing headerless CSVs correctly. 5. Collect
 * all parsed nights, then sort and deduplicate by excel serial, keeping the later duplicate when
 * dates repeat (same behavior as prior implementation).
 */
public final class CsvNightLoader {

  private CsvNightLoader() {}

  public static ArrayList<Night> load(Path csvPath) throws IOException {
    try (Reader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
      return load(reader);
    }
  }

  public static ArrayList<Night> load(Reader reader) throws IOException {
    ArrayList<Night> nights = new ArrayList<Night>();

    // CSVParser will correctly handle quoted fields and embedded commas; use DEFAULT which is
    // a common CSV layout (comma delimiter, double-quote for quoting).
    try (CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {
      int lineNumber = 0;
      for (CSVRecord record : parser) {
        lineNumber = (int) record.getRecordNumber();

        // Skip empty/blank lines
        boolean allBlank = true;
        for (String v : record) {
          if (v != null && !v.trim().isEmpty()) {
            allBlank = false;
            break;
          }
        }
        if (allBlank) {
          continue;
        }

        if (record.size() < 2) {
          throw new IOException("Line " + lineNumber + " must contain a date and hours slept.");
        }

        try {
          nights.add(parseNight(record.get(0), record.get(1)));
        } catch (RuntimeException ex) {
          // Preserve the convenience from the previous loader: if the very first line looks like a
          // header (e.g., Date,Hours), tolerate it and continue. For any other parse error, fail
          // with a helpful message including the original text.
          if (lineNumber == 1 && looksLikeHeader(record.get(0), record.get(1))) {
            continue;
          }
          throw new IOException(
              "Could not parse CSV line " + lineNumber + ": " + record.toString(), ex);
        }
      }
    }

    // Sort and deduplicate (keep later entries when duplicates exist) — same post-processing as
    // the original implementation.
    nights.sort(Comparator.comparingInt(Night::excelDateSerial));

    ArrayList<Night> unique = new ArrayList<Night>();
    for (Night n : nights) {
      if (unique.isEmpty()
          || unique.get(unique.size() - 1).excelDateSerial() != n.excelDateSerial()) {
        unique.add(n);
      } else {
        unique.set(unique.size() - 1, n);
      }
    }

    return unique;
  }

  private static Night parseNight(String dateToken, String hoursToken) {
    int dateSerial = ExcelDateConverter.toExcelSerial(ExcelDateConverter.parseDateToken(dateToken));
    double hoursSlept = Double.parseDouble(hoursToken.trim());
    return new Night(dateSerial, hoursSlept);
  }

  private static boolean looksLikeHeader(String firstColumn, String secondColumn) {
    String first = firstColumn == null ? "" : firstColumn.trim().toLowerCase(Locale.US);
    String second = secondColumn == null ? "" : secondColumn.trim().toLowerCase(Locale.US);
    return first.contains("date") || first.contains("serial") || second.contains("hour");
  }
}
