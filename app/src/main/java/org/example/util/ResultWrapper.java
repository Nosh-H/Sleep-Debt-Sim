package org.example.util;

import java.util.ArrayList;

/** Stores the x and y coordinates to be graphed. Holds two ArrayLists in one record. */
public record ResultWrapper(ArrayList<Integer> x, ArrayList<Double> y) {}
