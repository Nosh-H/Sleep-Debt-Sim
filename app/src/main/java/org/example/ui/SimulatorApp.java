package org.example.ui;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;
import org.example.sim.Simulation;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SimulatorApp {

    private Simulation sim;
    private final List<Double> xData = new ArrayList<>();
    private final List<Double> yData = new ArrayList<>();
    private SwingWrapper<XYChart> swingWrapper;

    public static void launchSwing(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SimulatorApp().start();
        });
    }

    // Will become start
    public void start() {
        // XYChart chart = new XYChartBuilder().width(800).height(600).title("Simulation - Live Chart").xAxisTitle("Step").yAxisTitle("Value").build();
        xData.add(6.0);
        xData.add(7.0);
        xData.add(8.0);
        yData.add(2.0);
        yData.add(1.0);
        yData.add(0.0);
        // double[] xData = new double[] { 0.0, 1.0, 2.0 };
        // double[] yData = new double[] { 2.0, 1.0, 0.0 };
        XYChart chart = QuickChart.getChart("Sleep Debt over Multiple Nights", "X", "Y", "Sleep Debt", xData, yData);
        // chart.getStyler().setLegendVisible(false);

        // Show it
        new SwingWrapper<>(chart).displayChart();
        // swingWrapper
        // setupSimulation(chart);
    }

    // Temporary start function
    public void launch(ArrayList<Integer> days, ArrayList<Double> debt) {
        XYChart chart = QuickChart.getChart("Sleep Balance over Multiple Nights", "Day", "Balance/Debt", "Sleep Balance", days, debt);
        // chart.getStyler().setLegendVisible(false);

        // Show it
        new SwingWrapper<>(chart).displayChart();
    }

    private void setupSimulation(XYChart chart) {
        sim = new Simulation();
        sim.addListener((step, value) -> {
            synchronized (xData) {
                xData.add((double) step);
                yData.add(value);
                if (xData.size() > 200) {
                    xData.remove(0);
                    yData.remove(0);
                }
            }
            // Update chart on EDT
            SwingUtilities.invokeLater(() -> {
                synchronized (xData) {
                    chart.updateXYSeries("data", new ArrayList<>(xData), new ArrayList<>(yData), null);
                    swingWrapper.repaintChart();
                }
            });
        });
        sim.start(100);
    }
}
