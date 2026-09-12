package com.example.ridersafety.repository;

import com.example.ridersafety.model.InsuranceClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {
    List<InsuranceClaim> findByAccident_Id(Long accidentId);

    List<InsuranceClaim> findByAccident_Rider_Station_IdOrderByCreatedAtDesc(Long stationId);
}
