package com.smartedge.repository;

import com.smartedge.entity.EnergyReading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnergyReadingRepository
        extends JpaRepository<EnergyReading, Long> {

    // Latest 20 readings for live charts
    List<EnergyReading> findTop20ByOrderByIdDesc();
    Optional<EnergyReading> findTopByDeviceIdOrderByIdDesc(String deviceId);

    // All readings between two timestamps
    List<EnergyReading> findByCreatedAtBetweenOrderByCreatedAtAsc(
            LocalDateTime start,
            LocalDateTime end
    );
}