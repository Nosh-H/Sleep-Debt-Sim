package org.example.ui;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;
import org.example.sim.Simulation;
import org.example.util.CalculateGraph;
import org.example.util.Night;
import org.example.util.ResultWrapper;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

public class SimulatorApp {

    private Simulation sim;
    private final List<Double> xData = new ArrayList<Double>();
    private final List<Double> yData = new ArrayList<Double>();
    private final List<LocalDate> date = new ArrayList<LocalDate>();
    private final List<Double> hoursSlept = new ArrayList<Double>();
    private final ArrayList<Night> nights = new ArrayList<Night>();

    private SwingWrapper<XYChart> swingWrapper;

    public static void launchSwing() {

    }

    // Will become start
    public void start() {
        // Build main frame
        JFrame frame = new JFrame("Simulator - Date Picker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        // Left control panel: DatePicker, Add button, and list of added dates
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Date Picker calendar
        DatePickerSettings dpSettings = new DatePickerSettings();
        DatePicker datePicker = new DatePicker(dpSettings);
        datePicker.setMaximumSize(new Dimension(200, 30));

        // Text input for hours sleep
        JTextField textField = new JTextField(20); // (where 20 is the column width).
        textField.setText("8.0"); // Sets default text to 8.0

        // Add Date button
        JButton addBtn = new JButton("Add Date");
        // Date list
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> dateList = new JList<>(listModel);
        dateList.setVisibleRowCount(8);
        JScrollPane listScroll = new JScrollPane(dateList);
        listScroll.setPreferredSize(new Dimension(220, 160));

        // Add User Input to left side of GUI
        controls.add(new JLabel("Choose date:"));
        controls.add(Box.createVerticalStrut(6));
        controls.add(datePicker);
        controls.add(textField);
        controls.add(Box.createVerticalStrut(8));
        controls.add(addBtn);
        controls.add(Box.createVerticalStrut(12));
        controls.add(new JLabel("Added dates:"));
        controls.add(listScroll);

        // Chart: embedded XChart panel
        XYChart chart = new XYChartBuilder().width(800).height(600).title("Sleep Debt over Nights").xAxisTitle("Index").yAxisTitle("Debt").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        // Base values to prevent crash. Assume no sleep debt on day 0.
        xData.add(0.0);
        yData.add(0.0);
        chart.addSeries("Sleep Debt", xData, yData);
        XChartPanel<XYChart> chartPanel = new XChartPanel<>(chart);

        // Add button behaviour: add the selected date to list and update chart
        addBtn.addActionListener(ev -> {
            LocalDate ld = datePicker.getDate();
            if (ld == null) {
                JOptionPane.showMessageDialog(frame, "Please pick a date before adding.", "No date", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Add to UI list
            listModel.addElement(ld.toString());

            // Append to chart data (use ResultWrapper utility to hold X and Y's)
            synchronized (nights) {
                // Prepare user input for data handling
                nights.add(new Night(datePicker.getDate(), Double.parseDouble(textField.getText())));
                // Recalculate sleep debts
                ResultWrapper coordinates = CalculateGraph.computeValues(nights);
                // Update the chart
                chart.updateXYSeries("Sleep Debt", new ArrayList<>(coordinates.x()), new ArrayList<>(coordinates.y()), null);
            }
            chartPanel.revalidate();
            chartPanel.repaint();
        });

        frame.add(controls, BorderLayout.WEST);
        frame.add(chartPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Temporary start function
    public void launch(ArrayList<Integer> days, ArrayList<Double> debt) {
        XYChart chart = QuickChart.getChart("Sleep Balance over Multiple Nights", "Day", "Balance/Debt", "Sleep Balance", days, debt);
        // chart.getStyler().setLegendVisible(false);

        // Show it
        new SwingWrapper<>(chart).displayChart();
    }

    // OLD - currently not called
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
