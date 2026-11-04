package com.smartedge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class IntelligenceResponse {

    private String systemHealth;

    private String voltageStatus;
    private String currentStatus;
    private String powerStatus;
    private String frequencyStatus;
    private String powerFactorStatus;
    private String wifiStatus;

    private int anomalyCount;

    private List<String> anomalies;

    private List<String> recommendations;
}