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
}
