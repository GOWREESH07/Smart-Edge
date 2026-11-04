package com.smartedge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnergyForecastResponse {

    private Double currentAveragePower;

    private Double predictedPower;

    private Double powerDifference;

    private String trend;

    private Double confidence;

    private String predictionMethod;

    private String recommendation;
}