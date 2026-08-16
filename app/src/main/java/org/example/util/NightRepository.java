package org.example.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Stores nights in ascending Excel-serial order and ensures a date is never duplicated.
 *
 * <p>The UI previously embedded the same "look up by serial, replace if it already exists, else
 * insert before the first larger serial" loop in multiple handlers. Centralizing it here keeps the
 * data rules in one place and makes the behavior easy to test without Swing.
 */
public class NightRepository {

  private final ArrayList<Night> nights = new ArrayList<Night>();

  public ArrayList<Night> snapshot() {
    synchronized (nights) {
      return new ArrayList<Night>(nights);
    }
  }

  public void upsert(Night night) {
    synchronized (nights) {
      upsertLocked(night);
    }
  }

  public void upsertAll(Collection<Night> importedNights) {
    synchronized (nights) {
      // CSV imports are already sorted and deduplicated by CsvNightLoader, but the repository still
      // validates that invariant and preserves it for any caller that may pass an arbitrary list.
      for (Night incoming : importedNights) {
        upsertLocked(incoming);
      }
    }
  }

  private void upsertLocked(Night night) {
    // Keep the list sorted by Excel serial so the chart, list display, and CSV export always share
    // the same chronological ordering. A single scan is enough to either replace an existing date
    // or
    // insert before the first later date without duplicating entries.
    for (int i = 0; i < nights.size(); i++) {
      int existingSerial = nights.get(i).excelDateSerial();
      if (existingSerial == night.excelDateSerial()) {
        nights.set(i, night);
        return;
      }
      if (existingSerial > night.excelDateSerial()) {
        nights.add(i, night);
        return;
      }
    }

    nights.add(night);
  }
}
