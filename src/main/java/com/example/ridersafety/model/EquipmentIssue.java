package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 装备领用记录 */
@Entity
@Table(name = "equipment_issues")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_type_id", nullable = false)
    private EquipmentType equipmentType;

    @Column(nullable = false)
    private String size;

    /** 发放时间 */
    @Column(nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    /** 实收押金 */
    @Column(nullable = false)
    private BigDecimal depositPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.IN_USE;

    /** 预计应更换日期（按更换周期计算） */
    private LocalDate expectedReplaceAt;

    private LocalDateTime returnedAt;

    @Column(length = 500)
    private String notes;
}
