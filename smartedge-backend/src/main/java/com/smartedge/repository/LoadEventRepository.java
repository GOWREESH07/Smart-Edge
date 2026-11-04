package com.smartedge.repository;

import com.smartedge.entity.LoadEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoadEventRepository
        extends JpaRepository<LoadEvent, Long> {

    List<LoadEvent> findTop20ByOrderByDetectedAtDesc();
}