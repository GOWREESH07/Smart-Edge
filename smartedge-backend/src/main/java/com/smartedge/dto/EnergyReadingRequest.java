package com.smartedge.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnergyReadingRequest {

    // ==========================================
    // BASIC ENERGY MEASUREMENTS
    // ==========================================

    private Double voltage;

    private Double current;

    private Double power;

    private Double energy;


    // ==========================================
    // ELECTRICAL PARAMETERS
    // ==========================================

    private Double frequency;

    private Double powerFactor;


    // ==========================================
    // TARIFF INFORMATION
    // ==========================================

    private Double tariffCost;


    // ==========================================
    // DEVICE INFORMATION
    // ==========================================

    private String deviceId;

    private Integer wifiStrength;

    private String firmwareVersion;


    // ==========================================
    // EDGE-DERIVED FEATURES
    // ==========================================

    /*
     * Change in power between consecutive
     * ESP32 readings.
     *
     * ΔP = Current Power - Previous Power
     */
    private Double deltaPower;


    /*
     * Change in current between consecutive
     * ESP32 readings.
     *
     * ΔI = Current Current - Previous Current
     */
    private Double deltaCurrent;


    /*
     * Change in power factor between consecutive
     * ESP32 readings.
     *
     * ΔPF = Current PF - Previous PF
     */
    private Double deltaPowerFactor;


    /*
     * Rate of change of power.
     *
     * Unit: W/s
     */
    private Double powerChangeRate;


    /*
     * Indicates whether the ESP32 had a
     * previous reading available.
     */
    private Boolean edgeFeatureReady;
}