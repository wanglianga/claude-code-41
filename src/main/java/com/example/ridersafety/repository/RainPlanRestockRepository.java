package com.example.ridersafety.repository;

import com.example.ridersafety.model.RainPlanRestock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RainPlanRestockRepository extends JpaRepository<RainPlanRestock, Long> {
    List<RainPlanRestock> findByPlan_Id(Long planId);
}
