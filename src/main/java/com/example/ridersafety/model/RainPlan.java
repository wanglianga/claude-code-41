package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 雨季雨衣集中更换/补货计划 */
@Entity
@Table(name = "rain_plans")
@Getter
@Setter
@NoArgsConstructor
public class RainPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false)
    private String title;

    /** 雨季标识，如 2026-雨季 */
    private String season;

    /** 天气预报：未来 30 天预计降雨天数 */
    private Integer rainyDays;

    /** 天气强度系数 = rainyDays / 15（基准雨季），参与需求计算 */
    private Double weatherFactor;

    /** 雨季排班：每骑手每周雨天班次（参与排班缓冲计算） */
    private Integer shiftsPerWeek;

    @Column(length = 500)
    private String weatherForecast;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RainPlanStatus status = RainPlanStatus.DRAFT;

    /** 站点骑手总数（生成时快照） */
    private Integer totalRiders;

    /** 需更换/新领雨衣的骑手数 */
    private Integer needReplace;

    private String createdBy;
    private String confirmedBy;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime confirmedAt;
}
