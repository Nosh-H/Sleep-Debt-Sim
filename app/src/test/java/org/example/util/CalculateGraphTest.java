package org.example.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class CalculateGraphTest {

  @Test
  public void computeValues_sortsUnsortedInput_beforeCalculating() {
    // Create nights out of order: 10, 8, 12
    ArrayList<Night> unsorted = new ArrayList<Night>();
    unsorted.add(new Night(10, 8.0));
    unsorted.add(new Night(8, 7.0));
    unsorted.add(new Night(12, 6.0));

    ResultWrapper res = CalculateGraph.computeValues(unsorted);

    // Dates are normalized to start at 0 (first day)
    ArrayList<Integer> x = res.x();
    assertEquals(3, x.size());

    // Expected ordering after sorting by serial: 8,10,12 -> offsets 0,2,4
    assertEquals(0, x.get(0));
    assertEquals(2, x.get(1));
    assertEquals(4, x.get(2));
  }

  @Test
  public void computeValues_appliesDeficitDecayAcrossEarlierNights() {
    ArrayList<Night> nights = new ArrayList<Night>();
    nights.add(new Night(8, 8.0));
    nights.add(new Night(10, 6.0));

    ResultWrapper res = CalculateGraph.computeValues(nights);

    // The second night's deficit is 2.0 hours, so the debt on the second day is exactly -2.0.
    assertEquals(2, res.x().size());
    assertEquals(0, res.x().get(0));
    assertEquals(2, res.x().get(1));
    assertEquals(0.0, res.y().get(0), 1e-9);
    assertEquals(-2.0, res.y().get(1), 1e-9);
  }

  @Test
  public void computeValues_usesEarlierDeficitsWhenComputingLaterDebt() {
    ArrayList<Night> nights = new ArrayList<Night>();
    nights.add(new Night(8, 8.0));
    nights.add(new Night(10, 7.0));
    nights.add(new Night(12, 8.0));

    ResultWrapper res = CalculateGraph.computeValues(nights);

    // Deficits: [0.0, 1.0, 0.0]. The cumulative debt is -1.0 on the second night and then
    // decays as the earlier deficit is spread over later days.
    assertEquals(0.0, res.y().get(0), 1e-9);
    assertEquals(-1.0, res.y().get(1), 1e-9);
    assertEquals(-0.420448207627, res.y().get(2), 1e-9);
  }
}
