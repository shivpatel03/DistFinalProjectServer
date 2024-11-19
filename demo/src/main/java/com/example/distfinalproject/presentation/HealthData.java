package com.example.distfinalproject.presentation;

import java.io.Serializable;

public class HealthData implements Serializable {
    private static final long serialVersionUID = 1234567890L;

    private String userId;
    private String username;
    private String deviceId;
    private float heartRate;
    private float steps;
    private long timestamp;

    public HealthData() {
        this.userId = "";
        this.username = "";
        this.deviceId = "";
        this.heartRate = 0f;
        this.steps = 0f;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public float getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(float heartRate) {
        this.heartRate = heartRate;
    }

    public float getSteps() {
        return steps;
    }

    public void setSteps(float steps) {
        this.steps = steps;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "HealthData{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", heartRate=" + heartRate +
                ", steps=" + steps +
                ", timestamp=" + timestamp +
                '}';
    }
}
