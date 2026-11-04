package com.smartedge.service;

import com.smartedge.dto.IntelligenceResponse;
import com.smartedge.entity.EnergyReading;
import com.smartedge.repository.EnergyReadingRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class IntelligenceService {

    private final EnergyReadingRepository repository;
    private final AnomalyEventService anomalyEventService;

    // Simulation OFF by default
    private String simulationMode = "OFF";

    // Prevent duplicate anomaly records while
    // the same anomaly remains active
    private final Set<String> activeAnomalies = new HashSet<>();


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public IntelligenceService(
            EnergyReadingRepository repository,
            AnomalyEventService anomalyEventService) {

        this.repository = repository;
        this.anomalyEventService = anomalyEventService;
    }


    // =====================================================
    // SET SIMULATION MODE
    // =====================================================

    public void setSimulationMode(String mode) {

        if (mode == null) {
            simulationMode = "OFF";
            return;
        }

        simulationMode = mode.toUpperCase();
    }


    // =====================================================
    // GET SIMULATION MODE
    // =====================================================

    public String getSimulationMode() {
        return simulationMode;
    }


    // =====================================================
    // ANALYZE LATEST READING
    // =====================================================

    public IntelligenceResponse analyzeLatestReading() {

        EnergyReading reading =
                repository.findAll()
                        .stream()
                        .max(
                                Comparator.comparing(
                                        EnergyReading::getId
                                )
                        )
                        .orElse(null);


        // =====================================================
        // NO DATA
        // =====================================================

        if (reading == null) {

            return new IntelligenceResponse(
                    "NO DATA",

                    "UNKNOWN",
                    "UNKNOWN",
                    "UNKNOWN",
                    "UNKNOWN",
                    "UNKNOWN",
                    "UNKNOWN",

                    0,

                    List.of(
                            "No sensor readings available"
                    ),

                    List.of(
                            "Check ESP32 and backend connection"
                    )
            );
        }


        // =====================================================
        // REAL SENSOR VALUES
        // =====================================================

        Double voltage = reading.getVoltage();
        Double current = reading.getCurrent();
        Double power = reading.getPower();
        Double frequency = reading.getFrequency();
        Double powerFactor = reading.getPowerFactor();
        Integer wifi = reading.getWifiStrength();

        String deviceId = reading.getDeviceId();

        if (deviceId == null) {
            deviceId = "UNKNOWN";
        }


        // =====================================================
        // SAFE SOFTWARE SIMULATION
        // =====================================================

        switch (simulationMode) {

            case "HIGH_VOLTAGE":
                voltage = 260.0;
                break;

            case "LOW_VOLTAGE":
                voltage = 185.0;
                break;

            case "HIGH_POWER":
                power = 3500.0;
                break;

            case "BAD_FREQUENCY":
                frequency = 47.5;
                break;

            case "LOW_POWER_FACTOR":
                powerFactor = 0.65;
                break;

            case "WEAK_WIFI":
                wifi = -90;
                break;

            case "MULTIPLE":
                voltage = 260.0;
                power = 3500.0;
                frequency = 47.5;
                powerFactor = 0.65;
                wifi = -90;
                break;

            case "AC_OFF":
                voltage = 0.0;
                current = 0.0;
                power = 0.0;
                frequency = 0.0;
                powerFactor = 0.0;
                break;

            case "OFF":
            default:
                // Use actual sensor values
                break;
        }


        // =====================================================
        // RESPONSE COLLECTIONS
        // =====================================================

        List<String> anomalies = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        Set<String> detectedNow = new HashSet<>();


        // =====================================================
        // DEFAULT STATUS
        // =====================================================

        String voltageStatus = "NORMAL";
        String currentStatus = "NORMAL";
        String powerStatus = "NORMAL";
        String frequencyStatus = "NORMAL";
        String powerFactorStatus = "EXCELLENT";
        String wifiStatus = "GOOD";


        // =====================================================
        // AC SUPPLY DETECTION
        //
        // IMPORTANT:
        // If voltage is below 10V, assume AC supply is absent.
        //
        // Frequency and PF values are NOT meaningful when
        // mains voltage is unavailable.
        // =====================================================

        boolean acSupplyAvailable =
                voltage != null && voltage >= 10.0;


        if (!acSupplyAvailable) {

            // =================================================
            // AC SUPPLY OFFLINE
            // =================================================

            voltageStatus = "NO SUPPLY";

            currentStatus = "NO SUPPLY";

            powerStatus = "NO SUPPLY";

            frequencyStatus = "NO SUPPLY";

            powerFactorStatus = "NO SUPPLY";


            String message =
                    "AC supply disconnected or unavailable";


            anomalies.add(message);


            recommendations.add(
                    "Check the mains connection if AC power is expected to be available."
            );


            recommendations.add(
                    "Frequency and power-factor monitoring are paused until a valid AC supply returns."
            );


            detectedNow.add("AC_SUPPLY_LOST");


            saveIfNew(
                    "AC_SUPPLY_LOST",
                    deviceId,
                    "CRITICAL",
                    "AC Supply",
                    voltage != null ? voltage : 0.0,
                    "V",
                    message
            );


        } else {

            // =====================================================
            // AC EXISTS
            //
            // Only evaluate electrical parameters when AC
            // supply is actually present.
            // =====================================================


            // =====================================================
            // VOLTAGE
            // =====================================================

            if (voltage < 200) {

                voltageStatus = "LOW";

                String message =
                        "Low voltage detected: "
                                + voltage
                                + " V";

                anomalies.add(message);

                recommendations.add(
                        "Check the mains supply for undervoltage."
                );

                detectedNow.add("LOW_VOLTAGE");

                saveIfNew(
                        "LOW_VOLTAGE",
                        deviceId,
                        "WARNING",
                        "Voltage",
                        voltage,
                        "V",
                        message
                );

            } else if (voltage > 250) {

                voltageStatus = "HIGH";

                String message =
                        "High voltage detected: "
                                + voltage
                                + " V";

                anomalies.add(message);

                recommendations.add(
                        "Monitor the mains supply and protect sensitive appliances from overvoltage."
                );

                detectedNow.add("HIGH_VOLTAGE");

                saveIfNew(
                        "HIGH_VOLTAGE",
                        deviceId,
                        "WARNING",
                        "Voltage",
                        voltage,
                        "V",
                        message
                );
            }


            // =====================================================
            // CURRENT
            // =====================================================

            if (current == null) {

                currentStatus = "UNKNOWN";

                anomalies.add(
                        "Current reading unavailable"
                );

            } else if (current > 10) {

                currentStatus = "HIGH";

                String message =
                        "High current detected: "
                                + current
                                + " A";

                anomalies.add(message);

                recommendations.add(
                        "Check connected loads for excessive current consumption."
                );

                detectedNow.add("HIGH_CURRENT");

                saveIfNew(
                        "HIGH_CURRENT",
                        deviceId,
                        "WARNING",
                        "Current",
                        current,
                        "A",
                        message
                );
            }


            // =====================================================
            // POWER
            // =====================================================

            if (power == null) {

                powerStatus = "UNKNOWN";

                anomalies.add(
                        "Power reading unavailable"
                );

            } else if (power > 3000) {

                powerStatus = "HIGH";

                String message =
                        "High power consumption detected: "
                                + power
                                + " W";

                anomalies.add(message);

                recommendations.add(
                        "Consider reducing or distributing high-power loads."
                );

                detectedNow.add("HIGH_POWER");

                saveIfNew(
                        "HIGH_POWER",
                        deviceId,
                        "WARNING",
                        "Power",
                        power,
                        "W",
                        message
                );
            }


            // =====================================================
            // FREQUENCY
            // =====================================================

            if (frequency == null) {

                frequencyStatus = "UNKNOWN";

                anomalies.add(
                        "Frequency reading unavailable"
                );

            } else if (
                    frequency < 49.0 ||
                    frequency > 51.0
            ) {

                frequencyStatus = "ABNORMAL";

                String message =
                        "Abnormal supply frequency detected: "
                                + frequency
                                + " Hz";

                anomalies.add(message);

                recommendations.add(
                        "Monitor the AC supply because frequency is outside the expected range."
                );

                detectedNow.add("BAD_FREQUENCY");

                saveIfNew(
                        "BAD_FREQUENCY",
                        deviceId,
                        "WARNING",
                        "Frequency",
                        frequency,
                        "Hz",
                        message
                );
            }


            // =====================================================
            // POWER FACTOR
            // =====================================================

            if (powerFactor == null) {

                powerFactorStatus = "UNKNOWN";

                anomalies.add(
                        "Power factor unavailable"
                );

            } else if (powerFactor < 0.80) {

                powerFactorStatus = "POOR";

                String message =
                        "Poor power factor detected: "
                                + powerFactor;

                anomalies.add(message);

                recommendations.add(
                        "Inspect inductive loads and consider power-factor correction."
                );

                detectedNow.add("LOW_POWER_FACTOR");

                saveIfNew(
                        "LOW_POWER_FACTOR",
                        deviceId,
                        "WARNING",
                        "Power Factor",
                        powerFactor,
                        "",
                        message
                );

            } else if (powerFactor < 0.95) {

                powerFactorStatus = "GOOD";

                recommendations.add(
                        "Power factor is acceptable but could be improved."
                );
            }
        }


        // =====================================================
        // WIFI
        //
        // WiFi is independent of AC supply.
        // Therefore we ALWAYS evaluate it.
        // =====================================================

        if (wifi == null) {

            wifiStatus = "UNKNOWN";

        } else if (wifi < -80) {

            wifiStatus = "POOR";

            String message =
                    "Very weak ESP32 WiFi signal: "
                            + wifi
                            + " dBm";

            anomalies.add(message);

            recommendations.add(
                    "Move the ESP32 closer to the WiFi access point."
            );

            detectedNow.add("WEAK_WIFI");

            saveIfNew(
                    "WEAK_WIFI",
                    deviceId,
                    "WARNING",
                    "WiFi",
                    wifi.doubleValue(),
                    "dBm",
                    message
            );

        } else if (wifi < -67) {

            wifiStatus = "FAIR";

            recommendations.add(
                    "ESP32 WiFi signal could be improved."
            );
        }


        // =====================================================
        // REMOVE ANOMALIES NO LONGER ACTIVE
        // =====================================================

        activeAnomalies.retainAll(detectedNow);


        // =====================================================
        // SYSTEM HEALTH
        // =====================================================

        int anomalyCount = anomalies.size();

        String systemHealth;


        // AC loss gets its own system state

        if (!acSupplyAvailable) {

            systemHealth = "AC OFFLINE";

        } else if (anomalyCount == 0) {

            systemHealth = "NORMAL";

        } else if (anomalyCount <= 2) {

            systemHealth = "WARNING";

        } else {

            systemHealth = "CRITICAL";
        }


        // =====================================================
        // NORMAL RECOMMENDATIONS
        // =====================================================

        if (recommendations.isEmpty()) {

            recommendations.add(
                    "Electrical parameters are currently within the configured operating ranges."
            );

            recommendations.add(
                    "Continue monitoring energy consumption for unusual changes."
            );
        }


        // =====================================================
        // RESPONSE
        // =====================================================

        return new IntelligenceResponse(
                systemHealth,

                voltageStatus,
                currentStatus,
                powerStatus,
                frequencyStatus,
                powerFactorStatus,
                wifiStatus,

                anomalyCount,

                anomalies,

                recommendations
        );
    }


    // =====================================================
    // SAVE ANOMALY ONLY ONCE WHILE ACTIVE
    // =====================================================

    private void saveIfNew(
            String anomalyType,
            String deviceId,
            String severity,
            String parameterName,
            Double measuredValue,
            String unit,
            String message) {

        if (!activeAnomalies.contains(anomalyType)) {

            anomalyEventService.saveAnomaly(
                    deviceId,
                    anomalyType,
                    severity,
                    parameterName,
                    measuredValue,
                    unit,
                    message
            );

            activeAnomalies.add(anomalyType);
        }
    }
}