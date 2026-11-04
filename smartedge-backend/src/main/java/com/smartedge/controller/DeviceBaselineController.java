package com.smartedge.controller;

import com.smartedge.entity.DeviceBaseline;
import com.smartedge.service.DeviceBaselineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/baseline")
@CrossOrigin
public class DeviceBaselineController {

    private final DeviceBaselineService deviceBaselineService;

    public DeviceBaselineController(
            DeviceBaselineService deviceBaselineService) {

        this.deviceBaselineService =
                deviceBaselineService;
    }


    // =====================================================
    // GET DEVICE BASELINE
    // =====================================================

    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceBaseline> getBaseline(
            @PathVariable String deviceId) {

        DeviceBaseline baseline =
                deviceBaselineService.getBaseline(deviceId);

        if (baseline == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(baseline);
    }
}