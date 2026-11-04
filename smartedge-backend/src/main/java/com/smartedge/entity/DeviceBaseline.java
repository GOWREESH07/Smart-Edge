package com.smartedge.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "device_baselines",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "device_id")
    }
)
public class DeviceBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true)
    private String deviceId;

    // Voltage statistics
    private Double voltageMean;
    private Double voltageM2;

    // Current statistics
    private Double currentMean;
    private Double currentM2;

    // Power statistics
    private Double powerMean;
    private Double powerM2;

    // Frequency statistics
    private Double frequencyMean;
    private Double frequencyM2;

    // Power factor statistics
    private Double powerFactorMean;
    private Double powerFactorM2;

    private Long sampleCount;

    private java.time.LocalDateTime lastUpdated;

    public DeviceBaseline() {
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Double getVoltageMean() {
        return voltageMean;
    }

    public void setVoltageMean(Double voltageMean) {
        this.voltageMean = voltageMean;
    }

    public Double getVoltageM2() {
        return voltageM2;
    }

    public void setVoltageM2(Double voltageM2) {
        this.voltageM2 = voltageM2;
    }

    public Double getCurrentMean() {
        return currentMean;
    }

    public void setCurrentMean(Double currentMean) {
        this.currentMean = currentMean;
    }

    public Double getCurrentM2() {
        return currentM2;
    }

    public void setCurrentM2(Double currentM2) {
        this.currentM2 = currentM2;
    }

    public Double getPowerMean() {
        return powerMean;
    }

    public void setPowerMean(Double powerMean) {
        this.powerMean = powerMean;
    }

    public Double getPowerM2() {
        return powerM2;
    }

    public void setPowerM2(Double powerM2) {
        this.powerM2 = powerM2;
    }

    public Double getFrequencyMean() {
        return frequencyMean;
    }

    public void setFrequencyMean(Double frequencyMean) {
        this.frequencyMean = frequencyMean;
    }

    public Double getFrequencyM2() {
        return frequencyM2;
    }

    public void setFrequencyM2(Double frequencyM2) {
        this.frequencyM2 = frequencyM2;
    }

    public Double getPowerFactorMean() {
        return powerFactorMean;
    }

    public void setPowerFactorMean(Double powerFactorMean) {
        this.powerFactorMean = powerFactorMean;
    }

    public Double getPowerFactorM2() {
        return powerFactorM2;
    }

    public void setPowerFactorM2(Double powerFactorM2) {
        this.powerFactorM2 = powerFactorM2;
    }

    public Long getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Long sampleCount) {
        this.sampleCount = sampleCount;
    }

    public java.time.LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(java.time.LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}