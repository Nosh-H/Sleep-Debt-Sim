package org.example.ui;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;
import org.example.Constants;
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
import com.google.common.primitives.Doubles;

public class SimulatorApp {

    private final List<Double> xData = new ArrayList<Double>();
    private final List<Double> yData = new ArrayList<Double>();
    private final ArrayList<Night> nights = new ArrayList<Night>();

    // Starts and runs the simulation
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
        XYChart chart = new XYChartBuilder().width(800).height(600).title("Sleep Debt over Nights").xAxisTitle("Days Since Earliest Entry").yAxisTitle("Debt").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        // Base values to prevent crash. Assume no sleep debt on day 0.
        xData.add(0.0);
        yData.add(0.0);
        chart.addSeries(Constants.XY_SERIES_NAME, xData, yData);
        XChartPanel<XYChart> chartPanel = new XChartPanel<>(chart);

        // Add button behaviour: add the selected date to list and update chart
        addBtn.addActionListener(ev -> {
            LocalDate ld = datePicker.getDate();
            if (ld == null) {
                JOptionPane.showMessageDialog(frame, "Please pick a date before adding.", "No date", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Append to chart data (use ResultWrapper utility to hold X and Y's)
            synchronized (nights) {
                // Prepare user input for data handling
                Double parsed = Doubles.tryParse((textField.getText() == null ? "" : textField.getText()));
                if (parsed == null) {
                    JOptionPane.showMessageDialog(frame, "Please only type an integer or decimal before adding.", "Invalid number", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                nights.add(new Night(datePicker.getDate(), parsed));
                // Recalculate sleep debts
                ResultWrapper coordinates = CalculateGraph.computeValues(nights);
                
                // Add to UI list
                listModel.addElement(ld.toString());
                // Update the chart
                chart.updateXYSeries(Constants.XY_SERIES_NAME, new ArrayList<>(coordinates.x()), new ArrayList<>(coordinates.y()), null);
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

    // Former temporary start function
    public void launch(ArrayList<Integer> days, ArrayList<Double> debt) {
        XYChart chart = QuickChart.getChart("Sleep Balance over Multiple Nights. Negative balance = debt", "Day", "Sleep Balance/Debt", Constants.XY_SERIES_NAME, days, debt);
        // chart.getStyler().setLegendVisible(false);

        // Show it
        new SwingWrapper<>(chart).displayChart();
    }
}
