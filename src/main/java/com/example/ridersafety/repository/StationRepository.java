package com.example.ridersafety.repository;

import com.example.ridersafety.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {
}
