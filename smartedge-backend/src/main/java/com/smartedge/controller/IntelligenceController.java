package com.smartedge.controller;

import com.smartedge.dto.IntelligenceResponse;
import com.smartedge.service.IntelligenceService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/readings")
@CrossOrigin(origins = "http://localhost:5173")
public class IntelligenceController {

    private final IntelligenceService intelligenceService;

    public IntelligenceController(
            IntelligenceService intelligenceService) {

        this.intelligenceService =
                intelligenceService;
    }

    // =====================================================
    // GET INTELLIGENCE
    // =====================================================

    @GetMapping("/intelligence")
    public IntelligenceResponse getIntelligence() {

        return intelligenceService
                .analyzeLatestReading();
    }

    // =====================================================
    // ENABLE SIMULATION
    // =====================================================

    @PostMapping("/intelligence/simulate/{mode}")
    public Map<String, String> simulate(
            @PathVariable String mode) {

        intelligenceService
                .setSimulationMode(mode);

        return Map.of(
                "message",
                "Simulation mode updated",

                "simulationMode",
                intelligenceService.getSimulationMode()
        );
    }

    // =====================================================
    // CHECK SIMULATION MODE
    // =====================================================

    @GetMapping("/intelligence/simulation")
    public Map<String, String> simulationStatus() {

        return Map.of(
                "simulationMode",
                intelligenceService.getSimulationMode()
        );
    }

    // =====================================================
    // DISABLE SIMULATION
    // =====================================================

    @PostMapping("/intelligence/simulate/off")
    public Map<String, String> disableSimulation() {

        intelligenceService
                .setSimulationMode("OFF");

        return Map.of(
                "message",
                "Simulation disabled",

                "simulationMode",
                "OFF"
        );
    }
}