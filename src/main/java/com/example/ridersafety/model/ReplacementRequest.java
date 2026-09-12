package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 装备更换申请 */
@Entity
@Table(name = "replacement_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReplacementRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private EquipmentIssue issue;

    @Column(length = 1000)
    private String reason;

    /** 磨损照片（URL，逗号分隔，历史遗留字段） */
    @Column(length = 1000)
    private String wearPhotos;

    /** 磨损照片附件 ID（逗号分隔，对应 attachments 表） */
    @Column(length = 500)
    private String photoIds;

    /** 申请时天气 */
    @Enumerated(EnumType.STRING)
    private Weather weather;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReplacementStatus status = ReplacementStatus.PENDING;

    /** 系统自动评估结论（依据：磨损、天气、历史领用、库存） */
    @Column(length = 1500)
    private String evaluation;

    /** 系统建议处理方式 */
    @Enumerated(EnumType.STRING)
    private ReplacementStatus suggestedDecision;

    /** 押金扣减金额 */
    private BigDecimal depositDeducted = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    private LocalDateTime processedAt;

    @Column(length = 500)
    private String processNote;

    private LocalDateTime createdAt = LocalDateTime.now();
}
