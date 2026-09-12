package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 骑手培训记录 */
@Entity
@Table(name = "training_records")
@Getter
@Setter
@NoArgsConstructor
public class TrainingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingCategory category;

    private LocalDateTime completedAt = LocalDateTime.now();

    private Integer score;

    public TrainingRecord(User rider, String title, TrainingCategory category, LocalDateTime completedAt, Integer score) {
        this.rider = rider;
        this.title = title;
        this.category = category;
        this.completedAt = completedAt;
        this.score = score;
    }
}
