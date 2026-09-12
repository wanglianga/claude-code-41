package com.example.ridersafety.repository;

import com.example.ridersafety.model.AccidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccidentReportRepository extends JpaRepository<AccidentReport, Long> {
    List<AccidentReport> findByRider_IdOrderByOccurredAtDesc(Long riderId);

    List<AccidentReport> findByRider_Station_IdOrderByOccurredAtDesc(Long stationId);
}
