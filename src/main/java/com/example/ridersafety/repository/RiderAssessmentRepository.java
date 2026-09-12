package com.example.ridersafety.repository;

import com.example.ridersafety.model.RiderAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiderAssessmentRepository extends JpaRepository<RiderAssessment, Long> {
    List<RiderAssessment> findByRider_IdOrderByCreatedAtDesc(Long riderId);

    List<RiderAssessment> findByRider_Station_IdOrderByCreatedAtDesc(Long stationId);
}
