package com.smartedge.service;

import com.smartedge.entity.DeviceBaseline;
import com.smartedge.entity.EnergyReading;
import com.smartedge.model.DeviationResult;
import com.smartedge.model.SeverityPersistence;

import org.springframework.stereotype.Service;

@Service
public class NormalizedDeviationService {

    private final DeviceBaselineService deviceBaselineService;
    private final SeverityPersistenceService severityPersistenceService;

    public NormalizedDeviationService(
        DeviceBaselineService deviceBaselineService,
        SeverityPersistenceService severityPersistenceService) {

    this.deviceBaselineService =
            deviceBaselineService;

    this.severityPersistenceService =
            severityPersistenceService;
}

    // =========================================================
    // MAIN DEVIATION CALCULATION
    // =========================================================

    public DeviationResult calculateDeviation(EnergyReading reading) {

        DeviationResult result = new DeviationResult();

        // -----------------------------------------------------
        // No reading available
        // -----------------------------------------------------

        if (reading == null) {

            result.setDeviceId(null);
            result.setDeviationLevel("UNKNOWN");
            result.setMostDeviantParameter("NONE");
            result.setOverallDeviationScore(0.0);

            return result;
        }

        String deviceId = reading.getDeviceId();

        result.setDeviceId(deviceId);

        // -----------------------------------------------------
        // Retrieve device-specific learned baseline
        // -----------------------------------------------------

        DeviceBaseline baseline =
                deviceBaselineService.getBaseline(deviceId);

        /*
         * Require enough historical samples before using
         * the learned baseline.
         */

        if (baseline == null
                || baseline.getSampleCount() == null
                || baseline.getSampleCount() < 10) {

            result.setDeviationLevel("INSUFFICIENT_DATA");
            result.setMostDeviantParameter("NONE");
            result.setOverallDeviationScore(0.0);

            return result;
        }

        long sampleCount =
                baseline.getSampleCount();

        // =====================================================
        // 1. NORMALIZED DEVIATION
        // =====================================================

        double voltageDeviation =
                calculateZScore(
                        reading.getVoltage(),
                        baseline.getVoltageMean(),
                        baseline.getVoltageM2(),
                        sampleCount
                );

        double currentDeviation =
                calculateZScore(
                        reading.getCurrent(),
                        baseline.getCurrentMean(),
                        baseline.getCurrentM2(),
                        sampleCount
                );

        double powerDeviation =
                calculateZScore(
                        reading.getPower(),
                        baseline.getPowerMean(),
                        baseline.getPowerM2(),
                        sampleCount
                );

        double frequencyDeviation =
                calculateZScore(
                        reading.getFrequency(),
                        baseline.getFrequencyMean(),
                        baseline.getFrequencyM2(),
                        sampleCount
                );

        double powerFactorDeviation =
                calculateZScore(
                        reading.getPowerFactor(),
                        baseline.getPowerFactorMean(),
                        baseline.getPowerFactorM2(),
                        sampleCount
                );

        // -----------------------------------------------------
        // Store normalized deviations
        // -----------------------------------------------------

        result.setVoltageDeviation(
                round(voltageDeviation)
        );

        result.setCurrentDeviation(
                round(currentDeviation)
        );

        result.setPowerDeviation(
                round(powerDeviation)
        );

        result.setFrequencyDeviation(
                round(frequencyDeviation)
        );

        result.setPowerFactorDeviation(
                round(powerFactorDeviation)
        );

        // =====================================================
        // 2. ADAPTIVE PARAMETER WEIGHTS
        // =====================================================

        double[] weights =
                calculateAdaptiveWeights(baseline);

        double voltageWeight = weights[0];
        double currentWeight = weights[1];
        double powerWeight = weights[2];
        double frequencyWeight = weights[3];
        double powerFactorWeight = weights[4];

        // -----------------------------------------------------
        // Store adaptive weights
        // -----------------------------------------------------

        result.setVoltageWeight(
                round(voltageWeight)
        );

        result.setCurrentWeight(
                round(currentWeight)
        );

        result.setPowerWeight(
                round(powerWeight)
        );

        result.setFrequencyWeight(
                round(frequencyWeight)
        );

        result.setPowerFactorWeight(
                round(powerFactorWeight)
        );

        // =====================================================
        // 3. FUSED ANOMALY SCORE
        // =====================================================

        double overallScore =
                (voltageDeviation * voltageWeight)
                        + (currentDeviation * currentWeight)
                        + (powerDeviation * powerWeight)
                        + (frequencyDeviation * frequencyWeight)
                        + (powerFactorDeviation * powerFactorWeight);

        result.setOverallDeviationScore(
                round(overallScore)
        );

        // =====================================================
        // 4. DEVIATION LEVEL
        // =====================================================

        result.setDeviationLevel(
                determineDeviationLevel(overallScore)
        );
        SeverityPersistence persistence =
        severityPersistenceService.process(result);

result.setConsecutiveAnomalies(
        persistence.getConsecutiveAnomalies()
);

result.setConsecutiveNormalReadings(
        persistence.getConsecutiveNormalReadings()
);

result.setPersistent(
        persistence.isPersistent()
);

result.setAnomalyDurationSeconds(
        persistence.getDurationSeconds()
);

result.setEffectiveSeverity(
        persistence.getCurrentSeverity()
);
        // =====================================================
        // 5. MOST DEVIANT PARAMETER
        // =====================================================

        result.setMostDeviantParameter(
                findMostDeviantParameter(
                        voltageDeviation,
                        currentDeviation,
                        powerDeviation,
                        frequencyDeviation,
                        powerFactorDeviation
                )
        );

        return result;
    }

    // =========================================================
    // Z-SCORE CALCULATION
    // =========================================================

    private double calculateZScore(
            Double value,
            Double mean,
            Double m2,
            long sampleCount) {

        /*
         * Validate all required values.
         */

        if (value == null
                || mean == null
                || m2 == null
                || sampleCount < 2) {

            return 0.0;
        }

        /*
         * DeviceBaseline stores Welford's M2.
         *
         * Sample variance:
         *
         * variance = M2 / (n - 1)
         */

        double variance =
                m2 / (sampleCount - 1);

        /*
         * Avoid division by zero when the historical
         * parameter has practically no variation.
         */

        if (variance <= 0.0000001) {

            return 0.0;
        }

        double standardDeviation =
                Math.sqrt(variance);

        /*
         * Absolute Z-score.
         *
         * Both higher-than-normal and lower-than-normal
         * values are treated as deviations.
         */

        return Math.abs(
                (value - mean) / standardDeviation
        );
    }

    // =========================================================
    // ADAPTIVE WEIGHT CALCULATION
    // =========================================================

    private double[] calculateAdaptiveWeights(
            DeviceBaseline baseline) {

        /*
         * -----------------------------------------------------
         * BASE DOMAIN WEIGHTS
         * -----------------------------------------------------
         *
         * These represent the initial importance of the
         * electrical parameters.
         *
         * Total = 1.00
         */

        double baseVoltage = 0.25;
        double baseCurrent = 0.15;
        double basePower = 0.30;
        double baseFrequency = 0.10;
        double basePowerFactor = 0.20;

        // -----------------------------------------------------
        // Calculate learned parameter stability
        // -----------------------------------------------------

        double voltageStability =
                calculateStability(
                        baseline.getVoltageMean(),
                        baseline.getVoltageM2(),
                        baseline.getSampleCount()
                );

        double currentStability =
                calculateStability(
                        baseline.getCurrentMean(),
                        baseline.getCurrentM2(),
                        baseline.getSampleCount()
                );

        double powerStability =
                calculateStability(
                        baseline.getPowerMean(),
                        baseline.getPowerM2(),
                        baseline.getSampleCount()
                );

        double frequencyStability =
                calculateStability(
                        baseline.getFrequencyMean(),
                        baseline.getFrequencyM2(),
                        baseline.getSampleCount()
                );

        double powerFactorStability =
                calculateStability(
                        baseline.getPowerFactorMean(),
                        baseline.getPowerFactorM2(),
                        baseline.getSampleCount()
                );

        // -----------------------------------------------------
        // Convert stability into adaptive scores
        // -----------------------------------------------------

        /*
         * Square-root transformation provides moderate
         * adaptation without allowing stability to completely
         * dominate the original domain importance.
         */

        double voltageScore =
                baseVoltage *
                        Math.sqrt(voltageStability);

        double currentScore =
                baseCurrent *
                        Math.sqrt(currentStability);

        double powerScore =
                basePower *
                        Math.sqrt(powerStability);

        double frequencyScore =
                baseFrequency *
                        Math.sqrt(frequencyStability);

        double powerFactorScore =
                basePowerFactor *
                        Math.sqrt(powerFactorStability);

        // -----------------------------------------------------
        // Normalize the adaptive scores
        // -----------------------------------------------------

        /*
         * Final weights must sum to 1.0.
         */

        double total =
                voltageScore
                        + currentScore
                        + powerScore
                        + frequencyScore
                        + powerFactorScore;

        /*
         * Safety fallback.
         */

        if (total <= 0.000001) {

            return new double[]{
                    baseVoltage,
                    baseCurrent,
                    basePower,
                    baseFrequency,
                    basePowerFactor
            };
        }

        return new double[]{
                voltageScore / total,
                currentScore / total,
                powerScore / total,
                frequencyScore / total,
                powerFactorScore / total
        };
    }

    // =========================================================
    // PARAMETER STABILITY
    // =========================================================

    private double calculateStability(
            Double mean,
            Double m2,
            Long sampleCount) {

        /*
         * Not enough historical data.
         */

        if (mean == null
                || m2 == null
                || sampleCount == null
                || sampleCount < 2) {

            return 1.0;
        }

        // -----------------------------------------------------
        // Calculate variance
        // -----------------------------------------------------

        double variance =
                m2 / (sampleCount - 1);

        if (variance < 0.0) {

            variance = 0.0;
        }

        // -----------------------------------------------------
        // Calculate standard deviation
        // -----------------------------------------------------

        double standardDeviation =
                Math.sqrt(variance);

        // -----------------------------------------------------
        // Calculate coefficient of variation
        // -----------------------------------------------------

        /*
         * CV = Standard Deviation / |Mean|
         *
         * Lower CV means the parameter is more stable.
         */

        double denominator =
                Math.max(
                        Math.abs(mean),
                        0.000001
                );

        double coefficientOfVariation =
                standardDeviation /
                        denominator;

        // -----------------------------------------------------
        // Convert CV to stability
        // -----------------------------------------------------

        /*
         * CV = 0
         *     -> stability = 1
         *
         * Higher CV
         *     -> lower stability
         */

        double stability =
                1.0 /
                        (1.0 + coefficientOfVariation);

        /*
         * Prevent extremely unstable parameters from
         * becoming completely irrelevant.
         */

        return Math.max(
                0.25,
                Math.min(
                        1.0,
                        stability
                )
        );
    }

    // =========================================================
    // DEVIATION LEVEL
    // =========================================================

    private String determineDeviationLevel(
            double score) {

        if (score < 1.5) {

            return "NORMAL";
        }

        if (score < 2.5) {

            return "MILD";
        }

        if (score < 3.5) {

            return "MODERATE";
        }

        if (score < 5.0) {

            return "HIGH";
        }

        return "CRITICAL";
    }

    // =========================================================
    // MOST DEVIANT PARAMETER
    // =========================================================

    private String findMostDeviantParameter(
            double voltage,
            double current,
            double power,
            double frequency,
            double powerFactor) {

        double maximum = voltage;

        String parameter = "VOLTAGE";

        if (current > maximum) {

            maximum = current;
            parameter = "CURRENT";
        }

        if (power > maximum) {

            maximum = power;
            parameter = "POWER";
        }

        if (frequency > maximum) {

            maximum = frequency;
            parameter = "FREQUENCY";
        }

        if (powerFactor > maximum) {

            maximum = powerFactor;
            parameter = "POWER_FACTOR";
        }

        return parameter;
    }

    // =========================================================
    // ROUNDING
    // =========================================================

    private double round(double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}