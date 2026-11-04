package com.smartedge.controller;

import com.smartedge.entity.EnergyReading;
import com.smartedge.model.DeviationResult;
import com.smartedge.repository.EnergyReadingRepository;
import com.smartedge.service.NormalizedDeviationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/readings")
@CrossOrigin(origins = "*")
public class NormalizedDeviationController {

    private final EnergyReadingRepository energyReadingRepository;
    private final NormalizedDeviationService normalizedDeviationService;

    public NormalizedDeviationController(
            EnergyReadingRepository energyReadingRepository,
            NormalizedDeviationService normalizedDeviationService) {

        this.energyReadingRepository = energyReadingRepository;
        this.normalizedDeviationService = normalizedDeviationService;
    }

    @GetMapping("/deviation/{deviceId}")
    public DeviationResult getDeviation(
            @PathVariable String deviceId) {

        EnergyReading latest =
                energyReadingRepository
                        .findTopByDeviceIdOrderByIdDesc(deviceId)
                        .orElse(null);

        return normalizedDeviationService
                .calculateDeviation(latest);
    }
}