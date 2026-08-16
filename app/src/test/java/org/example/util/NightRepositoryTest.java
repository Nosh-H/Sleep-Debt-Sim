package org.example.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class NightRepositoryTest {

  @Test
  public void upsert_replacesMatchingSerial_andKeepsDatesSorted() {
    NightRepository repository = new NightRepository();

    repository.upsert(new Night(10, 8.0));
    repository.upsert(new Night(8, 7.0));
    repository.upsert(new Night(12, 6.0));
    repository.upsert(new Night(10, 9.5));

    ArrayList<Integer> serials = new ArrayList<Integer>();
    for (Night night : repository.snapshot()) {
      serials.add(night.excelDateSerial());
    }

    assertEquals(List.of(8, 10, 12), serials);
    assertEquals(9.5, repository.snapshot().get(1).hoursSlept());
  }

  @Test
  public void upsertAll_preservesSortOrderForImportedNights() {
    NightRepository repository = new NightRepository();

    repository.upsertAll(List.of(new Night(25, 7.5), new Night(10, 5.0), new Night(25, 8.0)));

    ArrayList<Integer> serials = new ArrayList<Integer>();
    for (Night night : repository.snapshot()) {
      serials.add(night.excelDateSerial());
    }

    assertEquals(List.of(10, 25), serials);
    assertEquals(8.0, repository.snapshot().get(1).hoursSlept());
  }
}
