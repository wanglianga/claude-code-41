package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "equipment_types")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    /** 押金（元） */
    @Column(nullable = false)
    private BigDecimal deposit;

    /** 更换周期（月） */
    @Column(nullable = false)
    private Integer replacementCycleMonths;

    /** 可选尺码，逗号分隔 */
    private String sizes;

    private String description;

    public EquipmentType(String code, String name, BigDecimal deposit, Integer replacementCycleMonths, String sizes, String description) {
        this.code = code;
        this.name = name;
        this.deposit = deposit;
        this.replacementCycleMonths = replacementCycleMonths;
        this.sizes = sizes;
        this.description = description;
    }
}
