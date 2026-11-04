package com.smartedge.model;

public class DeviationResult {

    private String deviceId;

    // Z-score deviations
    private double voltageDeviation;
    private double currentDeviation;
    private double powerDeviation;
    private double frequencyDeviation;
    private double powerFactorDeviation;

    // Adaptive weights
    private double voltageWeight;
    private double currentWeight;
    private double powerWeight;
    private double frequencyWeight;
    private double powerFactorWeight;

    // Final fused score
    private double overallDeviationScore;

    private String deviationLevel;

    private String mostDeviantParameter;
    private int consecutiveAnomalies;

private int consecutiveNormalReadings;

private boolean persistent;

private long anomalyDurationSeconds;

private String effectiveSeverity;

    public DeviationResult() {
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public double getVoltageDeviation() {
        return voltageDeviation;
    }

    public void setVoltageDeviation(double voltageDeviation) {
        this.voltageDeviation = voltageDeviation;
    }

    public double getCurrentDeviation() {
        return currentDeviation;
    }

    public void setCurrentDeviation(double currentDeviation) {
        this.currentDeviation = currentDeviation;
    }

    public double getPowerDeviation() {
        return powerDeviation;
    }

    public void setPowerDeviation(double powerDeviation) {
        this.powerDeviation = powerDeviation;
    }

    public double getFrequencyDeviation() {
        return frequencyDeviation;
    }

    public void setFrequencyDeviation(double frequencyDeviation) {
        this.frequencyDeviation = frequencyDeviation;
    }

    public double getPowerFactorDeviation() {
        return powerFactorDeviation;
    }

    public void setPowerFactorDeviation(double powerFactorDeviation) {
        this.powerFactorDeviation = powerFactorDeviation;
    }

    public double getVoltageWeight() {
        return voltageWeight;
    }

    public void setVoltageWeight(double voltageWeight) {
        this.voltageWeight = voltageWeight;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(double currentWeight) {
        this.currentWeight = currentWeight;
    }

    public double getPowerWeight() {
        return powerWeight;
    }

    public void setPowerWeight(double powerWeight) {
        this.powerWeight = powerWeight;
    }

    public double getFrequencyWeight() {
        return frequencyWeight;
    }

    public void setFrequencyWeight(double frequencyWeight) {
        this.frequencyWeight = frequencyWeight;
    }

    public double getPowerFactorWeight() {
        return powerFactorWeight;
    }

    public void setPowerFactorWeight(double powerFactorWeight) {
        this.powerFactorWeight = powerFactorWeight;
    }

    public double getOverallDeviationScore() {
        return overallDeviationScore;
    }

    public void setOverallDeviationScore(double overallDeviationScore) {
        this.overallDeviationScore = overallDeviationScore;
    }

    public String getDeviationLevel() {
        return deviationLevel;
    }

    public void setDeviationLevel(String deviationLevel) {
        this.deviationLevel = deviationLevel;
    }

    public String getMostDeviantParameter() {
        return mostDeviantParameter;
    }

    public void setMostDeviantParameter(String mostDeviantParameter) {
        this.mostDeviantParameter = mostDeviantParameter;
    }
    public int getConsecutiveAnomalies() {
    return consecutiveAnomalies;
}

public void setConsecutiveAnomalies(
        int consecutiveAnomalies) {

    this.consecutiveAnomalies =
            consecutiveAnomalies;
}


public int getConsecutiveNormalReadings() {
    return consecutiveNormalReadings;
}

public void setConsecutiveNormalReadings(
        int consecutiveNormalReadings) {

    this.consecutiveNormalReadings =
            consecutiveNormalReadings;
}


public boolean isPersistent() {
    return persistent;
}

public void setPersistent(
        boolean persistent) {

    this.persistent = persistent;
}


public long getAnomalyDurationSeconds() {
    return anomalyDurationSeconds;
}

public void setAnomalyDurationSeconds(
        long anomalyDurationSeconds) {

    this.anomalyDurationSeconds =
            anomalyDurationSeconds;
}


public String getEffectiveSeverity() {
    return effectiveSeverity;
}

public void setEffectiveSeverity(
        String effectiveSeverity) {

    this.effectiveSeverity =
            effectiveSeverity;
}
}