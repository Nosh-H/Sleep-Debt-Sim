package org.example;

public class Constants {
    
    // TODO: Add lombok setters to allow user to configure values
    public static double optimalHours = 8.0;

    // Positive for energetic users, negative for tired users. Shifts graph up or down
    public static double naturalFatigueVerticalTranslation = 0.0;

    public static final double DEFICIT_DECAY_POWER = 1.25;
    public static final double DEFICIT_MULTIPLIER = 1;
    public static final String XY_SERIES_NAME = "Sleep Balance/Debt";
}
