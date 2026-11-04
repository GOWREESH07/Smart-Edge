package com.smartedge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "energy_readings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnergyReading {

    // ==========================================
    // PRIMARY KEY
    // ==========================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ==========================================
    // BASIC ENERGY MEASUREMENTS
    // ==========================================

    private Double voltage;

    private Double current;

    private Double power;

    private Double energy;


    // ==========================================
    // DEVICE INFORMATION
    // ==========================================

    private String deviceId;

    private Integer wifiStrength;

    private String firmwareVersion;


    // ==========================================
    // ELECTRICAL PARAMETERS
    // ==========================================

    @Column(name = "frequency")
    private Double frequency;

    @Column(name = "power_factor")
    private Double powerFactor;


    // ==========================================
    // TARIFF INFORMATION
    // ==========================================

    @Column(name = "tariff_cost")
    private Double tariffCost;


    // ==========================================
    // EDGE-DERIVED FEATURES
    // ==========================================

    /*
     * Change in power between the current
     * and previous ESP32 reading.
     *
     * ΔP = Current Power - Previous Power
     */
    @Column(name = "delta_power")
    private Double deltaPower;


    /*
     * Change in current between the current
     * and previous ESP32 reading.
     *
     * ΔI = Current Current - Previous Current
     */
    @Column(name = "delta_current")
    private Double deltaCurrent;


    /*
     * Change in power factor between the
     * current and previous ESP32 reading.
     *
     * ΔPF = Current PF - Previous PF
     */
    @Column(name = "delta_power_factor")
    private Double deltaPowerFactor;


    /*
     * Rate at which power is changing.
     *
     * RateP = ΔP / Δt
     *
     * Unit: W/s
     */
    @Column(name = "power_change_rate")
    private Double powerChangeRate;


    /*
     * Indicates whether the ESP32 had a
     * previous reading available when
     * calculating the edge features.
     */
    @Column(name = "edge_feature_ready")
    private Boolean edgeFeatureReady;


    // ==========================================
    // TIMESTAMP
    // ==========================================

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}