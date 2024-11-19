package com.example.distfinalproject.presentation;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthDashboard extends JFrame {
    private static final ConcurrentHashMap<String, HealthData> latestData = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LinkedList<HealthData>> historicalData = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 20;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel connectedClientsLabel;
    private final JPanel graphsPanel;
    private final JScrollPane graphsScrollPane;
    private static final ConcurrentHashMap<String, ChartPanel[]> clientCharts = new ConcurrentHashMap<>();

    public HealthDashboard() {
        setTitle("Health Data Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create split pane for table and graphs
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.3); // Give 30% space to the table

        // Create top panel for table
        JPanel topPanel = new JPanel(new BorderLayout());

        // Create header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add connected clients counter
        connectedClientsLabel = new JLabel("Connected Clients: 0");
        connectedClientsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerPanel.add(connectedClientsLabel, BorderLayout.WEST);

        topPanel.add(headerPanel, BorderLayout.NORTH);

        // Create table
        String[] columns = { "User ID", "Device ID", "Heart Rate", "Steps", "Avg Heart Rate", "Avg Steps",
                "Last Updated" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(150);

        // Add table to scroll pane
        JScrollPane tableScrollPane = new JScrollPane(table);
        topPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Create graphs panel
        graphsPanel = new JPanel();
        graphsPanel.setLayout(new BoxLayout(graphsPanel, BoxLayout.Y_AXIS));
        graphsScrollPane = new JScrollPane(graphsPanel);

        // Add components to split pane
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(graphsScrollPane);

        // Add split pane to frame
        add(splitPane, BorderLayout.CENTER);

        // Set frame properties
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Start periodic updates
        startPeriodicUpdate();
    }

    private void startPeriodicUpdate() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::updateDashboard, 0, 1, TimeUnit.SECONDS);
    }

    private void updateDashboard() {
        SwingUtilities.invokeLater(() -> {
            updateTable();
            updateGraphs();
        });
    }

    private void updateTable() {
        // Clear existing rows
        tableModel.setRowCount(0);

        // Update connected clients count
        connectedClientsLabel.setText("Connected Clients: " + latestData.size());

        // Add data for each client
        for (String userId : latestData.keySet()) {
            HealthData data = latestData.get(userId);
            List<HealthData> history = historicalData.get(userId);

            double avgHeartRate = history.stream()
                    .mapToDouble(HealthData::getHeartRate)
                    .average()
                    .orElse(0.0);

            double avgSteps = history.stream()
                    .mapToDouble(HealthData::getSteps)
                    .average()
                    .orElse(0.0);

            Vector<Object> row = new Vector<>();
            row.add(data.getUserId());
            row.add(data.getDeviceId());
            row.add(String.format("%.1f", data.getHeartRate()));
            row.add(String.format("%.0f", data.getSteps()));
            row.add(String.format("%.1f", avgHeartRate));
            row.add(String.format("%.0f", avgSteps));
            row.add(new Date(data.getTimestamp()).toString());
            tableModel.addRow(row);
        }
    }

    private void updateGraphs() {
        graphsPanel.removeAll();

        for (String userId : historicalData.keySet()) {
            List<HealthData> history = historicalData.get(userId);

            // Create client panel
            JPanel clientPanel = new JPanel(new GridLayout(1, 2, 5, 5));
            clientPanel.setBorder(BorderFactory.createTitledBorder("Client: " + userId));

            // Create or update charts
            ChartPanel[] charts = createOrUpdateCharts(userId, history);
            clientPanel.add(charts[0]); // Heart Rate chart
            clientPanel.add(charts[1]); // Steps chart

            clientPanel.setPreferredSize(new Dimension(1100, 300));
            clientPanel.setMaximumSize(new Dimension(1100, 300));

            graphsPanel.add(clientPanel);
            graphsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        graphsPanel.revalidate();
        graphsPanel.repaint();
    }

    private ChartPanel[] createOrUpdateCharts(String userId, List<HealthData> history) {
        if (!clientCharts.containsKey(userId)) {
            // Create new charts
            TimeSeries heartRateSeries = new TimeSeries("Heart Rate");
            TimeSeries stepsSeries = new TimeSeries("Steps");

            // Create datasets
            TimeSeriesCollection heartRateDataset = new TimeSeriesCollection(heartRateSeries);
            TimeSeriesCollection stepsDataset = new TimeSeriesCollection(stepsSeries);

            // Create charts
            JFreeChart heartRateChart = ChartFactory.createTimeSeriesChart(
                    "Heart Rate Over Time",
                    "Time",
                    "Heart Rate (BPM)",
                    heartRateDataset,
                    true,
                    true,
                    false);

            JFreeChart stepsChart = ChartFactory.createTimeSeriesChart(
                    "Steps Over Time",
                    "Time",
                    "Steps",
                    stepsDataset,
                    true,
                    true,
                    false);

            // Customize the charts
            customizeChart(heartRateChart);
            customizeChart(stepsChart);

            // Create chart panels
            ChartPanel heartRatePanel = new ChartPanel(heartRateChart);
            ChartPanel stepsPanel = new ChartPanel(stepsChart);

            ChartPanel[] charts = new ChartPanel[] { heartRatePanel, stepsPanel };
            clientCharts.put(userId, charts);
        }

        // Update the data
        ChartPanel[] charts = clientCharts.get(userId);
        updateChartData(charts[0], history, HealthData::getHeartRate);
        updateChartData(charts[1], history, HealthData::getSteps);

        return charts;
    }

    private void customizeChart(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
    }

    private void updateChartData(ChartPanel chartPanel, List<HealthData> history, ValueExtractor extractor) {
        TimeSeriesCollection dataset = (TimeSeriesCollection) chartPanel.getChart().getXYPlot().getDataset();
        TimeSeries series = dataset.getSeries(0);
        series.clear();

        for (HealthData data : history) {
            series.add(new Second(new Date(data.getTimestamp())), extractor.extract(data));
        }
    }

    @FunctionalInterface
    interface ValueExtractor {
        float extract(HealthData data);
    }

    public static void updateClientData(HealthData data) {
        // Update latest data
        latestData.put(data.getUserId(), data);

        // Update historical data
        historicalData.computeIfAbsent(data.getUserId(), k -> new LinkedList<>());
        LinkedList<HealthData> history = historicalData.get(data.getUserId());

        // Add new data point
        history.addLast(data);

        // Remove oldest data point if we exceed MAX_HISTORY
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    public static void removeClient(String userId) {
        latestData.remove(userId);
        historicalData.remove(userId);
        clientCharts.remove(userId);
    }
}