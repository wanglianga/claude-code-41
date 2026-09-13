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

    /** 近 90 天该尺码雨衣发放次数（历史损耗） */
    private Integer historyLoss;

    /** 雨季排班轮换缓冲（骑手数 × 20% 向上取整） */
    private Integer shiftBuffer;

    /** 需求合计 = 待更换 + 排班缓冲 + 历史损耗补充 */
    private Integer demand;

    /** 生成时库存快照 */
    private Integer stockBefore;

    /** 补货量 = max(0, 需求 - 库存) */
    private Integer restockQty;

    /** 确认后是否已写入库存 */
    private Boolean applied = false;
}
