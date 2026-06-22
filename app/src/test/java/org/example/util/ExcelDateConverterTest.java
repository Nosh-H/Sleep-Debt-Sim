package org.example.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ExcelDateConverterTest {

    @Test
    void fromExcelSerialRoundTripsValidNearbySerialsAndRejectsInvalidExcelSerialSixty() {
        for (int serial = 58; serial <= 63; serial++) {
            final int currentSerial = serial;
            if (currentSerial == 60) {
                assertThrows(IllegalArgumentException.class, () -> ExcelDateConverter.fromExcelSerial(currentSerial));
                continue;
            }

            LocalDate date = ExcelDateConverter.fromExcelSerial(currentSerial);
            int backToSerial = ExcelDateConverter.toExcelSerial(date);

            assertEquals(currentSerial, backToSerial, "Serial " + currentSerial + " should round-trip cleanly");
        }
    }
}
