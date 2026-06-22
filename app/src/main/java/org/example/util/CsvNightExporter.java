package org.example.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

public final class CsvNightExporter {

    private CsvNightExporter() {
    }

    public static void write(Path csvPath, ArrayList<Night> nights) throws IOException {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            write(bufferedWriter, nights);
        }
    }

    public static void write(Writer writer, ArrayList<Night> nights) throws IOException {
        ArrayList<Night> orderedNights = nights == null ? new ArrayList<Night>() : new ArrayList<Night>(nights);
        orderedNights.sort(Comparator.comparingInt(Night::excelDateSerial));

        ArrayList<Double> sleepDeficits = CalculateGraph.computeSleepDeficits(orderedNights);
        ResultWrapper sleepDebts = CalculateGraph.computeValues(orderedNights);

        BufferedWriter bufferedWriter = writer instanceof BufferedWriter ? (BufferedWriter) writer : new BufferedWriter(writer);
        bufferedWriter.write("Date,Hours,Sleep Deficit,Sleep Debt");
        bufferedWriter.newLine();

        for (int i = 0; i < orderedNights.size(); i++) {
            Night night = orderedNights.get(i);
            bufferedWriter.write(ExcelDateConverter.toDisplayString(night.excelDateSerial()));
            bufferedWriter.write(",");
            bufferedWriter.write(Double.toString(night.hoursSlept()));
            bufferedWriter.write(",");
            bufferedWriter.write(formatNumber(sleepDeficits.get(i)));
            bufferedWriter.write(",");
            bufferedWriter.write(formatNumber(sleepDebts.y().get(i)));
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
    }

    private static String formatNumber(double value) {
        if (Math.abs(value) < 1.0e-12) {
            value = 0.0;
        }
        return Double.toString(value);
    }
}
