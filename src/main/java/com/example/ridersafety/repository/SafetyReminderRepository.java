package com.example.ridersafety.repository;

import com.example.ridersafety.model.SafetyReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SafetyReminderRepository extends JpaRepository<SafetyReminder, Long> {
    List<SafetyReminder> findByRider_IdOrderByCreatedAtDesc(Long riderId);

    long countByRider_IdAndReadFlagFalse(Long riderId);
}
