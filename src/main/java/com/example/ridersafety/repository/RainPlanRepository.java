package com.example.ridersafety.repository;

import com.example.ridersafety.model.RainPlan;
import com.example.ridersafety.model.RainPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RainPlanRepository extends JpaRepository<RainPlan, Long> {
    List<RainPlan> findByStation_IdOrderByCreatedAtDesc(Long stationId);

    boolean existsByStation_IdAndStatusIn(Long stationId, Collection<RainPlanStatus> statuses);
}
