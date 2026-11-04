package com.smartedge.service;

import com.smartedge.dto.EnergyForecastResponse;
import com.smartedge.entity.EnergyReading;
import com.smartedge.repository.EnergyReadingRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class EnergyForecastService {

    private final EnergyReadingRepository repository;

    private static final int WINDOW_SIZE = 10;

    private static final double MAX_VALID_POWER = 5000.0;


    public EnergyForecastService(
            EnergyReadingRepository repository) {

        this.repository = repository;
    }


    // ==========================================
    // GENERATE ENERGY FORECAST
    // ==========================================

    public EnergyForecastResponse generateForecast() {

        List<EnergyReading> readings =
                repository.findTop20ByOrderByIdDesc();


        if (readings == null ||
                readings.isEmpty()) {

            return emptyForecast();
        }


        // Repository returns newest first.
        // Reverse so calculations run oldest -> newest.
        List<EnergyReading> orderedReadings =
                new ArrayList<>(readings);

        Collections.reverse(orderedReadings);


        // ======================================
        // FILTER INVALID SENSOR VALUES
        // ======================================

        List<EnergyReading> validReadings =
                orderedReadings.stream()

                        .filter(reading ->
                                reading.getPower() != null
                        )

                        .filter(reading ->
                                reading.getPower() >= 0
                        )

                        .filter(reading ->
                                reading.getPower()
                                        <= MAX_VALID_POWER
                        )

                        .toList();


        if (validReadings.size() < 4) {

            return insufficientDataForecast(
                    validReadings
            );
        }


        // ======================================
        // SELECT RECENT WINDOW
        // ======================================

        int startIndex =
                Math.max(
                        0,
                        validReadings.size()
                                - WINDOW_SIZE
                );


        List<EnergyReading> window =
                validReadings.subList(
                        startIndex,
                        validReadings.size()
                );


        // ======================================
        // CURRENT AVERAGE POWER
        // ======================================

        double currentAveragePower =
                window.stream()

                        .mapToDouble(
                                EnergyReading::getPower
                        )

                        .average()

                        .orElse(0.0);


        // ======================================
        // LINEAR TREND CALCULATION
        // ======================================
        //
        // x = reading position
        // y = power
        //
        // We estimate the slope of recent
        // power consumption.
        //
        // ======================================

        int n = window.size();

        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumXSquare = 0.0;


        for (int i = 0; i < n; i++) {

            double x = i;

            double y =
                    window
                            .get(i)
                            .getPower();


            sumX += x;

            sumY += y;

            sumXY += x * y;

            sumXSquare += x * x;
        }


        double denominator =
                (n * sumXSquare)
                        - (sumX * sumX);


        double slope = 0.0;


        if (denominator != 0) {

            slope =
                    (
                            (n * sumXY)
                                    -
                            (sumX * sumY)
                    )
                    / denominator;
        }


        // ======================================
        // PREDICT NEXT POWER VALUE
        // ======================================

        double meanX =
                sumX / n;

        double meanY =
                sumY / n;


        double intercept =
                meanY
                        - (slope * meanX);


        double nextX = n;


        double predictedPower =
                intercept
                        + (slope * nextX);


        // Power cannot physically be negative
        predictedPower =
                Math.max(
                        0.0,
                        predictedPower
                );


        // Protect against absurd predictions
        predictedPower =
                Math.min(
                        predictedPower,
                        MAX_VALID_POWER
                );


        // ======================================
        // POWER DIFFERENCE
        // ======================================

        double powerDifference =
                predictedPower
                        - currentAveragePower;


        // ======================================
        // TREND
        // ======================================

        String trend;


        double trendThreshold =
                Math.max(
                        1.0,
                        currentAveragePower * 0.05
                );


        if (powerDifference >
                trendThreshold) {

            trend = "INCREASING";

        } else if (powerDifference <
                -trendThreshold) {

            trend = "DECREASING";

        } else {

            trend = "STABLE";
        }


        // ======================================
        // CONFIDENCE
        // ======================================

        double confidence =
                calculateConfidence(
                        window,
                        slope,
                        intercept
                );


        // ======================================
        // RECOMMENDATION
        // ======================================

        String recommendation =
                buildRecommendation(
                        trend,
                        predictedPower
                );


        // ======================================
        // RETURN FORECAST
        // ======================================

        return new EnergyForecastResponse(

                round(
                        currentAveragePower,
                        2
                ),

                round(
                        predictedPower,
                        2
                ),

                round(
                        powerDifference,
                        2
                ),

                trend,

                round(
                        confidence,
                        2
                ),

                "LINEAR_TREND",

                recommendation
        );
    }


    // ==========================================
    // CALCULATE MODEL CONFIDENCE
    // ==========================================

    private double calculateConfidence(
            List<EnergyReading> readings,
            double slope,
            double intercept) {


        if (readings.size() < 2) {
            return 0.0;
        }


        double mean =
                readings.stream()

                        .mapToDouble(
                                EnergyReading::getPower
                        )

                        .average()

                        .orElse(0.0);


        double totalVariation = 0.0;

        double errorVariation = 0.0;


        for (int i = 0;
             i < readings.size();
             i++) {


            double actual =
                    readings
                            .get(i)
                            .getPower();


            double predicted =
                    intercept
                            + (slope * i);


            totalVariation +=
                    Math.pow(
                            actual - mean,
                            2
                    );


            errorVariation +=
                    Math.pow(
                            actual - predicted,
                            2
                    );
        }


        /*
         * If readings are almost perfectly
         * constant, the forecast is stable.
         */
        if (totalVariation < 0.000001) {

            return 0.95;
        }


        double rSquared =
                1.0
                        -
                (
                        errorVariation
                                /
                        totalVariation
                );


        /*
         * Keep confidence between 0 and 1.
         */
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        rSquared
                )
        );
    }


    // ==========================================
    // BUILD RECOMMENDATION
    // ==========================================

    private String buildRecommendation(
            String trend,
            double predictedPower) {


        if ("INCREASING".equals(trend)) {

            return "Power consumption is trending upward. Monitor recently activated loads.";
        }


        if ("DECREASING".equals(trend)) {

            return "Power consumption is trending downward. One or more loads may have been reduced or switched off.";
        }


        if (predictedPower < 1.0) {

            return "Power demand is currently very low or the monitored load may be switched off.";
        }


        return "Power consumption is currently stable with no significant short-term change predicted.";
    }


    // ==========================================
    // INSUFFICIENT DATA RESPONSE
    // ==========================================

    private EnergyForecastResponse
    insufficientDataForecast(
            List<EnergyReading> readings) {


        double averagePower =
                readings.stream()

                        .mapToDouble(
                                EnergyReading::getPower
                        )

                        .average()

                        .orElse(0.0);


        return new EnergyForecastResponse(

                round(
                        averagePower,
                        2
                ),

                round(
                        averagePower,
                        2
                ),

                0.0,

                "INSUFFICIENT_DATA",

                0.0,

                "WAITING_FOR_DATA",

                "More valid SmartEdge sensor readings are required before a forecast can be generated."
        );
    }


    // ==========================================
    // EMPTY FORECAST
    // ==========================================

    private EnergyForecastResponse
    emptyForecast() {

        return new EnergyForecastResponse(

                0.0,

                0.0,

                0.0,

                "NO_DATA",

                0.0,

                "WAITING_FOR_DATA",

                "No SmartEdge energy readings are currently available for forecasting."
        );
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