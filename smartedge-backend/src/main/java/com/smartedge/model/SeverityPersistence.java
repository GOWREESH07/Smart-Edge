package com.smartedge.model;

public class SeverityPersistence {

    private String deviceId;

    private int consecutiveAnomalies;

    private int consecutiveNormalReadings;

    private String currentSeverity;

    private String previousSeverity;

    private boolean persistent;

    private long durationSeconds;

    public SeverityPersistence() {
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getConsecutiveAnomalies() {
        return consecutiveAnomalies;
    }

    public void setConsecutiveAnomalies(int consecutiveAnomalies) {
        this.consecutiveAnomalies = consecutiveAnomalies;
    }

    public int getConsecutiveNormalReadings() {
        return consecutiveNormalReadings;
    }

    public void setConsecutiveNormalReadings(int consecutiveNormalReadings) {
        this.consecutiveNormalReadings = consecutiveNormalReadings;
    }

    public String getCurrentSeverity() {
        return currentSeverity;
    }

    public void setCurrentSeverity(String currentSeverity) {
        this.currentSeverity = currentSeverity;
    }

    public String getPreviousSeverity() {
        return previousSeverity;
    }

    public void setPreviousSeverity(String previousSeverity) {
        this.previousSeverity = previousSeverity;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}