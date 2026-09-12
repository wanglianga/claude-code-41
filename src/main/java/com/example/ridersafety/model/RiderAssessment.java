package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 骑手考核记录（基准分 100，加减分） */
@Entity
@Table(name = "rider_assessments")
@Getter
@Setter
@NoArgsConstructor
public class RiderAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accident_id")
    private AccidentReport accident;

    /** 正为加分，负为扣分 */
    @Column(nullable = false)
    private Integer pointsChange;

    @Column(length = 500)
    private String reason;

    private LocalDateTime createdAt = LocalDateTime.now();

    public RiderAssessment(User rider, AccidentReport accident, Integer pointsChange, String reason) {
        this.rider = rider;
        this.accident = accident;
        this.pointsChange = pointsChange;
        this.reason = reason;
    }
}
