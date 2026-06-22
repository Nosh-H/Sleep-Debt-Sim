package org.example.ui;

import org.example.Constants;
import org.example.util.CalculateGraph;
import org.example.util.CsvNightExporter;
import org.example.util.CsvNightLoader;
import org.example.util.ExcelDateConverter;
import org.example.util.Night;
import org.example.util.ResultWrapper;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.google.common.primitives.Doubles;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;

public class SimulatorApp {

    private final ArrayList<Night> nights = new ArrayList<Night>();

    // Starts and runs the simulation
    public void start() {
        JFrame frame = new JFrame("Simulator - Date Picker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel controls = new JPanel();
        controls.setLayout(new javax.swing.BoxLayout(controls, javax.swing.BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        DatePickerSettings dpSettings = new DatePickerSettings();
        DatePicker datePicker = new DatePicker(dpSettings);
        datePicker.setMaximumSize(new Dimension(200, 30));

        JTextField textField = new JTextField(20);
        textField.setText("8.0");

        JButton addBtn = new JButton("Add Date");
        JButton uploadBtn = new JButton("Upload CSV");
        JButton downloadBtn = new JButton("Download CSV");

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> dateList = new JList<>(listModel);
        dateList.setVisibleRowCount(8);
        JScrollPane listScroll = new JScrollPane(dateList);
        listScroll.setPreferredSize(new Dimension(220, 160));

        controls.add(new JLabel("Choose date:"));
        controls.add(javax.swing.Box.createVerticalStrut(6));
        controls.add(datePicker);
        controls.add(textField);
        controls.add(javax.swing.Box.createVerticalStrut(8));
        controls.add(addBtn);
        controls.add(javax.swing.Box.createVerticalStrut(8));
        controls.add(uploadBtn);
        controls.add(javax.swing.Box.createVerticalStrut(8));
        controls.add(downloadBtn);
        controls.add(javax.swing.Box.createVerticalStrut(8));
        controls.add(new JLabel("CSV format: date + hours"));
        controls.add(new JLabel("Date may be Excel serial or yyyy-MM-dd."));
        controls.add(javax.swing.Box.createVerticalStrut(12));
        controls.add(new JLabel("Added dates:"));
        controls.add(listScroll);

        XYChart chart = new XYChartBuilder()
            .width(800)
            .height(600)
            .title("Sleep Debt over Nights")
            .xAxisTitle("Days Since Earliest Entry")
            .yAxisTitle("Debt")
            .build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);

        ArrayList<Integer> initialX = new ArrayList<Integer>();
        ArrayList<Double> initialY = new ArrayList<Double>();
        initialX.add(0);
        initialY.add(0.0);
        chart.addSeries(Constants.XY_SERIES_NAME, initialX, initialY);
        XChartPanel<XYChart> chartPanel = new XChartPanel<>(chart);

        addBtn.addActionListener(ev -> {
            LocalDate selectedDate = datePicker.getDate();
            if (selectedDate == null) {
                JOptionPane.showMessageDialog(frame, "Please pick a date before adding.", "No date", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Double parsedHours = Doubles.tryParse(textField.getText() == null ? "" : textField.getText().trim());
            if (parsedHours == null) {
                JOptionPane.showMessageDialog(frame, "Please only type an integer or decimal before adding.", "Invalid number", JOptionPane.WARNING_MESSAGE);
                return;
            }

            addNightAndRefresh(new Night(ExcelDateConverter.toExcelSerial(selectedDate), parsedHours), chart, listModel, chartPanel);
        });

        uploadBtn.addActionListener(ev -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose a sleep data CSV");
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));

            if (fileChooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path csvPath = fileChooser.getSelectedFile().toPath();
            try {
                ArrayList<Night> importedNights = CsvNightLoader.load(csvPath);
                synchronized (nights) {
                    nights.addAll(importedNights);
                }
                refreshChartAndList(chart, listModel, chartPanel);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Could not load CSV:\n" + ex.getMessage(), "CSV error", JOptionPane.ERROR_MESSAGE);
            }
        });

        downloadBtn.addActionListener(ev -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save sleep data CSV");
            fileChooser.setSelectedFile(new File("sleep-data-export.csv"));
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));

            if (fileChooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path csvPath = fileChooser.getSelectedFile().toPath();
            String fileName = csvPath.getFileName().toString();
            if (!fileName.toLowerCase().endsWith(".csv")) {
                Path parent = csvPath.getParent();
                csvPath = parent == null ? Path.of(fileName + ".csv") : parent.resolve(fileName + ".csv");
            }

            ArrayList<Night> snapshot;
            synchronized (nights) {
                snapshot = new ArrayList<Night>(nights);
            }

            try {
                CsvNightExporter.write(csvPath, snapshot);
                JOptionPane.showMessageDialog(frame, "Saved CSV to:\n" + csvPath, "CSV saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Could not save CSV:\n" + ex.getMessage(), "CSV error", JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.add(controls, BorderLayout.WEST);
        frame.add(chartPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void addNightAndRefresh(Night night, XYChart chart, DefaultListModel<String> listModel, XChartPanel<XYChart> chartPanel) {
        synchronized (nights) {
            nights.add(night);
        }
        refreshChartAndList(chart, listModel, chartPanel);
    }

    private void refreshChartAndList(XYChart chart, DefaultListModel<String> listModel, XChartPanel<XYChart> chartPanel) {
        ArrayList<Night> orderedNights;
        synchronized (nights) {
            orderedNights = new ArrayList<Night>(nights);
        }

        orderedNights.sort(Comparator.comparingInt(Night::excelDateSerial));

        listModel.clear();
        for (Night night : orderedNights) {
            listModel.addElement(formatNightForDisplay(night));
        }

        ResultWrapper coordinates = CalculateGraph.computeValues(orderedNights);
        chart.updateXYSeries(Constants.XY_SERIES_NAME, new ArrayList<Integer>(coordinates.x()), new ArrayList<Double>(coordinates.y()), null);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private String formatNightForDisplay(Night night) {
        return ExcelDateConverter.toDisplayString(night.excelDateSerial()) + " - " + night.hoursSlept() + " hours";
    }

    // Former temporary start function
    public void launch(ArrayList<Integer> days, ArrayList<Double> debt) {
        XYChart chart = QuickChart.getChart("Sleep Balance over Multiple Nights. Negative balance = debt", "Day", "Sleep Balance/Debt", Constants.XY_SERIES_NAME, days, debt);
        new SwingWrapper<>(chart).displayChart();
    }
}
