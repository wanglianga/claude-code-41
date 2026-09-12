package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 安全事故申报 */
@Entity
@Table(name = "accident_reports")
@Getter
@Setter
@NoArgsConstructor
public class AccidentReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccidentType type;

    /** 事故时间 */
    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private String location;

    /** 所属线路/区域 */
    private String routeArea;

    /** 关联订单号 */
    private String orderNo;

    /** 事故时装备状态描述 */
    @Column(length = 1000)
    private String equipmentStatusDesc;

    /** 损坏的装备类型（用于补发） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damaged_equipment_type_id")
    private EquipmentType damagedEquipmentType;

    /** 伤情描述 */
    @Column(length = 1000)
    private String injuryDesc;

    /** 交警记录编号 */
    private String policeRecordNo;

    /** 现场照片（URL，逗号分隔，历史遗留字段） */
    @Column(length = 1000)
    private String photoUrls;

    /** 现场照片附件 ID（逗号分隔，对应 attachments 表） */
    @Column(length = 500)
    private String photoIds;

    @Enumerated(EnumType.STRING)
    private Weather weather;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccidentStatus status = AccidentStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
}
