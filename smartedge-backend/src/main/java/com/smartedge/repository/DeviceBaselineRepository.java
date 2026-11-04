package com.smartedge.repository;

import com.smartedge.entity.DeviceBaseline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceBaselineRepository
        extends JpaRepository<DeviceBaseline, Long> {

    Optional<DeviceBaseline> findByDeviceId(String deviceId);
}