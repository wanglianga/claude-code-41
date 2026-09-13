package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 雨季计划补货行：按尺码计算需求与补货量 */
@Entity
@Table(name = "rain_plan_restocks")
@Getter
@Setter
@NoArgsConstructor
public class RainPlanRestock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private RainPlan plan;

    @Column(nullable = false)
    private String size;

    /** 该尺码骑手总数 */
    private Integer riderCount;

    /** 其中无有效雨衣需更换人数 */
    private Integer needReplace;

    /** 近 90 天可追溯损耗：已批准雨衣更换申请数 + 雨衣损坏事故数 */
    private Integer lossBase;

    /** 损耗来源明细（可追溯，如 "更换#2、事故#5"） */
    @Column(length = 500)
    private String lossSources;

    /** 未来 30 天预期损耗 = lossBase × (30/90) × 天气强度系数 */
    private Double expectedLoss;

    /** 雨季排班缓冲 = 骑手数 × 每周雨天班次 × 天气强度系数 × 0.2 */
    private Double shiftBuffer;

    /** 需求合计 = 待更换 + ceil(预期损耗 + 排班缓冲) */
    private Integer demand;

    /** 生成时库存快照 */
    private Integer stockBefore;

    /** 补货量 = max(0, 需求 - 库存) */
    private Integer restockQty;

    /** 确认后是否已写入库存 */
    private Boolean applied = false;
}
