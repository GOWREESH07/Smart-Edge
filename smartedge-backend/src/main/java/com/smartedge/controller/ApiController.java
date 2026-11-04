package com.smartedge.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/health")
    public Map<String, String> health() {

        return Map.of(
                "status", "ONLINE",
                "service", "SmartEdge Backend",
                "version", "1.0.0"
        );

    }

}