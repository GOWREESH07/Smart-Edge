package com.smartedge.controller;

import com.smartedge.entity.LoadEvent;
import com.smartedge.service.LoadDetectionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/load-events")
@CrossOrigin(origins = "http://localhost:5173")
public class LoadEventController {

    private final LoadDetectionService
            loadDetectionService;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public LoadEventController(
            LoadDetectionService loadDetectionService) {

        this.loadDetectionService =
                loadDetectionService;
    }


    // ==========================================
    // GET RECENT LOAD EVENTS
    // ==========================================

    @GetMapping("/recent")
    public List<LoadEvent> getRecentEvents() {

        return loadDetectionService
                .getRecentEvents();
    }


    // ==========================================
    // SIMULATE LOAD EVENT
    // ==========================================
    //
    // Examples:
    //
    // POST
    // /api/load-events/simulate/LED_ON
    //
    // POST
    // /api/load-events/simulate/LED_OFF
    //
    // ==========================================

    @PostMapping("/simulate/{mode}")
    public ResponseEntity<?> simulateLoadEvent(
            @PathVariable String mode) {

        try {

            LoadEvent event =
                    loadDetectionService
                            .simulateLedEvent(mode);


            return ResponseEntity.ok(
                    event
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            e.getMessage()
                    );
        }
    }
}