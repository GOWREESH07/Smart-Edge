package com.smartedge.service;

import com.smartedge.entity.AnomalyEvent;
import com.smartedge.repository.AnomalyEventRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnomalyEventService {

    private final AnomalyEventRepository repository;

    public AnomalyEventService(
            AnomalyEventRepository repository) {

        this.repository = repository;
    }

    public AnomalyEvent saveAnomaly(
            String deviceId,
            String anomalyType,
            String severity,
            String parameterName,
            Double measuredValue,
            String unit,
            String message) {

        AnomalyEvent event = new AnomalyEvent();

        event.setDeviceId(deviceId);
        event.setAnomalyType(anomalyType);
        event.setSeverity(severity);
        event.setParameterName(parameterName);
        event.setMeasuredValue(measuredValue);
        event.setUnit(unit);
        event.setMessage(message);
        event.setDetectedAt(LocalDateTime.now());

        return repository.save(event);
    }

    public List<AnomalyEvent> getRecentAnomalies() {

        return repository
                .findTop20ByOrderByDetectedAtDesc();
    }

    public List<AnomalyEvent> getAllAnomalies() {

        return repository.findAll();
    }

    public void clearAll() {

        repository.deleteAll();
    }
}