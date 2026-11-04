package com.smartedge.repository;

import com.smartedge.entity.AnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalyEventRepository
        extends JpaRepository<AnomalyEvent, Long> {

    List<AnomalyEvent>
    findTop20ByOrderByDetectedAtDesc();
}