package com.example.ridersafety.repository;

import com.example.ridersafety.model.EquipmentReissue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentReissueRepository extends JpaRepository<EquipmentReissue, Long> {
    List<EquipmentReissue> findByAccident_Id(Long accidentId);

    List<EquipmentReissue> findByRider_Station_IdOrderByCreatedAtDesc(Long stationId);
}
