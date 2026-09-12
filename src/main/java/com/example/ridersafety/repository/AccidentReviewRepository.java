package com.example.ridersafety.repository;

import com.example.ridersafety.model.AccidentReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccidentReviewRepository extends JpaRepository<AccidentReview, Long> {
    Optional<AccidentReview> findByAccident_Id(Long accidentId);
}
