package org.example.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class CsvNightLoaderMalformedTest {

  @Test
  public void load_missingColumns_throwsIOException() {
    String csv = "2024-01-10\n"; // missing hours column
    assertThrows(java.io.IOException.class, () -> CsvNightLoader.load(new StringReader(csv)));
  }

  @Test
  public void load_unparsableHours_throwsIOException() {
    String csv = "2024-01-10,not-a-number\n";
    assertThrows(java.io.IOException.class, () -> CsvNightLoader.load(new StringReader(csv)));
  }

  @Test
  public void load_unparsableDate_throwsIOException() {
    // Use a token that does not accidentally match the header heuristic (avoid the substring
    // "date").
    String csv = "garbage,8.0\n";
    assertThrows(java.io.IOException.class, () -> CsvNightLoader.load(new StringReader(csv)));
  }
}
