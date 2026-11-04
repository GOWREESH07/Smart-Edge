package com.smartedge.controller;

import com.smartedge.dto.DailyStatsResponse;
import com.smartedge.dto.EnergyForecastResponse;
import com.smartedge.dto.EnergyReadingRequest;
import com.smartedge.entity.EnergyReading;
import com.smartedge.service.EnergyForecastService;
import com.smartedge.service.EnergyReadingService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/readings")
@CrossOrigin(origins = "http://localhost:5173")
public class EnergyReadingController {

    private final EnergyReadingService service;

    private final EnergyForecastService
            energyForecastService;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public EnergyReadingController(
            EnergyReadingService service,
            EnergyForecastService energyForecastService) {

        this.service = service;

        this.energyForecastService =
                energyForecastService;
    }


    // ==========================================
    // POST SENSOR DATA FROM ESP32
    // ==========================================

    @PostMapping
    public EnergyReading saveReading(
            @RequestBody EnergyReadingRequest request) {

        EnergyReading reading =
                new EnergyReading();


        // ======================================
        // BASIC ENERGY MEASUREMENTS
        // ======================================

        reading.setVoltage(
                request.getVoltage());

        reading.setCurrent(
                request.getCurrent());

        reading.setPower(
                request.getPower());

        reading.setEnergy(
                request.getEnergy());


        // ======================================
        // ELECTRICAL PARAMETERS
        // ======================================

        reading.setFrequency(
                request.getFrequency());

        reading.setPowerFactor(
                request.getPowerFactor());


        // ======================================
        // TARIFF INFORMATION
        // ======================================

        reading.setTariffCost(
                request.getTariffCost());


        // ======================================
        // DEVICE INFORMATION
        // ======================================

        reading.setDeviceId(
                request.getDeviceId());

        reading.setWifiStrength(
                request.getWifiStrength());

        reading.setFirmwareVersion(
                request.getFirmwareVersion());


        // ======================================
        // EDGE-DERIVED FEATURES
        // ======================================

        reading.setDeltaPower(
                request.getDeltaPower());

        reading.setDeltaCurrent(
                request.getDeltaCurrent());

        reading.setDeltaPowerFactor(
                request.getDeltaPowerFactor());

        reading.setPowerChangeRate(
                request.getPowerChangeRate());

        reading.setEdgeFeatureReady(
                request.getEdgeFeatureReady());


        // ======================================
        // SAVE READING
        // ======================================

        return service.save(reading);
    }


    // ==========================================
    // GET ALL READINGS
    // ==========================================

    @GetMapping
    public List<EnergyReading> getAllReadings() {

        return service.getAll();
    }


    // ==========================================
    // GET LATEST READING
    // ==========================================

    @GetMapping("/latest")
    public EnergyReading getLatestReading() {

        return service.getLatest();
    }


    // ==========================================
    // GET RECENT READINGS
    // ==========================================

    @GetMapping("/recent")
    public List<EnergyReading> getRecentReadings() {

        return service.getRecent();
    }


    // ==========================================
    // TODAY'S ENERGY STATISTICS
    // ==========================================

    @GetMapping("/stats/today")
    public DailyStatsResponse getTodayStats() {

        return service.getTodayStats();
    }


    // ==========================================
    // SMARTEDGE ENERGY FORECAST
    // ==========================================

    @GetMapping("/forecast")
    public EnergyForecastResponse getEnergyForecast() {

        return energyForecastService
                .generateForecast();
    }
}