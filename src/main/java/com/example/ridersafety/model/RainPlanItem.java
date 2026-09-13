package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 雨季计划明细：每个骑手的雨衣更换/领取行 */
@Entity
@Table(name = "rain_plan_items")
@Getter
@Setter
@NoArgsConstructor
public class RainPlanItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private RainPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    /** 计划尺码（取自骑手最近雨衣领用尺码，无记录默认 L） */
    @Column(nullable = false)
    private String size;

    /** 生成计划时是否已持有有效雨衣 */
    private Boolean hadValidRaincoat;

    /** 被替换的旧雨衣领用 ID */
    private Long oldIssueId;

    /** 领取后生成的新领用 ID */
    private Long newIssueId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RainPlanItemStatus status = RainPlanItemStatus.PENDING;

    private LocalDateTime remindedAt;
    private LocalDateTime issuedAt;
}
