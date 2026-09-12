package com.example.ridersafety.repository;

import com.example.ridersafety.model.ReplacementRequest;
import com.example.ridersafety.model.ReplacementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplacementRequestRepository extends JpaRepository<ReplacementRequest, Long> {
    List<ReplacementRequest> findByRider_IdOrderByCreatedAtDesc(Long riderId);

    List<ReplacementRequest> findByRider_Station_IdOrderByCreatedAtDesc(Long stationId);

    List<ReplacementRequest> findByStatus(ReplacementStatus status);
}
