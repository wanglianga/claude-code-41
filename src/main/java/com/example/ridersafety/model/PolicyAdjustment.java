package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 站点采购 / 培训 / 审核规则调整 */
@Entity
@Table(name = "policy_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class PolicyAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 1500)
    private String content;

    private String createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    public PolicyAdjustment(Station station, PolicyType type, String title, String content, String createdBy) {
        this.station = station;
        this.type = type;
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
    }
}
