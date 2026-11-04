package com.smartedge.controller;

import com.smartedge.model.EventChainResult;
import com.smartedge.service.EventChainService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/event-chain")
@CrossOrigin(origins = "*")
public class EventChainController {

    private final EventChainService eventChainService;


    public EventChainController(
            EventChainService eventChainService) {

        this.eventChainService =
                eventChainService;
    }


    // =========================================================
    // ANALYZE EVENT CHAIN
    // =========================================================

    @GetMapping("/{deviceId}")
    public EventChainResult analyzeEventChain(
            @PathVariable String deviceId) {

        return eventChainService.analyzeDevice(
                deviceId
        );
    }
}