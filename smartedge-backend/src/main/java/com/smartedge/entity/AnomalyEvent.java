package com.smartedge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEvent {

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
    // ANOMALY INFORMATION
    // ==========================================

    /*
     * Example values:
     *
     * LOW_VOLTAGE
     * HIGH_VOLTAGE
     * HIGH_CURRENT
     * HIGH_POWER
     * BAD_FREQUENCY
     * LOW_POWER_FACTOR
     * HIGH_POWER_FACTOR
     */
    @Column(name = "anomaly_type")
    private String anomalyType;


    /*
     * Severity of the detected anomaly.
     *
     * Example:
     *
     * NORMAL
     * MILD
     * MODERATE
     * HIGH
     * CRITICAL
     */
    @Column(name = "severity")
    private String severity;


    // ==========================================
    // PARAMETER INFORMATION
    // ==========================================

    /*
     * Parameter responsible for the anomaly.
     *
     * Example:
     *
     * VOLTAGE
     * CURRENT
     * POWER
     * FREQUENCY
     * POWER_FACTOR
     */
    @Column(name = "parameter_name")
    private String parameterName;


    /*
     * Actual measured value when
     * the anomaly was detected.
     */
    @Column(name = "measured_value")
    private Double measuredValue;


    /*
     * Unit of the measured value.
     *
     * Example:
     *
     * V
     * A
     * W
     * Hz
     */
    @Column(name = "unit")
    private String unit;


    // ==========================================
    // ANOMALY CONFIDENCE
    // ==========================================

    /*
     * Confidence of the anomaly detection.
     *
     * Range:
     *
     * 0.0 → 1.0
     *
     * Example:
     *
     * 0.90 = 90% confidence
     *
     * This value is used by the
     * Confidence-Weighted Event Chain Engine.
     */
    @Column(name = "confidence")
    private Double confidence;


    // ==========================================
    // MESSAGE
    // ==========================================

    /*
     * Human-readable explanation
     * of the detected anomaly.
     */
    @Column(name = "message")
    private String message;


    // ==========================================
    // TIMESTAMP
    // ==========================================

    /*
     * Time at which the anomaly
     * was detected.
     */
    @Column(name = "detected_at")
    private LocalDateTime detectedAt;
}