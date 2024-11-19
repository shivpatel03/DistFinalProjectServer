package com.example.distfinalproject.presentation;

import java.net.Socket;
import java.io.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String currentUserId;
    private volatile boolean running = true;
    private final ConcurrentHashMap<String, ClientHandler> activeClients;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> activeClients) {
        this.clientSocket = socket;
        this.activeClients = activeClients;
    }

    @Override
    public void run() {
        try {
            System.out.println("\n=== Starting Client Handler ===");

            output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.flush();
            System.out.println("Output stream created and flushed");

            input = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("Input stream created");

            System.out.println("Handler initialized for: " + clientSocket.getInetAddress());

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Object received = input.readObject();
                    if (received instanceof HealthData) {
                        HealthData data = (HealthData) received;
                        handleHealthData(data);
                    }
                } catch (EOFException e) {
                    System.out.println("Client disconnected normally: " + currentUserId);
                    break;
                } catch (IOException e) {
                    System.out.println("Connection error for " + currentUserId + ": " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Handler error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void handleHealthData(HealthData data) {
        String userId = data.getUserId();
        if (currentUserId == null) {
            currentUserId = userId;
            activeClients.put(currentUserId, this);
            System.out.println("\n=== New Client Registered ===");
            System.out.println("User ID: " + currentUserId);
            System.out.println("Username: " + data.getUsername());
            System.out.println("Total Active Clients: " + activeClients.size());
        }

        // printHealthData(data);
        HealthDashboard.updateClientData(data);
    }

    // private void printHealthData(HealthData data) {
    // System.out.println("\n=== Health Data Received ===");
    // System.out.println("Time: " + new Date());
    // System.out.println("User ID: " + data.getUserId());
    // System.out.println("Username: " + data.getUsername());
    // System.out.println("Device ID: " + data.getDeviceId());
    // System.out.println("Heart Rate: " + data.getHeartRate());
    // System.out.println("Steps: " + data.getSteps());
    // System.out.println("Active Clients: " + activeClients.size());
    // System.out.println("========================");
    // }

    private void cleanup() {
        running = false;

        System.out.println("\n=== Cleaning Up Client Handler ===");
        System.out.println("Client ID: " + currentUserId);

        if (currentUserId != null) {
            Server.removeClient(currentUserId);
            HealthDashboard.removeClient(currentUserId);
        }

        try {
            if (output != null) {
                output.close();
                System.out.println("Output stream closed");
            }
            if (input != null) {
                input.close();
                System.out.println("Input stream closed");
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Socket closed");
            }
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }

        System.out.println("Cleanup completed for: " + currentUserId);
        System.out.println("Remaining active clients: " + activeClients.size());
    }
}
