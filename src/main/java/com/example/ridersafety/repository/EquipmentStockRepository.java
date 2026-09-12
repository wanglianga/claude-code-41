package com.example.ridersafety.repository;

import com.example.ridersafety.model.EquipmentStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentStockRepository extends JpaRepository<EquipmentStock, Long> {
    List<EquipmentStock> findByStation_Id(Long stationId);

    Optional<EquipmentStock> findByStation_IdAndEquipmentType_IdAndSize(Long stationId, Long equipmentTypeId, String size);
}
