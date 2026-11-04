package com.smartedge.service;

import com.smartedge.entity.EnergyReading;
import com.smartedge.entity.LoadEvent;
import com.smartedge.repository.LoadEventRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoadDetectionService {

    // ==========================================
    // DEPENDENCIES
    // ==========================================

    private final LoadEventRepository loadEventRepository;

    private final LoadClassificationService loadClassificationService;


    // ==========================================
    // LOAD DETECTION THRESHOLD
    // ==========================================

    /*
     * Minimum power difference required before
     * SmartEdge considers the change to be an
     * actual load event.
     */

    private static final double LOAD_CHANGE_THRESHOLD = 10.0;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public LoadDetectionService(
            LoadEventRepository loadEventRepository,
            LoadClassificationService loadClassificationService) {

        this.loadEventRepository =
                loadEventRepository;

        this.loadClassificationService =
                loadClassificationService;
    }


    // ==========================================
    // DETECT REAL LOAD CHANGE
    // ==========================================

    public void detectLoadChange(
            EnergyReading previous,
            EnergyReading current) {

        // ======================================
        // NULL SAFETY
        // ======================================

        if (previous == null ||
                current == null) {

            return;
        }


        // ======================================
        // POWER VALIDATION
        // ======================================

        if (previous.getPower() == null ||
                current.getPower() == null) {

            return;
        }


        // ======================================
        // DEVICE SAFETY
        // ======================================

        if (previous.getDeviceId() != null &&
                current.getDeviceId() != null &&
                !previous.getDeviceId()
                        .equals(current.getDeviceId())) {

            return;
        }


        // ======================================
        // POWER VALUES
        // ======================================

        double previousPower =
                previous.getPower();

        double currentPower =
                current.getPower();


        // ======================================
        // REJECT CORRUPTED POWER VALUES
        // ======================================

        if (previousPower < 0 ||
                previousPower > 5000 ||
                currentPower < 0 ||
                currentPower > 5000) {

            return;
        }


        // ======================================
        // CALCULATE POWER CHANGE
        // ======================================

        double powerChange =
                currentPower - previousPower;


        // ======================================
        // IGNORE SMALL FLUCTUATIONS
        // ======================================

        if (Math.abs(powerChange)
                < LOAD_CHANGE_THRESHOLD) {

            return;
        }


        // ======================================
        // DETERMINE EVENT TYPE
        // ======================================

        String eventType;

        if (powerChange > 0) {

            eventType = "LOAD_ON";

        } else {

            eventType = "LOAD_OFF";
        }


        // ======================================
        // CREATE MULTI-FEATURE EVENT
        // ======================================

        createAndSaveEvent(
                current.getDeviceId(),
                eventType,
                previousPower,
                currentPower,
                current
        );
    }


    // ==========================================
    // SIMULATE LED EVENT
    // ==========================================

    public LoadEvent simulateLedEvent(
            String mode) {

        // ======================================
        // VALIDATE MODE
        // ======================================

        if (mode == null) {

            throw new IllegalArgumentException(
                    "Simulation mode is required"
            );
        }


        String normalizedMode =
                mode.toUpperCase();


        double previousPower;
        double currentPower;
        String eventType;


        // ======================================
        // SELECT SIMULATION
        // ======================================

        switch (normalizedMode) {

            // ==================================
            // LED ON
            // ==================================

            case "LED_ON":

                previousPower =
                        0.0;

                /*
                 * Approximate demo power.
                 *
                 * This is simulated data only.
                 */

                currentPower =
                        15.0;

                eventType =
                        "LOAD_ON";

                break;


            // ==================================
            // LED OFF
            // ==================================

            case "LED_OFF":

                previousPower =
                        15.0;

                currentPower =
                        0.0;

                eventType =
                        "LOAD_OFF";

                break;


            // ==================================
            // INVALID MODE
            // ==================================

            default:

                throw new IllegalArgumentException(
                        "Unknown simulation mode: "
                                + mode
                                + ". Use LED_ON or LED_OFF."
                );
        }


        // ======================================
        // POWER-ONLY SIMULATION
        // ======================================

        /*
         * The simulation does not have real
         * ESP32 edge features.
         *
         * Therefore the backward-compatible
         * power-only classifier is used.
         */

        return createAndSaveSimulationEvent(
                "ESP32-001",
                eventType,
                previousPower,
                currentPower
        );
    }


    // ==========================================
    // CREATE + CLASSIFY + SAVE REAL EVENT
    // ==========================================

    private LoadEvent createAndSaveEvent(
            String deviceId,
            String eventType,
            double previousPower,
            double currentPower,
            EnergyReading currentReading) {


        // ======================================
        // CALCULATE POWER CHANGE
        // ======================================

        double powerChange =
                currentPower - previousPower;


        // ======================================
        // READ EDGE FEATURES
        // ======================================

        double currentChange =
                getSafeValue(
                        currentReading.getDeltaCurrent()
                );


        double powerFactorChange =
                getSafeValue(
                        currentReading.getDeltaPowerFactor()
                );


        double powerChangeRate =
                getSafeValue(
                        currentReading.getPowerChangeRate()
                );


        double voltage =
                getSafeValue(
                        currentReading.getVoltage()
                );


        // ======================================
        // CHECK EDGE FEATURE AVAILABILITY
        // ======================================

        boolean edgeFeatureReady =
                currentReading.getEdgeFeatureReady() != null
                        && currentReading.getEdgeFeatureReady();


        // ======================================
        // MULTI-FEATURE CLASSIFICATION
        // ======================================

        LoadClassificationService.ClassificationResult
                classification;


        if (edgeFeatureReady) {

            /*
             * Real ESP32 event.
             *
             * Classification uses:
             *
             * 1. Power change
             * 2. Current change
             * 3. Power-factor change
             * 4. Power change rate
             * 5. Voltage
             */

            classification =
                    loadClassificationService.classify(
                            powerChange,
                            currentChange,
                            powerFactorChange,
                            powerChangeRate,
                            voltage
                    );

        } else {

            /*
             * Backward-compatible fallback.
             *
             * If edge features are unavailable,
             * classify using power change only.
             */

            classification =
                    loadClassificationService.classify(
                            powerChange
                    );
        }


        // ======================================
        // CREATE EVENT
        // ======================================

        LoadEvent event =
                new LoadEvent();


        // ======================================
        // DEVICE INFORMATION
        // ======================================

        event.setDeviceId(
                deviceId
        );


        // ======================================
        // EVENT TYPE
        // ======================================

        event.setEventType(
                eventType
        );


        // ======================================
        // POWER INFORMATION
        // ======================================

        event.setPreviousPower(
                round(
                        previousPower,
                        2
                )
        );


        event.setCurrentPower(
                round(
                        currentPower,
                        2
                )
        );


        event.setPowerChange(
                round(
                        powerChange,
                        2
                )
        );


        // ======================================
        // EDGE FEATURE SIGNATURE
        // ======================================

        /*
         * Store the actual feature values used
         * by the classifier.
         *
         * This makes every event research-ready
         * because the classification decision can
         * later be analysed using the stored
         * feature signature.
         */

        event.setDeltaCurrent(
                round(
                        currentChange,
                        4
                )
        );


        event.setDeltaPowerFactor(
                round(
                        powerFactorChange,
                        4
                )
        );


        event.setPowerChangeRate(
                round(
                        powerChangeRate,
                        4
                )
        );


        event.setVoltage(
                round(
                        voltage,
                        2
                )
        );


        // ======================================
        // CLASSIFICATION INFORMATION
        // ======================================

        event.setApplianceType(
                classification
                        .getApplianceType()
        );


        event.setConfidence(
                round(
                        classification
                                .getConfidence(),
                        2
                )
        );


        event.setClassificationMethod(
                classification
                        .getClassificationMethod()
        );


        // ======================================
        // TIMESTAMP
        // ======================================

        event.setDetectedAt(
                LocalDateTime.now()
        );


        // ======================================
        // SAVE EVENT
        // ======================================

        LoadEvent savedEvent =
                loadEventRepository.save(
                        event
                );


        // ======================================
        // CONSOLE OUTPUT
        // ======================================

        printEventDetails(
                savedEvent,
                currentReading
        );


        return savedEvent;
    }


    // ==========================================
    // CREATE + CLASSIFY + SAVE SIMULATION
    // ==========================================

    private LoadEvent createAndSaveSimulationEvent(
            String deviceId,
            String eventType,
            double previousPower,
            double currentPower) {


        // ======================================
        // CALCULATE POWER CHANGE
        // ======================================

        double powerChange =
                currentPower - previousPower;


        // ======================================
        // POWER-ONLY CLASSIFICATION
        // ======================================

        LoadClassificationService.ClassificationResult
                classification =
                loadClassificationService.classify(
                        powerChange
                );


        // ======================================
        // CREATE EVENT
        // ======================================

        LoadEvent event =
                new LoadEvent();


        // ======================================
        // DEVICE INFORMATION
        // ======================================

        event.setDeviceId(
                deviceId
        );


        // ======================================
        // EVENT TYPE
        // ======================================

        event.setEventType(
                eventType
        );


        // ======================================
        // POWER INFORMATION
        // ======================================

        event.setPreviousPower(
                round(
                        previousPower,
                        2
                )
        );


        event.setCurrentPower(
                round(
                        currentPower,
                        2
                )
        );


        event.setPowerChange(
                round(
                        powerChange,
                        2
                )
        );


        // ======================================
        // SIMULATION FEATURE VALUES
        // ======================================

        /*
         * Simulation does not contain actual
         * ESP32 feature measurements.
         *
         * Therefore these are stored as null
         * instead of fake sensor values.
         *
         * This is important for research data
         * integrity.
         */

        event.setDeltaCurrent(null);

        event.setDeltaPowerFactor(null);

        event.setPowerChangeRate(null);

        event.setVoltage(null);


        // ======================================
        // CLASSIFICATION
        // ======================================

        event.setApplianceType(
                classification
                        .getApplianceType()
        );


        event.setConfidence(
                round(
                        classification
                                .getConfidence(),
                        2
                )
        );


        event.setClassificationMethod(
                classification
                        .getClassificationMethod()
        );


        // ======================================
        // TIMESTAMP
        // ======================================

        event.setDetectedAt(
                LocalDateTime.now()
        );


        // ======================================
        // SAVE
        // ======================================

        LoadEvent savedEvent =
                loadEventRepository.save(
                        event
                );


        // ======================================
        // CONSOLE OUTPUT
        // ======================================

        printEventDetails(
                savedEvent,
                null
        );


        return savedEvent;
    }


    // ==========================================
    // PRINT EVENT DETAILS
    // ==========================================

    private void printEventDetails(
            LoadEvent savedEvent,
            EnergyReading reading) {

        System.out.println();

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "SmartEdge Load Event Detected"
        );

        System.out.println(
                "Device: "
                        + savedEvent.getDeviceId()
        );

        System.out.println(
                "Event: "
                        + savedEvent.getEventType()
        );

        System.out.println(
                "Previous Power: "
                        + savedEvent.getPreviousPower()
                        + " W"
        );

        System.out.println(
                "Current Power: "
                        + savedEvent.getCurrentPower()
                        + " W"
        );

        System.out.println(
                "Power Change: "
                        + savedEvent.getPowerChange()
                        + " W"
        );


        // ======================================
        // CLASSIFICATION
        // ======================================

        System.out.println(
                "Possible Appliance: "
                        + savedEvent.getApplianceType()
        );

        System.out.println(
                "Confidence: "
                        + round(
                                savedEvent
                                        .getConfidence()
                                        * 100,
                                0
                        )
                        + "%"
        );

        System.out.println(
                "Classification Method: "
                        + savedEvent
                                .getClassificationMethod()
        );


        // ======================================
        // STORED FEATURE SIGNATURE
        // ======================================

        System.out.println();

        System.out.println(
                "------- STORED FEATURE SIGNATURE -------"
        );

        System.out.println(
                "Delta Current: "
                        + safePrint(
                                savedEvent.getDeltaCurrent()
                        )
                        + " A"
        );

        System.out.println(
                "Delta Power Factor: "
                        + safePrint(
                                savedEvent.getDeltaPowerFactor()
                        )
        );

        System.out.println(
                "Power Change Rate: "
                        + safePrint(
                                savedEvent.getPowerChangeRate()
                        )
                        + " W/s"
        );

        System.out.println(
                "Voltage: "
                        + safePrint(
                                savedEvent.getVoltage()
                        )
                        + " V"
        );


        // ======================================
        // ORIGINAL READING FEATURE INFORMATION
        // ======================================

        if (reading != null) {

            System.out.println();

            System.out.println(
                    "------- ESP32 EDGE FEATURE STATUS -------"
            );

            System.out.println(
                    "Delta Power: "
                            + safePrint(
                                    reading.getDeltaPower()
                            )
                            + " W"
            );

            System.out.println(
                    "Delta Current: "
                            + safePrint(
                                    reading.getDeltaCurrent()
                            )
                            + " A"
            );

            System.out.println(
                    "Delta Power Factor: "
                            + safePrint(
                                    reading.getDeltaPowerFactor()
                            )
            );

            System.out.println(
                    "Power Change Rate: "
                            + safePrint(
                                    reading.getPowerChangeRate()
                            )
                            + " W/s"
            );

            System.out.println(
                    "Voltage: "
                            + safePrint(
                                    reading.getVoltage()
                            )
                            + " V"
            );

            System.out.println(
                    "Edge Features Ready: "
                            + reading
                                    .getEdgeFeatureReady()
            );
        }


        System.out.println(
                "=========================================="
        );

        System.out.println();
    }


    // ==========================================
    // SAFE NUMERIC VALUE
    // ==========================================

    private double getSafeValue(
            Double value) {

        if (value == null) {
            return 0.0;
        }

        if (value.isNaN() ||
                value.isInfinite()) {

            return 0.0;
        }

        return value;
    }


    // ==========================================
    // SAFE PRINT VALUE
    // ==========================================

    private String safePrint(
            Double value) {

        if (value == null) {
            return "N/A";
        }

        if (value.isNaN() ||
                value.isInfinite()) {

            return "N/A";
        }

        return String.valueOf(
                round(
                        value,
                        4
                )
        );
    }


    // ==========================================
    // GET RECENT LOAD EVENTS
    // ==========================================

    public List<LoadEvent> getRecentEvents() {

        return loadEventRepository
                .findTop20ByOrderByDetectedAtDesc();
    }


    // ==========================================
    // ROUNDING HELPER
    // ==========================================

    private double round(
            double value,
            int decimalPlaces) {

        double factor =
                Math.pow(
                        10,
                        decimalPlaces
                );

        return Math.round(
                value * factor
        ) / factor;
    }
}