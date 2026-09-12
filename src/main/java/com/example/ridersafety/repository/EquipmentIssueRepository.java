package com.example.ridersafety.repository;

import com.example.ridersafety.model.EquipmentIssue;
import com.example.ridersafety.model.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentIssueRepository extends JpaRepository<EquipmentIssue, Long> {
    List<EquipmentIssue> findByRider_IdOrderByIssuedAtDesc(Long riderId);

    List<EquipmentIssue> findByStation_IdOrderByIssuedAtDesc(Long stationId);

    List<EquipmentIssue> findByRider_IdAndStatus(Long riderId, IssueStatus status);

    List<EquipmentIssue> findByStatus(IssueStatus status);
}
