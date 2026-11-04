package com.smartedge.service;

import com.smartedge.entity.AnomalyEvent;
import com.smartedge.entity.LoadEvent;
import com.smartedge.model.EventChainResult;
import com.smartedge.repository.AnomalyEventRepository;
import com.smartedge.repository.LoadEventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class EventChainService {

    private final AnomalyEventRepository anomalyEventRepository;

    private final LoadEventRepository loadEventRepository;


    // =========================================================
    // CONFIGURATION
    // =========================================================

    /*
     * Maximum time allowed between related events.
     */
    private static final long MAX_CHAIN_WINDOW_SECONDS = 10;


    /*
     * Minimum confidence required before returning a diagnosis.
     */
    private static final double MIN_CONFIDENCE = 0.50;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public EventChainService(
            AnomalyEventRepository anomalyEventRepository,
            LoadEventRepository loadEventRepository) {

        this.anomalyEventRepository =
                anomalyEventRepository;

        this.loadEventRepository =
                loadEventRepository;
    }


    // =========================================================
    // MAIN METHOD
    // =========================================================

    public EventChainResult analyzeDevice(
            String deviceId) {

        EventChainResult result =
                new EventChainResult();

        result.setDeviceId(deviceId);


        if (deviceId == null || deviceId.isBlank()) {

            return createUnknownResult(
                    result,
                    "Invalid device ID."
            );
        }


        // -----------------------------------------------------
        // Retrieve recent events
        // -----------------------------------------------------

        List<AnomalyEvent> anomalyEvents =
                anomalyEventRepository
                        .findTop20ByOrderByDetectedAtDesc();

        List<LoadEvent> loadEvents =
                loadEventRepository
                        .findTop20ByOrderByDetectedAtDesc();


        // -----------------------------------------------------
        // Combine events belonging to this device
        // -----------------------------------------------------

        List<ChainEvent> events =
                new ArrayList<>();


        for (AnomalyEvent event : anomalyEvents) {

            if (deviceId.equals(event.getDeviceId())) {

                events.add(
                        new ChainEvent(
                                event.getAnomalyType(),
                                event.getSeverity(),
                                event.getDetectedAt(),
                                event.getConfidence(),
                                "ANOMALY"
                        )
                );
            }
        }


        for (LoadEvent event : loadEvents) {

            if (deviceId.equals(event.getDeviceId())) {

                events.add(
                        new ChainEvent(
                                event.getEventType(),
                                null,
                                event.getDetectedAt(),
                                event.getConfidence(),
                                "LOAD"
                        )
                );
            }
        }


        // -----------------------------------------------------
        // No events
        // -----------------------------------------------------

        if (events.isEmpty()) {

            return createUnknownResult(
                    result,
                    "No recent events available for this device."
            );
        }


        // -----------------------------------------------------
        // Sort chronologically
        // -----------------------------------------------------

        events.sort(
                Comparator.comparing(
                        ChainEvent::getDetectedAt
                )
        );


        // -----------------------------------------------------
        // Check patterns
        // -----------------------------------------------------

        EventChainResult supplyInterruption =
                detectSupplyInterruption(
                        deviceId,
                        events
                );

        if (isValidDiagnosis(supplyInterruption)) {

            return supplyInterruption;
        }


        EventChainResult supplyInstability =
                detectSupplyInstability(
                        deviceId,
                        events
                );

        if (isValidDiagnosis(supplyInstability)) {

            return supplyInstability;
        }


        EventChainResult overload =
                detectPossibleOverload(
                        deviceId,
                        events
                );

        if (isValidDiagnosis(overload)) {

            return overload;
        }


        // -----------------------------------------------------
        // No known pattern
        // -----------------------------------------------------

        result.setDiagnosis("NO_KNOWN_PATTERN");

        result.setConfidence(0.0);

        result.setSeverity("NORMAL");

        result.setPatternMatch(0.0);

        result.setTemporalMatch(0.0);

        result.setEventConfidence(0.0);

        result.setTimeWindowSeconds(0L);

        result.setEventSequence(
                getEventNames(events)
        );

        result.setExplanation(
                "Recent events were detected, but no known event-chain pattern was matched."
        );

        return result;
    }


    // =========================================================
    // PATTERN 1
    //
    // LOW_VOLTAGE → LOAD_OFF
    //
    // Diagnosis:
    // SUPPLY_INTERRUPTION
    // =========================================================

    private EventChainResult detectSupplyInterruption(
            String deviceId,
            List<ChainEvent> events) {

        for (int i = 0; i < events.size() - 1; i++) {

            ChainEvent first =
                    events.get(i);

            ChainEvent second =
                    events.get(i + 1);


            if (!"LOW_VOLTAGE".equals(
                    first.getType())) {

                continue;
            }


            if (!"LOAD_OFF".equals(
                    second.getType())) {

                continue;
            }


            long seconds =
                    secondsBetween(
                            first.getDetectedAt(),
                            second.getDetectedAt()
                    );


            if (seconds < 0
                    || seconds > MAX_CHAIN_WINDOW_SECONDS) {

                continue;
            }


            double temporalMatch =
                    calculateTemporalMatch(
                            seconds
                    );


            double eventConfidence =
                    calculateEventConfidence(
                            first,
                            second
                    );


            double patternMatch = 1.0;


            double confidence =
                    patternMatch
                            * temporalMatch
                            * eventConfidence;


            EventChainResult result =
                    buildResult(
                            deviceId,
                            "SUPPLY_INTERRUPTION",
                            confidence,
                            first,
                            second,
                            patternMatch,
                            temporalMatch,
                            eventConfidence,
                            seconds
                    );


            if (confidence >= MIN_CONFIDENCE) {

                return result;
            }
        }

        return null;
    }


    // =========================================================
    // PATTERN 2
    //
    // LOW_VOLTAGE → BAD_FREQUENCY
    //
    // Diagnosis:
    // SUPPLY_INSTABILITY
    // =========================================================

    private EventChainResult detectSupplyInstability(
            String deviceId,
            List<ChainEvent> events) {

        for (int i = 0; i < events.size() - 1; i++) {

            ChainEvent first =
                    events.get(i);

            ChainEvent second =
                    events.get(i + 1);


            if (!"LOW_VOLTAGE".equals(
                    first.getType())) {

                continue;
            }


            if (!"BAD_FREQUENCY".equals(
                    second.getType())) {

                continue;
            }


            long seconds =
                    secondsBetween(
                            first.getDetectedAt(),
                            second.getDetectedAt()
                    );


            if (seconds < 0
                    || seconds > MAX_CHAIN_WINDOW_SECONDS) {

                continue;
            }


            double temporalMatch =
                    calculateTemporalMatch(
                            seconds
                    );


            double eventConfidence =
                    calculateEventConfidence(
                            first,
                            second
                    );


            double patternMatch = 1.0;


            double confidence =
                    patternMatch
                            * temporalMatch
                            * eventConfidence;


            EventChainResult result =
                    buildResult(
                            deviceId,
                            "SUPPLY_INSTABILITY",
                            confidence,
                            first,
                            second,
                            patternMatch,
                            temporalMatch,
                            eventConfidence,
                            seconds
                    );


            if (confidence >= MIN_CONFIDENCE) {

                return result;
            }
        }

        return null;
    }


    // =========================================================
    // PATTERN 3
    //
    // HIGH_CURRENT → HIGH_POWER
    //
    // Diagnosis:
    // POSSIBLE_OVERLOAD
    // =========================================================

    private EventChainResult detectPossibleOverload(
            String deviceId,
            List<ChainEvent> events) {

        for (int i = 0; i < events.size() - 1; i++) {

            ChainEvent first =
                    events.get(i);

            ChainEvent second =
                    events.get(i + 1);


            if (!"HIGH_CURRENT".equals(
                    first.getType())) {

                continue;
            }


            if (!"HIGH_POWER".equals(
                    second.getType())) {

                continue;
            }


            long seconds =
                    secondsBetween(
                            first.getDetectedAt(),
                            second.getDetectedAt()
                    );


            if (seconds < 0
                    || seconds > MAX_CHAIN_WINDOW_SECONDS) {

                continue;
            }


            double temporalMatch =
                    calculateTemporalMatch(
                            seconds
                    );


            double eventConfidence =
                    calculateEventConfidence(
                            first,
                            second
                    );


            double patternMatch = 1.0;


            double confidence =
                    patternMatch
                            * temporalMatch
                            * eventConfidence;


            EventChainResult result =
                    buildResult(
                            deviceId,
                            "POSSIBLE_OVERLOAD",
                            confidence,
                            first,
                            second,
                            patternMatch,
                            temporalMatch,
                            eventConfidence,
                            seconds
                    );


            if (confidence >= MIN_CONFIDENCE) {

                return result;
            }
        }

        return null;
    }


    // =========================================================
    // TEMPORAL MATCH
    // =========================================================

    private double calculateTemporalMatch(
            long seconds) {

        if (seconds <= 0) {

            return 1.0;
        }


        if (seconds >= MAX_CHAIN_WINDOW_SECONDS) {

            return 0.50;
        }


        /*
         * Closer events receive higher confidence.
         *
         * 0 sec  → 1.00
         * 5 sec  → 0.75
         * 10 sec → 0.50
         */

        return 1.0
                - (
                0.5
                        * (
                        (double) seconds
                                / MAX_CHAIN_WINDOW_SECONDS
                )
        );
    }


    // =========================================================
    // EVENT CONFIDENCE
    // =========================================================

    private double calculateEventConfidence(
            ChainEvent first,
            ChainEvent second) {

        double firstConfidence =
                first.getConfidence() == null
                        ? 1.0
                        : first.getConfidence();


        double secondConfidence =
                second.getConfidence() == null
                        ? 1.0
                        : second.getConfidence();


        return (
                firstConfidence
                        + secondConfidence
        ) / 2.0;
    }


    // =========================================================
    // BUILD RESULT
    // =========================================================

    private EventChainResult buildResult(
            String deviceId,
            String diagnosis,
            double confidence,
            ChainEvent first,
            ChainEvent second,
            double patternMatch,
            double temporalMatch,
            double eventConfidence,
            long seconds) {

        EventChainResult result =
                new EventChainResult();

        result.setDeviceId(deviceId);

        result.setDiagnosis(
                diagnosis
        );

        result.setConfidence(
                round(confidence)
        );

        result.setPatternMatch(
                round(patternMatch)
        );

        result.setTemporalMatch(
                round(temporalMatch)
        );

        result.setEventConfidence(
                round(eventConfidence)
        );

        result.setTimeWindowSeconds(
                seconds
        );


        List<String> sequence =
                new ArrayList<>();

        sequence.add(
                first.getType()
        );

        sequence.add(
                second.getType()
        );

        result.setEventSequence(
                sequence
        );


        result.setSeverity(
                determineDiagnosisSeverity(
                        diagnosis
                )
        );


        result.setExplanation(
                createExplanation(
                        diagnosis,
                        first,
                        second,
                        seconds,
                        confidence
                )
        );


        return result;
    }


    // =========================================================
    // DIAGNOSIS SEVERITY
    // =========================================================

    private String determineDiagnosisSeverity(
            String diagnosis) {

        switch (diagnosis) {

            case "SUPPLY_INTERRUPTION":
                return "CRITICAL";

            case "SUPPLY_INSTABILITY":
                return "HIGH";

            case "POSSIBLE_OVERLOAD":
                return "HIGH";

            default:
                return "NORMAL";
        }
    }


    // =========================================================
    // EXPLANATION
    // =========================================================

    private String createExplanation(
            String diagnosis,
            ChainEvent first,
            ChainEvent second,
            long seconds,
            double confidence) {

        return String.format(
                "%s followed by %s within %d seconds indicates %s. Chain confidence: %.2f.",
                first.getType(),
                second.getType(),
                seconds,
                diagnosis.replace("_", " ").toLowerCase(),
                confidence
        );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private boolean isValidDiagnosis(
            EventChainResult result) {

        return result != null
                && result.getConfidence() != null
                && result.getConfidence()
                >= MIN_CONFIDENCE;
    }


    // =========================================================
    // UNKNOWN RESULT
    // =========================================================

    private EventChainResult createUnknownResult(
            EventChainResult result,
            String explanation) {

        result.setDiagnosis(
                "INSUFFICIENT_EVENTS"
        );

        result.setConfidence(0.0);

        result.setSeverity("NORMAL");

        result.setPatternMatch(0.0);

        result.setTemporalMatch(0.0);

        result.setEventConfidence(0.0);

        result.setTimeWindowSeconds(0L);

        result.setEventSequence(
                new ArrayList<>()
        );

        result.setExplanation(
                explanation
        );

        return result;
    }


    // =========================================================
    // EVENT NAMES
    // =========================================================

    private List<String> getEventNames(
            List<ChainEvent> events) {

        List<String> names =
                new ArrayList<>();

        for (ChainEvent event : events) {

            names.add(
                    event.getType()
            );
        }

        return names;
    }


    // =========================================================
    // TIME DIFFERENCE
    // =========================================================

    private long secondsBetween(
            LocalDateTime first,
            LocalDateTime second) {

        if (first == null || second == null) {

            return Long.MAX_VALUE;
        }

        return Math.abs(
                Duration.between(
                        first,
                        second
                ).getSeconds()
        );
    }


    // =========================================================
    // ROUND
    // =========================================================

    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }


    // =========================================================
    // INTERNAL EVENT REPRESENTATION
    // =========================================================

    private static class ChainEvent {

        private final String type;

        private final String severity;

        private final LocalDateTime detectedAt;

        private final Double confidence;

        private final String source;


        public ChainEvent(
                String type,
                String severity,
                LocalDateTime detectedAt,
                Double confidence,
                String source) {

            this.type = type;
            this.severity = severity;
            this.detectedAt = detectedAt;
            this.confidence = confidence;
            this.source = source;
        }


        public String getType() {
            return type;
        }

        public String getSeverity() {
            return severity;
        }

        public LocalDateTime getDetectedAt() {
            return detectedAt;
        }

        public Double getConfidence() {
            return confidence;
        }

        public String getSource() {
            return source;
        }
    }
}