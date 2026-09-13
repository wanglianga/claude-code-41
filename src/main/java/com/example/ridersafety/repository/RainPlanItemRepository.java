package com.example.ridersafety.repository;

import com.example.ridersafety.model.RainPlanItem;
import com.example.ridersafety.model.RainPlanItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RainPlanItemRepository extends JpaRepository<RainPlanItem, Long> {
    List<RainPlanItem> findByPlan_Id(Long planId);

    List<RainPlanItem> findByPlan_IdAndStatus(Long planId, RainPlanItemStatus status);

    List<RainPlanItem> findByRider_IdAndStatus(Long riderId, RainPlanItemStatus status);
}
