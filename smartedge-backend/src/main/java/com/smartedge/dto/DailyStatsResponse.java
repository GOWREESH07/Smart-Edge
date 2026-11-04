package com.smartedge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DailyStatsResponse {

    private Double energyUsed;
    private Double averagePower;
    private Double peakPower;
    private Double averageVoltage;
    private Double averageCurrent;
    private Double estimatedCost;
    private Long totalReadings;
}