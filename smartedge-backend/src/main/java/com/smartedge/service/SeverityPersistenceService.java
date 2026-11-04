package com.smartedge.service;

import com.smartedge.model.DeviationResult;
import com.smartedge.model.SeverityPersistence;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SeverityPersistenceService {

    /*
     * Stores the current state of each device in memory.
     *
     * Key   = deviceId
     * Value = current persistence state
     */
    private final Map<String, DeviceSeverityState> deviceStates =
            new HashMap<>();


    /*
     * Number of consecutive abnormal readings required
     * before the anomaly is considered persistent.
     */
    private static final int PERSISTENCE_THRESHOLD = 3;


    /*
     * Number of consecutive normal readings required
     * before clearing an existing persistent anomaly.
     */
    private static final int RECOVERY_THRESHOLD = 3;


    /*
     * Main processing method.
     */
    public synchronized SeverityPersistence process(
            DeviationResult deviationResult) {

        SeverityPersistence result =
                new SeverityPersistence();

        if (deviationResult == null
                || deviationResult.getDeviceId() == null) {

            result.setCurrentSeverity("UNKNOWN");
            result.setPreviousSeverity("UNKNOWN");
            result.setPersistent(false);

            return result;
        }

        String deviceId =
                deviationResult.getDeviceId();

        result.setDeviceId(deviceId);


        // -----------------------------------------------------
        // Retrieve or create device state
        // -----------------------------------------------------

        DeviceSeverityState state =
                deviceStates.computeIfAbsent(
                        deviceId,
                        key -> new DeviceSeverityState()
                );


        String newSeverity =
                deviationResult.getDeviationLevel();

        String previousSeverity =
                state.currentSeverity;


        result.setPreviousSeverity(
                previousSeverity
        );


        // =====================================================
        // NORMAL READING
        // =====================================================

        if ("NORMAL".equals(newSeverity)) {

            state.consecutiveNormalReadings++;

            /*
             * Reset abnormal counter only after a normal
             * reading is confirmed.
             */

            if (state.consecutiveNormalReadings
                    >= RECOVERY_THRESHOLD) {

                state.consecutiveAnomalies = 0;

                state.anomalyStartTime = null;

                state.persistent = false;
            }

            state.currentSeverity =
                    "NORMAL";

            result.setCurrentSeverity(
                    "NORMAL"
            );

            result.setConsecutiveAnomalies(
                    state.consecutiveAnomalies
            );

            result.setConsecutiveNormalReadings(
                    state.consecutiveNormalReadings
            );

            result.setPersistent(
                    state.persistent
            );

            result.setDurationSeconds(
                    calculateDuration(
                            state.anomalyStartTime
                    )
            );

            return result;
        }


        // =====================================================
        // INSUFFICIENT DATA
        // =====================================================

        if ("INSUFFICIENT_DATA".equals(newSeverity)) {

            result.setCurrentSeverity(
                    "INSUFFICIENT_DATA"
            );

            result.setConsecutiveAnomalies(
                    state.consecutiveAnomalies
            );

            result.setConsecutiveNormalReadings(
                    state.consecutiveNormalReadings
            );

            result.setPersistent(
                    false
            );

            result.setDurationSeconds(0);

            return result;
        }


        // =====================================================
        // ABNORMAL READING
        // =====================================================

        state.consecutiveAnomalies++;

        state.consecutiveNormalReadings = 0;


        /*
         * Start timing when the anomaly first appears.
         */

        if (state.anomalyStartTime == null) {

            state.anomalyStartTime =
                    LocalDateTime.now();
        }


        /*
         * Persistence becomes true after enough
         * consecutive abnormal readings.
         */

        if (state.consecutiveAnomalies
                >= PERSISTENCE_THRESHOLD) {

            state.persistent = true;
        }


        // =====================================================
        // SEVERITY ESCALATION
        // =====================================================

        String effectiveSeverity =
                calculateEscalatedSeverity(
                        newSeverity,
                        state.consecutiveAnomalies
                );


        state.currentSeverity =
                effectiveSeverity;


        // =====================================================
        // BUILD RESPONSE
        // =====================================================

        result.setCurrentSeverity(
                effectiveSeverity
        );

        result.setConsecutiveAnomalies(
                state.consecutiveAnomalies
        );

        result.setConsecutiveNormalReadings(
                state.consecutiveNormalReadings
        );

        result.setPersistent(
                state.persistent
        );

        result.setDurationSeconds(
                calculateDuration(
                        state.anomalyStartTime
                )
        );

        return result;
    }


    // =========================================================
    // SEVERITY ESCALATION
    // =========================================================

    private String calculateEscalatedSeverity(
            String originalSeverity,
            int consecutiveAnomalies) {

        /*
         * CRITICAL remains CRITICAL immediately.
         */

        if ("CRITICAL".equals(originalSeverity)) {

            return "CRITICAL";
        }


        /*
         * HIGH remains HIGH.
         *
         * Persistent HIGH can later be integrated with
         * system-health escalation.
         */

        if ("HIGH".equals(originalSeverity)) {

            return "HIGH";
        }


        /*
         * MODERATE becomes HIGH when persistent.
         */

        if ("MODERATE".equals(originalSeverity)) {

            if (consecutiveAnomalies >= 5) {

                return "HIGH";
            }

            return "MODERATE";
        }


        /*
         * MILD anomaly becomes MODERATE when it persists.
         */

        if ("MILD".equals(originalSeverity)) {

            if (consecutiveAnomalies >= 6) {

                return "MODERATE";
            }

            return "MILD";
        }


        /*
         * Unknown abnormal severity.
         */

        return originalSeverity;
    }


    // =========================================================
    // DURATION
    // =========================================================

    private long calculateDuration(
            LocalDateTime startTime) {

        if (startTime == null) {

            return 0;
        }

        return Duration.between(
                startTime,
                LocalDateTime.now()
        ).getSeconds();
    }


    // =========================================================
    // INTERNAL DEVICE STATE
    // =========================================================

    private static class DeviceSeverityState {

        private int consecutiveAnomalies = 0;

        private int consecutiveNormalReadings = 0;

        private String currentSeverity = "NORMAL";

        private boolean persistent = false;

        private LocalDateTime anomalyStartTime = null;
    }
}