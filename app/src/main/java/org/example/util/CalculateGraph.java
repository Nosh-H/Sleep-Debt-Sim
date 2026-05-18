package org.example.util;

import java.util.ArrayList;
import java.util.Date;

import org.example.Constants;
import org.example.ui.SimulatorApp;

public class CalculateGraph {
    
    /**
     * Computes sleep debt for each night from the first one until the final one.
     * Passes in date and sleep debt values to be graphed.
     * @param nights - Preconditon: nights is sorted by date in ascending order
     */
    public static void computeValues(ArrayList<Night> nights) {
        ArrayList<Double> sleepDeficits = new ArrayList<Double>();
        ArrayList<Double> sleepDebts = new ArrayList<Double>(); // Y axis
        ArrayList<Integer> dates = new ArrayList<Integer>();
        // Compute "Day 1" offset
        int offset = 1 + millisecondsToDays(nights.get(0).date().getTime());
        // For each night - On^2
        for(int i = 0; i < nights.size(); i++) {
            sleepDeficits.add(Constants.optimalHours - nights.get(i).hoursSlept());
            // Compute sleep debt from the nights before it
            double sleepDebt = 0;
            for(int j = 0; j <= i; j++) {
                // For now, have the sleep debt from a night's deficit be inversely proportional to how long ago that deficit was
                sleepDebt += sleepDeficits.get(j) / Math.pow((i + 1 - j), Constants.DEFICIT_DECAY_POWER) * Constants.DEFICIT_MULTIPLIER;
            }
            sleepDebts.add(-sleepDebt);
            dates.add(i);
            System.out.println("Date: " + (i));
            System.out.println("Debt: " + sleepDebt);
        }

        // Finally pass in the two ArrayLists into the Simulator to be graphed
        // Temporary method
        new SimulatorApp().launch(dates, sleepDebts);
    }

    public static int millisecondsToDays(long milliseconds) {
        return (int) (milliseconds / (1000.0 / 3600 / 24));
    }

    public static long daysToMilliseconds(int days) {
        return days * 24 * 3600 * 1000;
    }
}
