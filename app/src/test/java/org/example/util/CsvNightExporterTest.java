package org.example.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class CsvNightExporterTest {

  @Test
  void writesSortedCsvWithDeficitAndDebtColumns() throws Exception {
    ArrayList<Night> nights = new ArrayList<Night>();
    nights.add(new Night(ExcelDateConverter.toExcelSerial(LocalDate.of(2026, 6, 22)), 6.0));
    nights.add(new Night(ExcelDateConverter.toExcelSerial(LocalDate.of(2026, 6, 20)), 8.0));

    StringWriter writer = new StringWriter();
    CsvNightExporter.write(writer, nights);

    String[] lines = writer.toString().trim().split("\\R");

    assertEquals("Date,Hours,Sleep Deficit,Sleep Debt", lines[0]);
    assertEquals("2026-06-20,8.0,0.0,0.0", lines[1]);
    assertEquals("2026-06-22,6.0,2.0,-2.0", lines[2]);
  }

  @Test
  void handlesEmptyNightList() throws Exception {
    StringWriter writer = new StringWriter();

    CsvNightExporter.write(writer, new ArrayList<Night>());

    assertEquals("Date,Hours,Sleep Deficit,Sleep Debt", writer.toString().trim());
  }
}
