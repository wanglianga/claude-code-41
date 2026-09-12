package com.example.ridersafety.repository;

import com.example.ridersafety.model.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {
    List<TrainingRecord> findByRider_IdOrderByCompletedAtDesc(Long riderId);

    List<TrainingRecord> findByRider_Station_IdOrderByCompletedAtDesc(Long stationId);
}
