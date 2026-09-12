package com.example.ridersafety.repository;

import com.example.ridersafety.model.PolicyAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyAdjustmentRepository extends JpaRepository<PolicyAdjustment, Long> {
    List<PolicyAdjustment> findByStation_IdOrderByCreatedAtDesc(Long stationId);
}
