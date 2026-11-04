package com.smartedge.controller;

import com.smartedge.entity.AnomalyEvent;
import com.smartedge.service.AnomalyEventService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@CrossOrigin(origins = "http://localhost:5173")
public class AnomalyEventController {

    private final AnomalyEventService service;

    public AnomalyEventController(
            AnomalyEventService service) {

        this.service = service;
    }

    @GetMapping
    public List<AnomalyEvent> getAll() {

        return service.getAllAnomalies();
    }

    @GetMapping("/recent")
    public List<AnomalyEvent> getRecent() {

        return service.getRecentAnomalies();
    }

    @DeleteMapping
    public void clearAll() {

        service.clearAll();
    }
}