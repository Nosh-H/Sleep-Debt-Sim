package org.example.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class CsvNightLoaderTest {

    @Test
    void parsesHeaderedCsvWithIsoAndExcelDates() throws Exception {
        int secondDateSerial = ExcelDateConverter.toExcelSerial(LocalDate.of(2026, 6, 21));
        String csv = """
            Date,Hours
            %d,6.5
            2026-06-20,8.0
            """.formatted(secondDateSerial);

        ArrayList<Night> nights = CsvNightLoader.load(new StringReader(csv));

        assertEquals(2, nights.size());
        assertEquals(ExcelDateConverter.toExcelSerial(LocalDate.of(2026, 6, 20)), nights.get(0).excelDateSerial());
        assertEquals(8.0, nights.get(0).hoursSlept(), 0.0001);
        assertEquals(secondDateSerial, nights.get(1).excelDateSerial());
        assertEquals(6.5, nights.get(1).hoursSlept(), 0.0001);
    }

    @Test
    void excelSerialRoundTripsToLocalDate() {
        LocalDate date = LocalDate.of(2026, 6, 20);

        int serial = ExcelDateConverter.toExcelSerial(date);

        assertEquals(date, ExcelDateConverter.fromExcelSerial(serial));
    }

    @Test
    void load_unsortedWithDuplicates_returnsSortedUnique() throws Exception {
        String csv = "2024-01-12,8.0\n2024-01-10,7.5\n2024-01-12,6.5\n";

        ArrayList<Night> nights = CsvNightLoader.load(new StringReader(csv));
        assertEquals(2, nights.size(), "Should deduplicate duplicate dates and keep later entry");

        int firstSerial = nights.get(0).excelDateSerial();
        int secondSerial = nights.get(1).excelDateSerial();
        assertTrue(firstSerial < secondSerial, "Returned list should be sorted by date serial");

        // Check that the later duplicate (6.5) replaced the earlier (8.0)
        assertEquals(7.5, nights.get(0).hoursSlept(), 0.0001);
        assertEquals(6.5, nights.get(1).hoursSlept(), 0.0001);
    }
}
