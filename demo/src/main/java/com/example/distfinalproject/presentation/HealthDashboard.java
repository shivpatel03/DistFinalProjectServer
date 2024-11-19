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
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel connectedClientsLabel;
    private final JPanel graphsPanel;

    // Store chart panels for each client
    private final ConcurrentHashMap<String, JPanel> clientPanels = new ConcurrentHashMap<>();

    public HealthDashboard() {
        setTitle("Health Data Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize table
        String[] columns = {"User ID", "Device ID", "Heart Rate", "Steps", "Avg Heart Rate", "Avg Steps", "Last Updated"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        // Create header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        connectedClientsLabel = new JLabel("Connected Clients: 0");
        connectedClientsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerPanel.add(connectedClientsLabel, BorderLayout.WEST);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create top panel with table
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Create graphs panel
        graphsPanel = new JPanel();
        graphsPanel.setLayout(new BoxLayout(graphsPanel, BoxLayout.Y_AXIS));
        JScrollPane graphsScrollPane = new JScrollPane(graphsPanel);

        // Add components to split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(graphsScrollPane);
        splitPane.setResizeWeight(0.3);
        add(splitPane);

        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Start updates
        startPeriodicUpdate();
    }

    private void startPeriodicUpdate() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                SwingUtilities.invokeLater(this::updateDashboard);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void updateDashboard() {
        updateTable();
        updateGraphs();
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        connectedClientsLabel.setText("Connected Clients: " + latestData.size());

        for (Map.Entry<String, HealthData> entry : latestData.entrySet()) {
            String userId = entry.getKey();
            HealthData data = entry.getValue();
            List<HealthData> history = historicalData.get(userId);

            if (history != null && !history.isEmpty()) {
                double avgHeartRate = history.stream()
                    .mapToDouble(HealthData::getHeartRate)
                    .average()
                    .orElse(0.0);

                double avgSteps = history.stream()
                    .mapToDouble(HealthData::getSteps)
                    .average()
                    .orElse(0.0);

                Object[] row = {
                    userId,
                    data.getDeviceId(),
                    String.format("%.1f", data.getHeartRate()),
                    String.format("%.0f", data.getSteps()),
                    String.format("%.1f", avgHeartRate),
                    String.format("%.0f", avgSteps),
                    dateFormat.format(new Date(data.getTimestamp()))
                };
                tableModel.addRow(row);
            }
        }
    }

    private void updateGraphs() {
        Set<String> currentUsers = new HashSet<>(historicalData.keySet());
        
        // Remove panels for disconnected clients
        Set<String> panelUsers = new HashSet<>(clientPanels.keySet());
        panelUsers.stream()
            .filter(userId -> !currentUsers.contains(userId))
            .forEach(userId -> {
                graphsPanel.remove(clientPanels.get(userId));
                clientPanels.remove(userId);
            });

        // Update or create panels for connected clients
        for (String userId : currentUsers) {
            List<HealthData> history = historicalData.get(userId);
            if (history == null || history.isEmpty()) continue;

            JPanel clientPanel = clientPanels.computeIfAbsent(userId, this::createClientPanel);
            if (clientPanel.getParent() == null) {
                graphsPanel.add(clientPanel);
                graphsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
            
            updateClientPanel(userId, clientPanel, history);
        }

        graphsPanel.revalidate();
        graphsPanel.repaint();
    }

    private JPanel createClientPanel(String userId) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Client: " + userId));
        panel.setPreferredSize(new Dimension(1100, 300));
        panel.setMaximumSize(new Dimension(1100, 300));

        // Create charts
        TimeSeriesCollection heartRateDataset = new TimeSeriesCollection(new TimeSeries("Heart Rate"));
        TimeSeriesCollection stepsDataset = new TimeSeriesCollection(new TimeSeries("Steps"));

        JFreeChart heartRateChart = createChart("Heart Rate Over Time", "Heart Rate (BPM)", heartRateDataset);
        JFreeChart stepsChart = createChart("Steps Over Time", "Steps", stepsDataset);

        panel.add(new ChartPanel(heartRateChart));
        panel.add(new ChartPanel(stepsChart));

        return panel;
    }

    private JFreeChart createChart(String title, String yAxisLabel, TimeSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            title,
            "Time",
            yAxisLabel,
            dataset,
            true,
            true,
            false
        );

        XYPlot plot = chart.getXYPlot();
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(dateFormat);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        return chart;
    }

    private void updateClientPanel(String userId, JPanel panel, List<HealthData> history) {
        // Update heart rate chart
        ChartPanel heartRatePanel = (ChartPanel) panel.getComponent(0);
        TimeSeriesCollection heartRateDataset = (TimeSeriesCollection) heartRatePanel.getChart().getXYPlot().getDataset();
        TimeSeries heartRateSeries = heartRateDataset.getSeries(0);
        heartRateSeries.clear();

        // Update steps chart
        ChartPanel stepsPanel = (ChartPanel) panel.getComponent(1);
        TimeSeriesCollection stepsDataset = (TimeSeriesCollection) stepsPanel.getChart().getXYPlot().getDataset();
        TimeSeries stepsSeries = stepsDataset.getSeries(0);
        stepsSeries.clear();

        // Add new data points
        for (HealthData data : history) {
            Second second = new Second(new Date(data.getTimestamp()));
            heartRateSeries.add(second, data.getHeartRate());
            stepsSeries.add(second, data.getSteps());
        }
    }

    public static void updateClientData(HealthData data) {
        String userId = data.getUserId();
        latestData.put(userId, data);
        
        LinkedList<HealthData> history = historicalData.computeIfAbsent(userId, k -> new LinkedList<>());
        history.addLast(data);
        
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }

    public static void removeClient(String userId) {
        latestData.remove(userId);
        historicalData.remove(userId);
    }
}