package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 站长事故核查结论 */
@Entity
@Table(name = "accident_reviews")
@Getter
@Setter
@NoArgsConstructor
public class AccidentReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accident_id", nullable = false, unique = true)
    private AccidentReport accident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    /** 是否在配送中 */
    private Boolean wasDelivering;

    /** 是否佩戴装备 */
    private Boolean wearingEquipment;

    /** 是否违规骑行 */
    private Boolean hasViolation;

    @Column(length = 500)
    private String violationDesc;

    /** 是否需要保险材料 */
    private Boolean insuranceNeeded;

    /** 保险材料是否齐全 */
    private Boolean materialsComplete;

    @Column(length = 1000)
    private String reviewNotes;

    /** APPROVED / REJECTED */
    @Column(nullable = false)
    private String result;

    private LocalDateTime reviewedAt = LocalDateTime.now();
}
