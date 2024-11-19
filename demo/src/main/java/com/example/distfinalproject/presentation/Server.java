package com.example.distfinalproject.presentation;

import javax.swing.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int PORT = 9999;
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService statusExecutor = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        // Create and show the dashboard
        SwingUtilities.invokeLater(() -> {
            HealthDashboard dashboard = new HealthDashboard();
            dashboard.setVisible(true);
        });

        // Start status updater
        statusExecutor.scheduleAtFixedRate(() -> {
            System.out.println("Current Connected Clients: " + activeClients.size());
            if (!activeClients.isEmpty()) {
                System.out.println("Connected Client IDs: " + String.join(", ", activeClients.keySet()));
            }
            System.out.println("------------------------");
        }, 0, 5, TimeUnit.SECONDS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\nNew client connected from: " + clientSocket.getInetAddress());
                System.out.println("Total clients connected: " + (activeClients.size() + 1));

                ClientHandler handler = new ClientHandler(clientSocket, activeClients);
                executorService.execute(handler);
            }
        } catch (Exception e) {
            System.out.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private static void cleanup() {
        System.out.println("Shutting down server...");
        executorService.shutdown();
        statusExecutor.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            statusExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Error during shutdown: " + e.getMessage());
        }
        System.out.println("Server shutdown complete");
    }

    public static void removeClient(String userId) {
        if (userId != null) {
            ClientHandler handler = activeClients.remove(userId);
            if (handler != null) {
                System.out.println("\nClient disconnected: " + userId);
                System.out.println("Current connected clients: " + activeClients.size());
                if (!activeClients.isEmpty()) {
                    System.out.println("Remaining clients: " + String.join(", ", activeClients.keySet()));
                }
            }
        }
    }

    public static int getActiveClientCount() {
        return activeClients.size();
    }
}
