package com.smartedge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "load_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadEvent {

    // ==========================================
    // PRIMARY KEY
    // ==========================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ==========================================
    // DEVICE INFORMATION
    // ==========================================

    @Column(name = "device_id")
    private String deviceId;


    // ==========================================
    // LOAD EVENT INFORMATION
    // ==========================================

    @Column(name = "event_type")
    private String eventType;


    @Column(name = "previous_power")
    private Double previousPower;


    @Column(name = "current_power")
    private Double currentPower;


    @Column(name = "power_change")
    private Double powerChange;


    // ==========================================
    // EDGE FEATURE SIGNATURE
    // ==========================================

    /*
     * These values are calculated at the ESP32
     * before transmission to the backend.
     *
     * They form the multi-feature signature
     * used for intelligent load classification.
     */

    @Column(name = "delta_current")
    private Double deltaCurrent;


    @Column(name = "delta_power_factor")
    private Double deltaPowerFactor;


    @Column(name = "power_change_rate")
    private Double powerChangeRate;


    @Column(name = "voltage")
    private Double voltage;


    // ==========================================
    // SMART LOAD CLASSIFICATION
    // ==========================================

    /*
     * Example values:
     *
     * SMALL_ELECTRONIC_LOAD
     * POSSIBLE_FAN
     * MEDIUM_APPLIANCE
     * LARGE_APPLIANCE
     * HIGH_POWER_APPLIANCE
     * HEAVY_LOAD
     */

    @Column(name = "appliance_type")
    private String applianceType;


    /*
     * Confidence is stored between:
     *
     * 0.0 -> 1.0
     *
     * Example:
     * 0.70 = 70% confidence
     */

    @Column(name = "confidence")
    private Double confidence;


    /*
     * Current value:
     *
     * RULE_BASED
     * MULTI_FEATURE_SIGNATURE
     * CONFIDENCE_ADAPTIVE_MULTI_FEATURE
     *
     * Later this can become:
     *
     * ML_MODEL
     */

    @Column(name = "classification_method")
    private String classificationMethod;


    // ==========================================
    // TIMESTAMP
    // ==========================================

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;
}