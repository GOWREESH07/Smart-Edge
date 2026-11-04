package com.smartedge.service;

import com.smartedge.dto.DailyStatsResponse;
import com.smartedge.entity.EnergyReading;
import com.smartedge.repository.EnergyReadingRepository;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class EnergyReadingService {

    private final EnergyReadingRepository repository;
    private final LoadDetectionService loadDetectionService;
    private final DeviceBaselineService deviceBaselineService;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public EnergyReadingService(
            EnergyReadingRepository repository,
            LoadDetectionService loadDetectionService,DeviceBaselineService deviceBaselineService) {

        this.repository = repository;
        this.loadDetectionService = loadDetectionService;
        this.deviceBaselineService = deviceBaselineService;
    }


    // ==========================================
    // SAVE ESP32 READING
    // ==========================================

    public EnergyReading save(EnergyReading reading) {

        /*
         * Get the previous reading BEFORE
         * saving the new reading.
         *
         * This allows SmartEdge to compare:
         *
         * previous power -> current power
         */

        EnergyReading previous =
                getLatest();


        // Set current timestamp
        reading.setCreatedAt(
                LocalDateTime.now()
        );


        // Save the new sensor reading
        EnergyReading savedReading =
                repository.save(reading);
        // ======================================
// DEVICE BASELINE LEARNING
// ======================================

deviceBaselineService.updateBaseline(
        savedReading
);


        // ======================================
        // LOAD CHANGE DETECTION
        // ======================================

        /*
         * Compare the previous reading with
         * the newly saved reading.
         *
         * Example:
         *
         * 15W -> 75W
         * Difference = +60W
         * LOAD_ON
         *
         * 75W -> 15W
         * Difference = -60W
         * LOAD_OFF
         */

        loadDetectionService.detectLoadChange(
                previous,
                savedReading
        );


        return savedReading;
    }


    // ==========================================
    // GET ALL READINGS
    // ==========================================

    public List<EnergyReading> getAll() {

        return repository.findAll();
    }


    // ==========================================
    // GET LATEST READING
    // ==========================================

    public EnergyReading getLatest() {

        return repository
                .findAll()
                .stream()
                .max(
                        (a, b) ->
                                Long.compare(
                                        a.getId(),
                                        b.getId()
                                )
                )
                .orElse(null);
    }


    // ==========================================
    // GET RECENT 20 READINGS
    // ==========================================

    public List<EnergyReading> getRecent() {

        List<EnergyReading> readings =
                repository
                        .findTop20ByOrderByIdDesc();


        /*
         * Repository returns newest first.
         *
         * Reverse it so charts receive:
         *
         * oldest -> newest
         */

        Collections.reverse(readings);


        return readings;
    }


    // ==========================================
    // TODAY'S STATISTICS
    // ==========================================

    public DailyStatsResponse getTodayStats() {

        LocalDate today =
                LocalDate.now();


        LocalDateTime start =
                today.atStartOfDay();


        LocalDateTime end =
                today
                        .plusDays(1)
                        .atStartOfDay();


        // ======================================
        // GET TODAY'S READINGS
        // ======================================

        List<EnergyReading> readings =
                repository
                        .findByCreatedAtBetweenOrderByCreatedAtAsc(
                                start,
                                end
                        );


        // ======================================
        // FILTER CORRUPTED READINGS
        // ======================================

        List<EnergyReading> validReadings =
                readings.stream()


                        // Required fields
                        .filter(r ->
                                r.getVoltage() != null &&
                                r.getCurrent() != null &&
                                r.getPower() != null &&
                                r.getEnergy() != null &&
                                r.getCreatedAt() != null
                        )


                        // ==================================
                        // VOLTAGE SANITY CHECK
                        // ==================================

                        .filter(r ->
                                r.getVoltage() >= 180 &&
                                r.getVoltage() <= 270
                        )


                        // ==================================
                        // CURRENT SANITY CHECK
                        // ==================================

                        .filter(r ->
                                r.getCurrent() >= 0 &&
                                r.getCurrent() <= 20
                        )


                        // ==================================
                        // POWER SANITY CHECK
                        // ==================================

                        .filter(r ->
                                r.getPower() >= 0 &&
                                r.getPower() <= 5000
                        )


                        // ==================================
                        // ENERGY CANNOT BE NEGATIVE
                        // ==================================

                        .filter(r ->
                                r.getEnergy() >= 0
                        )


                        .toList();


        // ======================================
        // NO VALID DATA
        // ======================================

        if (validReadings.isEmpty()) {

            return new DailyStatsResponse(

                    0.0,   // Energy used

                    0.0,   // Average power

                    0.0,   // Peak power

                    0.0,   // Average voltage

                    0.0,   // Average current

                    0.0,   // Estimated cost

                    0L      // Total valid readings
            );
        }


        // ======================================
        // BASIC STATISTICS
        // ======================================

        double totalPower =
                0.0;

        double totalVoltage =
                0.0;

        double totalCurrent =
                0.0;

        double peakPower =
                0.0;


        for (EnergyReading reading :
                validReadings) {


            double power =
                    reading.getPower();


            double voltage =
                    reading.getVoltage();


            double current =
                    reading.getCurrent();


            totalPower +=
                    power;


            totalVoltage +=
                    voltage;


            totalCurrent +=
                    current;


            peakPower =
                    Math.max(
                            peakPower,
                            power
                    );
        }


        // ======================================
        // NUMBER OF VALID READINGS
        // ======================================

        long count =
                validReadings.size();


        // ======================================
        // AVERAGES
        // ======================================

        double averagePower =
                totalPower / count;


        double averageVoltage =
                totalVoltage / count;


        double averageCurrent =
                totalCurrent / count;


        // ======================================
        // CALCULATE ENERGY FROM POWER + TIME
        // ======================================
        //
        // Energy (kWh)
        //
        // Power(W) x Time(hours)
        // ----------------------
        //          1000
        //
        //
        // We use the average power between
        // two consecutive readings.
        //
        // This is essentially trapezoidal
        // integration.
        //
        // ======================================

        double energyUsed =
                0.0;


        for (int i = 1;
             i < validReadings.size();
             i++) {


            EnergyReading previous =
                    validReadings.get(
                            i - 1
                    );


            EnergyReading current =
                    validReadings.get(
                            i
                    );


            // ==================================
            // TIME DIFFERENCE
            // ==================================

            long seconds =
                    Duration.between(

                            previous
                                    .getCreatedAt(),

                            current
                                    .getCreatedAt()

                    ).getSeconds();


            // ==================================
            // INVALID TIMESTAMP
            // ==================================

            if (seconds <= 0) {

                continue;
            }


            // ==================================
            // IGNORE LARGE DATA GAPS
            // ==================================
            //
            // ESP32 normally sends readings
            // every few seconds.
            //
            // If the ESP32/backend is offline
            // for a long period, we must NOT
            // assume the previous power
            // continued during the entire gap.
            //
            // ==================================

            if (seconds > 60) {

                continue;
            }


            // ==================================
            // POWER VALUES
            // ==================================

            double previousPower =
                    previous
                            .getPower();


            double currentPower =
                    current
                            .getPower();


            // ==================================
            // AVERAGE POWER DURING INTERVAL
            // ==================================

            double averageIntervalPower =
                    (
                            previousPower +
                            currentPower
                    ) / 2.0;


            // ==================================
            // CONVERT SECONDS TO HOURS
            // ==================================

            double hours =
                    seconds / 3600.0;


            // ==================================
            // CALCULATE INTERVAL ENERGY
            // ==================================
            //
            // W x hours = Wh
            //
            // Wh / 1000 = kWh
            //
            // ==================================

            double intervalEnergy =
                    (
                            averageIntervalPower *
                            hours
                    ) / 1000.0;


            // Add to today's total
            energyUsed +=
                    intervalEnergy;
        }


        // ======================================
        // ELECTRICITY TARIFF
        // ======================================

        /*
         * Current SmartEdge demo tariff:
         *
         * Rs. 8 per kWh
         *
         * Later we can move this into:
         *
         * application.properties
         *
         * or create configurable tariff plans.
         */

        double tariffPerKwh =
                8.0;


        // ======================================
        // ESTIMATED COST
        // ======================================

        double estimatedCost =
                energyUsed *
                tariffPerKwh;


        // ======================================
        // ROUND VALUES
        // ======================================

        energyUsed =
                round(
                        energyUsed,
                        4
                );


        averagePower =
                round(
                        averagePower,
                        2
                );


        peakPower =
                round(
                        peakPower,
                        2
                );


        averageVoltage =
                round(
                        averageVoltage,
                        2
                );


        averageCurrent =
                round(
                        averageCurrent,
                        3
                );


        estimatedCost =
                round(
                        estimatedCost,
                        2
                );


        // ======================================
        // RETURN DAILY STATISTICS
        // ======================================

        return new DailyStatsResponse(

                energyUsed,

                averagePower,

                peakPower,

                averageVoltage,

                averageCurrent,

                estimatedCost,

                count
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