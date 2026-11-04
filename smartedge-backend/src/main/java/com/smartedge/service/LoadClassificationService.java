package com.smartedge.service;

import org.springframework.stereotype.Service;

@Service
public class LoadClassificationService {

    // ==========================================
    // CLASSIFICATION RESULT
    // ==========================================

    public static class ClassificationResult {

        private final String applianceType;
        private final double confidence;
        private final String classificationMethod;

        public ClassificationResult(
                String applianceType,
                double confidence,
                String classificationMethod) {

            this.applianceType = applianceType;
            this.confidence = confidence;
            this.classificationMethod =
                    classificationMethod;
        }

        public String getApplianceType() {
            return applianceType;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getClassificationMethod() {
            return classificationMethod;
        }
    }


    // ==========================================
    // LEGACY POWER-ONLY CLASSIFICATION
    // ==========================================

    /*
     * Kept for backward compatibility with
     * existing simulation and older code.
     *
     * Since no additional features are available,
     * the adaptive classifier receives neutral
     * feature values.
     */

    public ClassificationResult classify(
            double powerChange) {

        return classify(
                powerChange,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }


    // ==========================================
    // MULTI-FEATURE ADAPTIVE CLASSIFICATION
    // ==========================================

    public ClassificationResult classify(
            double powerChange,
            double currentChange,
            double powerFactorChange,
            double powerChangeRate,
            double voltage) {


        // ======================================
        // ABSOLUTE FEATURE VALUES
        // ======================================

        double watts =
                Math.abs(powerChange);

        double amps =
                Math.abs(currentChange);

        double pfChange =
                Math.abs(powerFactorChange);

        double rate =
                Math.abs(powerChangeRate);


        // ======================================
        // VERY SMALL CHANGE
        // ======================================

        if (watts < 5) {

            double confidence =
                    adaptiveConfidence(
                            0.40,
                            watts,
                            amps,
                            pfChange,
                            rate,
                            voltage,
                            0.0
                    );

            return new ClassificationResult(
                    "UNKNOWN_SMALL_LOAD",
                    confidence,
                    "CONFIDENCE_ADAPTIVE_MULTI_FEATURE"
            );
        }


        // ======================================
        // SMALL ELECTRONIC LOAD
        // ======================================

        if (watts <= 20) {

            double expectedCurrent =
                    calculateExpectedCurrent(
                            watts,
                            voltage
                    );

            double confidence =
                    adaptiveConfidence(
                            0.60,
                            watts,
                            amps,
                            pfChange,
                            rate,
                            voltage,
                            expectedCurrent
                    );

            return new ClassificationResult(
                    "SMALL_ELECTRONIC_LOAD",
                    confidence,
                    "CONFIDENCE_ADAPTIVE_MULTI_FEATURE"
            );
        }


        // ======================================
        // POSSIBLE FAN / SMALL APPLIANCE
        // ======================================

        if (watts <= 80) {

            double expectedCurrent =
                    calculateExpectedCurrent(
                            watts,
                            voltage
                    );

            double confidence =
                    adaptiveConfidence(
                            0.68,
                            watts,
                            amps,
                            pfChange,
                            rate,
                            voltage,
                            expectedCurrent
                    );

            /*
             * A noticeable PF change provides
             * additional evidence for motor/
             * inductive behavior.
             */

            if (pfChange > 0.05) {
                confidence += 0.06;
            }

            /*
             * A meaningful transition rate supports
             * a genuine switching event.
             */

            if (rate > 10) {
                confidence += 0.03;
            }

            return new ClassificationResult(
                    "POSSIBLE_FAN",
                    clampConfidence(confidence),
                    "CONFIDENCE_ADAPTIVE_MULTI_FEATURE"
            );
        }


        // ======================================
        // MEDIUM APPLIANCE
        // ======================================

        if (watts <= 200) {

            double expectedCurrent =
                    calculateExpectedCurrent(
                            watts,
                            voltage
                    );

            double confidence =
                    adaptiveConfidence(
                            0.63,
                            watts,
                            amps,
                            pfChange,
                            rate,
                            voltage,
                            expectedCurrent
                    );

            return new ClassificationResult(
                    "MEDIUM_APPLIANCE",
                    confidence,
                    "CONFIDENCE_ADAPTIVE_MULTI_FEATURE"
            );
        }


        // ======================================
        // LARGE APPLIANCE
        // ======================================

        if (watts <= 500) {

            double expectedCurrent =
                    calculateExpectedCurrent(
                            watts,
                            voltage
                    );

            double confidence =
                    adaptiveConfidence(
                            0.58,
                            watts,
                            amps,
                            pfChange,
                            rate,
                            voltage,
                            expectedCurrent
                    );

            return new ClassificationResult(
                    "LARGE_APPLIANCE",
                    confidence,
                    "CONFIDENCE_ADAPTIVE_MULTI_FEATURE"
            );
        }


        // ======================================
        // HIGH POWER APPLIANCE
        // ======================================

        if (watts <= 1500) {

            double expectedCurrent =
                    calculateExpectedCurrent(
                            watts,
                            voltage
                    );

            double confidence =
                    adaptiveConfidence(
                            0.58,
                            watts,
                            amps,
                            pfChange,
                            rate,
                            voltage,
                            expectedCurrent
                    );

            return new ClassificationResult(
                    "HIGH_POWER_APPLIANCE",
                    confidence,
                    "CONFIDENCE_ADAPTIVE_MULTI_FEATURE"
            );
        }


        // ======================================
        // HEAVY LOAD
        // ======================================

        double expectedCurrent =
                calculateExpectedCurrent(
                        watts,
                        voltage
                );

        double confidence =
                adaptiveConfidence(
                        0.53,
                        watts,
                        amps,
                        pfChange,
                        rate,
                        voltage,
                        expectedCurrent
                );

        return new ClassificationResult(
                "HEAVY_LOAD",
                confidence,
                "CONFIDENCE_ADAPTIVE_MULTI_FEATURE"
        );
    }


    // ==========================================
    // ADAPTIVE CONFIDENCE ENGINE
    // ==========================================

    private double adaptiveConfidence(
            double baseConfidence,
            double watts,
            double amps,
            double pfChange,
            double rate,
            double voltage,
            double expectedCurrent) {


        double confidence =
                baseConfidence;


        // ======================================
        // 1. POWER MAGNITUDE EVIDENCE
        // ======================================

        /*
         * A measurable power change provides
         * stronger evidence than a tiny change.
         */

        if (watts >= 10) {

            confidence += 0.04;

        } else if (watts >= 5) {

            confidence += 0.02;
        }


        // ======================================
        // 2. CURRENT EVIDENCE
        // ======================================

        /*
         * If voltage is available, compare the
         * measured current change against the
         * current expected from the power change.
         */

        if (expectedCurrent > 0.001) {

            double currentRatio =
                    amps / expectedCurrent;

            /*
             * Strong consistency.
             */

            if (
                    currentRatio >= 0.70 &&
                    currentRatio <= 1.30
            ) {

                confidence += 0.10;
            }

            /*
             * Moderate consistency.
             */

            else if (
                    currentRatio >= 0.50 &&
                    currentRatio <= 1.50
            ) {

                confidence += 0.05;
            }

            /*
             * Strong inconsistency.
             */

            else {

                confidence -= 0.08;
            }
        }

        /*
         * When voltage is unavailable, use a
         * conservative current-based check.
         */

        else {

            if (amps > 0.05) {
                confidence += 0.03;
            }
        }


        // ======================================
        // 3. POWER FACTOR EVIDENCE
        // ======================================

        /*
         * Very small PF variation means the
         * electrical signature is stable.
         */

        if (pfChange <= 0.02) {

            confidence += 0.05;
        }

        /*
         * Moderate PF variation can provide
         * useful appliance-specific information.
         */

        else if (pfChange <= 0.10) {

            confidence += 0.03;
        }

        /*
         * Extremely large PF changes may indicate
         * noisy or inconsistent measurements.
         */

        else if (pfChange > 0.30) {

            confidence -= 0.08;
        }


        // ======================================
        // 4. TRANSITION RATE EVIDENCE
        // ======================================

        /*
         * A reasonable positive/negative transition
         * supports a genuine switching event.
         */

        if (rate >= 1 && rate <= 100) {

            confidence += 0.04;
        }

        /*
         * Extremely rapid changes may indicate
         * transient noise or unstable readings.
         */

        else if (rate > 1000) {

            confidence -= 0.12;
        }

        else if (rate > 500) {

            confidence -= 0.08;
        }

        else if (rate > 200) {

            confidence -= 0.04;
        }


        // ======================================
        // 5. VOLTAGE QUALITY
        // ======================================

        /*
         * Voltage is supplied by the edge device.
         *
         * Normal laboratory supply voltage gives
         * stronger confidence in the signature.
         */

        if (voltage >= 220 && voltage <= 250) {

            confidence += 0.04;

        } else if (
                voltage >= 200 &&
                voltage <= 260
        ) {

            confidence += 0.01;

        } else if (voltage > 0) {

            confidence -= 0.06;
        }


        // ======================================
        // 6. FEATURE AVAILABILITY
        // ======================================

        /*
         * If a real voltage/current signature is
         * available, confidence can be stronger.
         */

        if (
                voltage > 0 &&
                amps > 0
        ) {

            confidence += 0.02;
        }


        // ======================================
        // FINAL CONFIDENCE
        // ======================================

        return clampConfidence(
                confidence
        );
    }


    // ==========================================
    // EXPECTED CURRENT
    // ==========================================

    private double calculateExpectedCurrent(
            double watts,
            double voltage) {


        /*
         * For a simplified AC estimate:
         *
         * I ≈ P / V
         *
         * This is used only as a consistency
         * indicator, not as an exact electrical
         * model.
         */

        if (voltage <= 0) {
            return 0.0;
        }

        return watts / voltage;
    }


    // ==========================================
    // CONFIDENCE LIMIT
    // ==========================================

    private double clampConfidence(
            double confidence) {

        if (confidence < 0.0) {
            return 0.0;
        }

        if (confidence > 1.0) {
            return 1.0;
        }

        return confidence;
    }
}