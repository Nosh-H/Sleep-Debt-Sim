package org.example.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                2026-06-20,8.0
                %d,6.5
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
}
