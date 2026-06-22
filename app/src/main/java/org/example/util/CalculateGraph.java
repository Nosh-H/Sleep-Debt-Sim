package org.example.util;

import java.util.ArrayList;
import java.util.Comparator;

import org.example.Constants;

public class CalculateGraph {
    
    /**
     * Computes sleep debt for each night from the first one until the final one.
     * Passes in date and sleep debt values to be graphed.
     * @param nights - Preconditon: nights is sorted by date in ascending order
     */
    public static ResultWrapper computeValues(ArrayList<Night> nights) {
        ArrayList<Double> sleepDebts = new ArrayList<Double>(); // Y axis
        ArrayList<Integer> dates = new ArrayList<Integer>();

        if (nights == null || nights.isEmpty()) {
            dates.add(0);
            sleepDebts.add(0.0);
            return new ResultWrapper(dates, sleepDebts);
        }

        ArrayList<Night> orderedNights = new ArrayList<Night>(nights);
        orderedNights.sort(Comparator.comparingInt(Night::excelDateSerial));
        ArrayList<Double> sleepDeficits = computeSleepDeficits(orderedNights);

        // Compute "Day 1" offset
        int day1 = orderedNights.get(0).excelDateSerial();

        // For each night - On^2
        for (int i = 0; i < orderedNights.size(); i++) {
            // Compute sleep debt from the nights before it
            double sleepDebt = 0;
            for (int j = 0; j <= i; j++) {
                // For now, have the sleep debt from a night's deficit be inversely proportional to how long ago that deficit was
                sleepDebt += sleepDeficits.get(j) / Math.pow((i + 1 - j), Constants.DEFICIT_DECAY_POWER) * Constants.DEFICIT_MULTIPLIER;
            }
            sleepDebts.add(-sleepDebt);
            dates.add(orderedNights.get(i).excelDateSerial() - day1);
        }
        return new ResultWrapper(dates, sleepDebts);
        // Finally pass in the two ArrayLists into the Simulator to be graphed
        // Temporary method
        // new SimulatorApp().launch(dates, sleepDebts);
    }

    static ArrayList<Double> computeSleepDeficits(ArrayList<Night> orderedNights) {
        ArrayList<Double> sleepDeficits = new ArrayList<Double>();
        if (orderedNights == null) {
            return sleepDeficits;
        }

        for (Night night : orderedNights) {
            sleepDeficits.add(Constants.optimalHours - night.hoursSlept());
        }
        return sleepDeficits;
    }

    public static int millisecondsToDays(long milliseconds) {
        return (int) (milliseconds / (1000.0 / 3600 / 24));
    }

    public static long daysToMilliseconds(int days) {
        return days * 24 * 3600 * 1000;
    }
}
