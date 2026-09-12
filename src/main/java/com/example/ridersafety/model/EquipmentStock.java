package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment_stock",
        uniqueConstraints = @UniqueConstraint(columnNames = {"station_id", "equipment_type_id", "size"}))
@Getter
@Setter
@NoArgsConstructor
public class EquipmentStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_type_id", nullable = false)
    private EquipmentType equipmentType;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private Integer quantity = 0;

    public EquipmentStock(Station station, EquipmentType equipmentType, String size, Integer quantity) {
        this.station = station;
        this.equipmentType = equipmentType;
        this.size = size;
        this.quantity = quantity;
    }
}
